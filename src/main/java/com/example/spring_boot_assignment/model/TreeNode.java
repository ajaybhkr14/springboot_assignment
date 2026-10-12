package com.example.spring_boot_assignment.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TreeNode(
    String id,
    String parentId,
    String name,
    String accountNumber,
    int level,
    @JsonProperty("isLeaf") boolean isLeaf,
    List<TreeNode> children) {
}
