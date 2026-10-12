package com.example.spring_boot_assignment.exception;

public class NodeNotFoundException extends RuntimeException {
    public NodeNotFoundException(String id) {
        super("Graph node " + id + " does not exist");
    }
}
