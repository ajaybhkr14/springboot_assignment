package com.example.spring_boot_assignment.exception;

public class CycleDetectedException extends RuntimeException {
    public CycleDetectedException(String id) {
        super("Cycle detected in graph at node " + id);
    }
}
