package com.example.spring_boot_assignment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record NodeResponse(
    String id,
    String parentId,
    String name,
    String accountNumber,
    int level,
    @JsonProperty("isRoot") boolean isRoot,
    @JsonProperty("isLeaf") boolean isLeaf,
    List<NodeSummary> parentChain,
    List<NodeSummary> children,
    List<NodeTransaction> transactions,
    Map<String, List<NodeTransaction>> nextLevelTransactions,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<TreeNode> childrenTree) {
}
