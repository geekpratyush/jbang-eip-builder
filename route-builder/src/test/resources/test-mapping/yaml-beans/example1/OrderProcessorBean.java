package com.example;

public class OrderProcessorBean {
    private DatabaseHelper databaseHelper;
    private String environmentName;

    public void setDatabaseHelper(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String processOrder(String orderPayload) {
        if (databaseHelper != null && databaseHelper.isConnected()) {
            return "Processed [" + environmentName + "] -> " + orderPayload;
        }
        return "Database unavailable!";
    }
}
