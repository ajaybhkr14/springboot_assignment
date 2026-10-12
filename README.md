# Transaction Graph Node Explorer

A Spring Boot REST service for exploring a hierarchy of account nodes and their transactions. Given a node id, it
returns the node's level, ancestors, children and transactions, with optional child trees, filters and per-level totals.

**Stack:** Java 17, Spring Boot 3.5.16, Maven (wrapper included), JUnit 5. No database; data is held in memory.

## Run

```bash
./mvnw spring-boot:run       # macOS / Linux
.\mvnw.cmd spring-boot:run   # Windows

./mvnw test                  # run the tests
```

The app starts on http://localhost:8080 and logs `Loaded 20 nodes from transactions-graph.json`. The data file is
`src/main/resources/transactions-graph.json`; it keeps its provided name rather than the `transactions-graph-nodes.json`
suggested in the brief.

## Endpoints

| Endpoint                                          | Optional parameters                 | Returns                                                                                                                                                                                                           |
|---------------------------------------------------|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET /api/graph/nodes/{id}`                       | `maxDepth` (1–5, default 1)         | Node details, `level`, `isRoot`, `isLeaf`, `parentChain` (root first), direct `children`, own `transactions`, children's transactions as `nextLevelTransactions`, and a nested `childrenTree` when `maxDepth` > 1 |
| `GET /api/graph/nodes/{id}/children-transactions` | `minAmount`, `maxAmount`, `txnType` | Node details, `level`, and children's transactions that match all given filters, grouped by child id                                                                                                              |
| `GET /api/graph/nodes/{id}/level-aggregates`      | none                                | Node details, `level`, and for each level of its subtree: `nodeCount`, `nodeIds`, `transactionCount`, `creditTotal`, `debitTotal`, `totalAmount`                                                                  |

Examples:

```bash
curl "http://localhost:8080/api/graph/nodes/N2"
curl "http://localhost:8080/api/graph/nodes/N1?maxDepth=3"
curl "http://localhost:8080/api/graph/nodes/N2/children-transactions?minAmount=2000&maxAmount=2200&txnType=SALARY"
curl "http://localhost:8080/api/graph/nodes/N2/level-aggregates"
```

On Windows PowerShell, use `curl.exe`, and keep the quotes around URLs.

## Errors

Errors raised by the service use this format:

```json
{
    "error": "NODE_NOT_FOUND",
    "message": "Graph node N999 does not exist",
    "status": 404,
    "path": "/api/graph/nodes/N999",
    "timestamp": "2026-10-12T10:15:00.123Z"
}
```

| Code              | Status | When                                                                                               |
|-------------------|--------|----------------------------------------------------------------------------------------------------|
| `NODE_NOT_FOUND`  | 404    | The node id does not exist                                                                         |
| `INVALID_REQUEST` | 400    | Blank id, `maxDepth` outside 1–5, negative amount, `minAmount` > `maxAmount`, or unknown `txnType` |
| `CYCLE_DETECTED`  | 500    | The data contains a loop in parent links                                                           |

Framework errors (unknown URL, wrong HTTP method, non-numeric parameters such as `maxDepth=abc`) return the correct
status in Spring Boot's default error format.

## Assumptions and decisions

- A node whose parent does not exist (N19 → N99) is treated as a root: level 0, empty `parentChain`.
- `parentChain`, `children` and `childrenTree` contain node details only; transactions are shown once, under
  `transactions` or `nextLevelTransactions`.
- `maxDepth` accepts 1–5. The brief mentions both 5 and 10 as limits; 5 is its stated maximum, and 0 is rejected because
  the endpoint always returns direct children. `childrenTree` appears only when `maxDepth` > 1. Each tree node has
  `isLeaf`, so a node cut off by the depth limit (`children: []`, `isLeaf: false`) can be told apart from a real leaf.
- Filters: amount bounds are inclusive, `txnType` is case-insensitive, and children with no matching transactions are
  listed with an empty list.
- Level aggregates cover the whole subtree, including the node itself, using real graph levels (N2's subtree starts at
  level 1). `totalAmount` is the gross sum of all amounts regardless of direction; `creditTotal` and `debitTotal` are
  reported separately.
- A cycle returns 500, not 400, because it is caused by bad data rather than a bad request.
- Node ids are case-sensitive. Transaction types and directions are used as given.
- `amount` is a `double`, as in the brief; a production system would use `BigDecimal`.
