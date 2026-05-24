package com.enterprise.services;

public class OrderService {
    private AuditService auditService;
    private String env = "PROD";

    public void processOrder(String orderId, double amount) {
        System.out.println("Processing order " + orderId);
        auditService.logEvent("ORDER_PROCESSED", orderId);
    }
}
