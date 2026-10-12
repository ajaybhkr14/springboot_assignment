package com.example.spring_boot_assignment.service;

import com.example.spring_boot_assignment.exception.CycleDetectedException;
import com.example.spring_boot_assignment.exception.InvalidRequestException;
import com.example.spring_boot_assignment.exception.NodeNotFoundException;
import com.example.spring_boot_assignment.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class GraphService {


    private static final String DATA_FILE = "transactions-graph.json";
    private static final int MIN_DEPTH = 1;
    private static final int MAX_DEPTH = 5;
    private static final Set<String> TXN_TYPES = Set.of("POS", "ATM", "TRANSFER", "SALARY");

    private final Map<String, GraphNode> nodeById = new HashMap<>();
    private final Map<String, List<GraphNode>> nodesByParentId = new HashMap<>();

    // ===================== Loading =====================

    @PostConstruct
    public void loadData() {
        try (InputStream in = new ClassPathResource(DATA_FILE).getInputStream()) {
            GraphData data = new ObjectMapper().readValue(in, GraphData.class);
            index(data.nodes());
        } catch (IOException e) {
            throw new IllegalStateException("Could not load " + DATA_FILE, e);
        }
    }

    void index(List<GraphNode> nodes) {
        nodeById.clear();
        nodesByParentId.clear();
        for (GraphNode node : nodes) {
            nodeById.put(node.id(), node);
            if (node.parentId() != null) {
                nodesByParentId
                    .computeIfAbsent(node.parentId(), k -> new ArrayList<>())
                    .add(node);
            }
        }
    }

    // ===================== GET /api/graph/nodes/{id} =====================

    public NodeResponse getNode(String id) {
        return getNode(id, MIN_DEPTH);
    }

    public NodeResponse getNode(String id, int maxDepth) {
        if (maxDepth < MIN_DEPTH || maxDepth > MAX_DEPTH) {
            throw new InvalidRequestException(
                "maxDepth must be between " + MIN_DEPTH + " and " + MAX_DEPTH);
        }

        GraphNode node = findNode(id);
        List<NodeSummary> parentChain = buildParentChain(node);
        int level = parentChain.size();
        List<GraphNode> children = nodesByParentId.getOrDefault(id, List.of());

        Map<String, List<NodeTransaction>> nextLevel = new LinkedHashMap<>();
        for (GraphNode child : children) {
            nextLevel.put(child.id(), child.transactions());
        }

        List<TreeNode> childrenTree = null;
        if (maxDepth > 1) {
            Set<String> visited = new HashSet<>();
            visited.add(node.id());
            childrenTree = buildChildrenTree(node, level, maxDepth, visited);
        }

        return new NodeResponse(
            node.id(), node.parentId(), node.name(), node.accountNumber(),
            level,
            parentChain.isEmpty(),     // isRoot
            children.isEmpty(),        // isLeaf
            parentChain,
            children.stream().map(NodeSummary::from).toList(),
            node.transactions(),
            nextLevel,
            childrenTree);
    }

    // ===================== GET /api/graph/nodes/{id}/children-transactions =====================

    public ChildrenTransactionsResponse getChildrenTransactions(String id, Double minAmount,
                                                                Double maxAmount, String txnType) {
        validateFilters(minAmount, maxAmount, txnType);

        GraphNode node = findNode(id);
        int level = buildParentChain(node).size();
        List<GraphNode> children = nodesByParentId.getOrDefault(id, List.of());
        String type = (txnType == null) ? null : txnType.toUpperCase();

        Map<String, List<NodeTransaction>> filtered = new LinkedHashMap<>();
        for (GraphNode child : children) {
            List<NodeTransaction> matching = child.transactions().stream()
                .filter(t -> minAmount == null || t.amount() >= minAmount)
                .filter(t -> maxAmount == null || t.amount() <= maxAmount)
                .filter(t -> type == null || t.txnType().equals(type))
                .toList();
            filtered.put(child.id(), matching);
        }

        return new ChildrenTransactionsResponse(
            node.id(), node.parentId(), node.name(), node.accountNumber(), level, filtered);
    }

    private void validateFilters(Double minAmount, Double maxAmount, String txnType) {
        if (minAmount != null && minAmount < 0) {
            throw new InvalidRequestException("minAmount must not be negative");
        }
        if (maxAmount != null && maxAmount < 0) {
            throw new InvalidRequestException("maxAmount must not be negative");
        }
        if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
            throw new InvalidRequestException("minAmount must not be greater than maxAmount");
        }
        if (txnType != null && !TXN_TYPES.contains(txnType.toUpperCase())) {
            throw new InvalidRequestException("txnType must be one of POS, ATM, TRANSFER, SALARY");
        }
    }

    // ===================== GET /api/graph/nodes/{id}/level-aggregates =====================

    public LevelAggregatesResponse getLevelAggregates(String id) {
        GraphNode node = findNode(id);
        int level = buildParentChain(node).size();

        List<LevelAggregate> aggregates = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(node.id());

        List<GraphNode> currentRow = List.of(node);
        int rowLevel = level;

        while (!currentRow.isEmpty()) {
            aggregates.add(summarise(rowLevel, currentRow));

            List<GraphNode> nextRow = new ArrayList<>();
            for (GraphNode n : currentRow) {
                for (GraphNode child : nodesByParentId.getOrDefault(n.id(), List.of())) {
                    if (!visited.add(child.id())) {
                        throw new CycleDetectedException(child.id());
                    }
                    nextRow.add(child);
                }
            }
            currentRow = nextRow;
            rowLevel++;
        }

        return new LevelAggregatesResponse(
            node.id(), node.parentId(), node.name(), node.accountNumber(), level, aggregates);
    }

    private LevelAggregate summarise(int level, List<GraphNode> nodes) {
        int transactionCount = 0;
        double creditTotal = 0;
        double debitTotal = 0;
        double totalAmount = 0;

        for (GraphNode n : nodes) {
            for (NodeTransaction t : n.transactions()) {
                transactionCount++;
                totalAmount += t.amount();
                if ("CREDIT".equals(t.direction())) {
                    creditTotal += t.amount();
                } else if ("DEBIT".equals(t.direction())) {
                    debitTotal += t.amount();
                }
            }
        }

        List<String> nodeIds = nodes.stream().map(GraphNode::id).toList();
        return new LevelAggregate(level, nodes.size(), nodeIds, transactionCount,
            creditTotal, debitTotal, totalAmount);
    }

    // ===================== Shared helpers =====================

    private GraphNode findNode(String id) {
        if (id.isBlank()) {
            throw new InvalidRequestException("Node id must not be blank");
        }
        GraphNode node = nodeById.get(id);
        if (node == null) {
            throw new NodeNotFoundException(id);
        }
        return node;
    }

    private List<NodeSummary> buildParentChain(GraphNode node) {
        List<NodeSummary> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(node.id());

        GraphNode current = node;
        while (current.parentId() != null && nodeById.containsKey(current.parentId())) {
            GraphNode parent = nodeById.get(current.parentId());
            if (!visited.add(parent.id())) {
                throw new CycleDetectedException(parent.id());
            }
            chain.add(NodeSummary.from(parent));
            current = parent;
        }
        Collections.reverse(chain);
        return chain;
    }

    private List<TreeNode> buildChildrenTree(GraphNode node, int nodeLevel, int levelsLeft, Set<String> visited) {
        if (levelsLeft == 0) {
            return List.of();
        }

        List<TreeNode> result = new ArrayList<>();
        for (GraphNode child : nodesByParentId.getOrDefault(node.id(), List.of())) {
            if (!visited.add(child.id())) {
                throw new CycleDetectedException(child.id());
            }
            List<TreeNode> grandchildren = buildChildrenTree(child, nodeLevel + 1, levelsLeft - 1, visited);
            boolean childIsLeaf = nodesByParentId.getOrDefault(child.id(), List.of()).isEmpty();

            result.add(new TreeNode(child.id(), child.parentId(), child.name(), child.accountNumber(),
                nodeLevel + 1, childIsLeaf, grandchildren));
        }
        return result;
    }
}
