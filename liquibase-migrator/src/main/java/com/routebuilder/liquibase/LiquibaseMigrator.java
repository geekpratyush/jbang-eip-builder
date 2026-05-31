package com.routebuilder.liquibase;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LiquibaseMigrator {
    public static void main(String[] args) {
        String dbUrl = getEnv("DB_URL", "jdbc:h2:mem:testdb");
        String username = getEnv("DB_USERNAME", null);
        String password = getEnv("DB_PASSWORD", null);
        String changelogFile = getEnv("CHANGELOG_FILE", "changelog-master.xml");
        String searchPath = getEnv("SEARCH_PATH", ".");

        if (dbUrl.startsWith("jdbc:h2:") && username == null) {
            username = "sa";
            password = "";
        }

        System.out.println("==================================================");
        System.out.println("--- Starting Standalone Liquibase Migrator ---");
        System.out.println("DB_URL: " + dbUrl);
        System.out.println("CHANGELOG_FILE: " + changelogFile);
        System.out.println("SEARCH_PATH: " + searchPath);
        System.out.println("==================================================");

        try {
            // 1. Configure resources lookup path
            List<ResourceAccessor> accessors = new ArrayList<>();
            accessors.add(new DirectoryResourceAccessor(new File(searchPath)));
            CompositeResourceAccessor resourceAccessor = new CompositeResourceAccessor(accessors);

            // 2. Open database connection using factory auto-detection
            Database database = DatabaseFactory.getInstance().openDatabase(dbUrl, username, password, null, resourceAccessor);

            // 3. Execute Migration
            try (Liquibase liquibase = new Liquibase(changelogFile, resourceAccessor, database)) {
                liquibase.update("");
                System.out.println("--- Migration Executed Successfully! ---");
            }
        } catch (Exception e) {
            System.err.println("Migration failed with error:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getEnv(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null ? val : defaultValue;
    }
}
