package com.example.spring_boot_assignment.model;

import java.util.List;

public record GraphNode(
    String id, String parentId, String name,
    String accountNumber, List<NodeTransaction> transactions) {
}
