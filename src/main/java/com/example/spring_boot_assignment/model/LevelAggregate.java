package com.example.spring_boot_assignment.model;

import java.util.List;

public record LevelAggregate(
    int level,
    int nodeCount,
    List<String> nodeIds,
    int transactionCount,
    double creditTotal,
    double debitTotal,
    double totalAmount) {}
