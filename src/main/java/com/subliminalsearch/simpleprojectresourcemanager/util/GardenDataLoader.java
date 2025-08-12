package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class GardenDataLoader {
    
    public static void main(String[] args) {
        System.out.println("Loading Garden Project Data...");
        
        // Create data source
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            // Read SQL file
            InputStream is = GardenDataLoader.class.getResourceAsStream("/db/garden_data_generator.sql");
            String sql = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));
            
            // Remove comment lines and split by semicolon
            String cleanedSql = sql.replaceAll("(?m)^--.*$", "").trim();
            String[] statements = cleanedSql.split(";");
            Statement stmt = conn.createStatement();
            
            conn.setAutoCommit(false);
            
            int count = 0;
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        stmt.executeUpdate(trimmed);
                        count++;
                        if (count % 10 == 0) {
                            System.out.println("Executed " + count + " statements...");
                        }
                    } catch (Exception e) {
                        System.err.println("Error executing: " + trimmed.substring(0, Math.min(50, trimmed.length())) + "...");
                        System.err.println("Error: " + e.getMessage());
                    }
                }
            }
            
            conn.commit();
            System.out.println("Successfully loaded garden data! Total statements: " + count);
            
        } catch (Exception e) {
            System.err.println("Failed to load data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}