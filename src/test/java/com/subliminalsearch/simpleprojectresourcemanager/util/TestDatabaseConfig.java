package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;

public class TestDatabaseConfig {
    
    private static final String DEFAULT_TEST_DB = "test_unavailability.db";
    private static final String DB_URL_PROPERTY = "test.db.url";
    
    public static HikariDataSource createTestDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Check if custom database URL is provided via system property
        String dbUrl = System.getProperty(DB_URL_PROPERTY);
        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            config.setJdbcUrl(dbUrl);
            System.out.println("Using custom test database: " + dbUrl);
        } else {
            config.setJdbcUrl("jdbc:sqlite:" + DEFAULT_TEST_DB);
            System.out.println("Using default test database: " + DEFAULT_TEST_DB);
        }
        
        config.setMaximumPoolSize(1);
        return new HikariDataSource(config);
    }
    
    public static void cleanupTestDatabase() {
        // Only cleanup the default test database, not custom ones
        String customDb = System.getProperty(DB_URL_PROPERTY);
        if (customDb == null || customDb.trim().isEmpty()) {
            new File(DEFAULT_TEST_DB).delete();
        }
    }
    
    public static boolean isUsingProductionCopy() {
        String dbUrl = System.getProperty(DB_URL_PROPERTY);
        return dbUrl != null && dbUrl.contains("test_production_copy.db");
    }
}