package com.example.spring_boot_assignment.model;

public record ApiErrorResponse(String error, String message,
                               int status, String path, String timestamp) {

}
