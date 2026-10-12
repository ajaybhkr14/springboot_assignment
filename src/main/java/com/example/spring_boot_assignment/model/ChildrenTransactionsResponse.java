package com.example.spring_boot_assignment.model;


import java.util.List;
import java.util.Map;

public record ChildrenTransactionsResponse(
    String id,
    String parentId,
    String name,
    String accountNumber,
    int level,
    Map<String, List<NodeTransaction>> childrenTransactions) {}
