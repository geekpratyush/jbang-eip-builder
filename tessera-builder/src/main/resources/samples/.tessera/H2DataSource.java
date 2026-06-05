package com.tessera.simulator;

import org.apache.camel.BindToRegistry;
import org.h2.jdbcx.JdbcDataSource;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DataSource {
    private static final Logger LOG = LoggerFactory.getLogger(H2DataSource.class);

    @BindToRegistry("dataSource")
    public DataSource dataSource() {
        LOG.info("Starting Native Embedded H2 SQL Database on jdbc:h2:mem:testdb...");
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS PUBLIC");
        ds.setUser("sa");
        ds.setPassword("");
        LOG.info("Embedded H2 SQL Database successfully started!");
        return ds;
    }
}
