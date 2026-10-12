package com.example.spring_boot_assignment.model;

public record NodeSummary(String id, String parentId, String name, String accountNumber) {
    public static NodeSummary from(GraphNode n) {
        return new NodeSummary(n.id(), n.parentId(), n.name(), n.accountNumber());
    }
}
