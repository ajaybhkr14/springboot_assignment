package com.example.spring_boot_assignment.model;

public record NodeTransaction(String txnId, String direction, String txnType,
                              double amount, String currency, String timestamp, String description) {

}
