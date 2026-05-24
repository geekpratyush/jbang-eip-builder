package com.enterprise.services;

public class AuditService {
    public void logEvent(String type, String data) {
        System.out.println("AUDIT: " + type + " | " + data);
    }
}
