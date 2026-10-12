package com.example.spring_boot_assignment.model;

import java.util.List;

public record LevelAggregatesResponse(
    String id,
    String parentId,
    String name,
    String accountNumber,
    int level,
    List<LevelAggregate> levelAggregates) {}
