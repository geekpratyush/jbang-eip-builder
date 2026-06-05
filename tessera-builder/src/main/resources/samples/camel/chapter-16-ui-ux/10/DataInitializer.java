package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.BindToRegistry;

public class DataInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(DataInitializer.class);
    private static final String H2_URL = "jdbc:h2:mem:integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @BindToRegistry("dataInitializer")
    public void init() {
        LOG.info("Initializing Modular Integration Chassis Database...");
        try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS chassis");
                stmt.execute("DROP TABLE IF EXISTS chassis.transactions");
                stmt.execute("CREATE TABLE chassis.transactions ("
                        + "id VARCHAR(50) PRIMARY KEY, "
                        + "counterparty VARCHAR(100), "
                        + "transaction_type VARCHAR(50), "
                        + "amount DECIMAL(18,2), "
                        + "currency VARCHAR(10), "
                        + "status VARCHAR(50), "
                        + "created_at TIMESTAMP)");
            }

            conn.setAutoCommit(false);
            String sql = "INSERT INTO chassis.transactions (id, counterparty, transaction_type, amount, currency, status, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP - ? * INTERVAL '1' MINUTE)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                String[] counterparties = {"Global Industries LLC", "Stark Logistics", "Oceanic Trade Corp", "Acme Financials", "Wayne Enterprises", "LexCorp", "Cyberdyne Systems", "Umbrella Corp", "Massive Dynamic", "Tyrell Corp", "Weyland-Yutani", "Oscorp", "Initech", "Hooli Inc", "Virtucon", "Soylent Corp"};
                String[] types = {"PAYMENT", "EARMARK", "SWIFT_TRANSFER", "DIRECT_DEBIT", "CREDIT_ALLOCATION", "LIQUIDITY_SWAP"};
                String[] currencies = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD"};
                String[] statuses = {"PENDING_APPROVAL", "APPROVED", "REJECTED", "PROCESSING", "COMPLETED", "FAILED"};
                
                Random rand = new Random(42);
                int totalRecords = 50000;
                for (int i = 1; i <= totalRecords; i++) {
                    pstmt.setString(1, "TXN-" + (100000 + i));
                    pstmt.setString(2, counterparties[rand.nextInt(counterparties.length)]);
                    pstmt.setString(3, types[rand.nextInt(types.length)]);
                    pstmt.setDouble(4, 100.0 + rand.nextDouble() * 250000.0);
                    pstmt.setString(5, currencies[rand.nextInt(currencies.length)]);
                    pstmt.setString(6, statuses[rand.nextInt(statuses.length)]);
                    pstmt.setInt(7, i);
                    pstmt.addBatch();
                    
                    if (i % 10000 == 0) {
                        pstmt.executeBatch();
                    }
                }
                pstmt.executeBatch();
            }
            conn.commit();
            LOG.info("Successfully populated H2 database with 50,000 transaction records.");
        } catch (Exception e) {
            LOG.error("Failed to initialize H2 database", e);
        }
    }
}
