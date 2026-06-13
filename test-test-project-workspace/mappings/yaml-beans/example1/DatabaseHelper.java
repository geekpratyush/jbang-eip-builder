package com.example;

public class DatabaseHelper {
    private String jdbcUrl;
    private boolean connected = true;

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void ping() {
        System.out.println("Pinging database at: " + jdbcUrl);
    }
}
