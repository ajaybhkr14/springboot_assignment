# Design

## Structure

Four packages, one job each: `controller` maps HTTP requests to the service and has no logic; `service` (`GraphService`)
loads the data and computes every answer; `model` holds immutable records for both the stored data and the responses;
`exception` holds the domain exceptions and `GlobalExceptionHandler`, which turns them into HTTP responses.

## Loading the data

At startup, `loadData()` (`@PostConstruct`) reads the JSON file with Jackson and passes the nodes to `index(...)`, which
builds two maps in one pass:

- `nodeById` finds any node in constant time and is used to walk **up**.
- `nodesByParentId` reverses the parent links, because the file only records each node's parent. It makes finding a
  node's children a constant-time lookup, and is used to walk **down**.

Nodes are filed under their parent's id whether or not the parent has been read yet, so the order of the file does not
matter. The maps are written once before any request and only read afterwards, so plain `HashMap`s are safe. A missing
or malformed file stops startup with a clear error. `index(...)` is separate from file reading so that tests can supply
their own small graphs.

## Level, parentChain and isRoot

One upward walk from the node, following `parentId` through `nodeById`, collects the ancestors. Reversed, they form
`parentChain` (root first); its length is the `level`; and an empty chain means `isRoot`. The walk stops when `parentId`
is null **or** the parent does not exist, which is how orphan nodes become roots at level 0.

## Walking down

- **Children and next-level transactions** come from a single lookup in `nodesByParentId`; `isLeaf` is true when there
  are none.
- **`childrenTree` (maxDepth)** is built with recursion (depth-first): each call adds a node's children and asks for
  each child's own tree with one level fewer, stopping at zero. Recursion suits a nested result, and depth is capped at
  5.
- **Level aggregates** use a breadth-first loop: start with the node as the current row, total it, build the next row
  from all the children, and repeat until a row is empty. Working row by row gives all nodes of a level together, which
  is exactly what per-level totals need.
- **Filters** take the children's transactions and keep those that pass every given filter, using streams. Input is
  validated first.

## Cycles

All three walks keep a `visited` set and throw `CycleDetectedException` (HTTP 500) if they reach a node twice, so no
walk can loop forever. Detection happens per request: the app still starts with bad data, and only requests that pass
through the loop fail. Validating the whole graph at startup would report problems sooner, but would take the whole
service down for one bad record.

## Errors

The service throws domain exceptions and the controller handles none itself. `GlobalExceptionHandler` maps them to 404,
400 and 500 with one consistent body. Framework errors are left to Spring Boot's defaults; extending
`ResponseEntityExceptionHandler` would bring them into the same format.
