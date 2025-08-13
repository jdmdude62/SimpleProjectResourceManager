package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Utility to manage test data - clear old data and load garden demo data
 */
public class DataManager {
    
    public static void clearAndLoadGardenData() {
        System.out.println("=== Data Manager ===");
        System.out.println("Clearing old test data and loading garden demo data...\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            
            // Step 1: Clear ALL existing data
            System.out.println("Step 1: Clearing all existing data...");
            stmt.executeUpdate("DELETE FROM assignments");
            stmt.executeUpdate("DELETE FROM tasks");
            stmt.executeUpdate("DELETE FROM projects");
            stmt.executeUpdate("DELETE FROM resources");
            stmt.executeUpdate("DELETE FROM project_managers");
            System.out.println("  ✓ All old data cleared");
            
            // Step 2: Load garden data from SQL file
            System.out.println("\nStep 2: Loading garden demo data...");
            InputStream is = DataManager.class.getResourceAsStream("/db/garden_data_generator.sql");
            
            if (is == null) {
                System.err.println("  ✗ Could not find garden_data_generator.sql");
                return;
            }
            
            String sql = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));
            
            // Remove comments and split by semicolon
            String[] statements = sql.split(";");
            
            int count = 0;
            int errors = 0;
            for (String statement : statements) {
                String trimmed = statement.replaceAll("(?m)^--.*$", "").trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        // Skip DELETE statements since we already cleared
                        if (trimmed.toUpperCase().startsWith("DELETE")) {
                            continue;
                        }
                        
                        // Skip statements for tables that don't exist
                        if (trimmed.contains("resource_unavailability") || 
                            trimmed.contains("project_dependencies") ||
                            trimmed.contains("project_milestones") ||
                            trimmed.contains("budget_allocations")) {
                            continue;
                        }
                        
                        stmt.executeUpdate(trimmed);
                        count++;
                        
                        if (count % 20 == 0) {
                            System.out.println("  ... loaded " + count + " records");
                        }
                    } catch (Exception e) {
                        // Skip errors for missing tables
                        if (!e.getMessage().contains("no such table") && 
                            !e.getMessage().contains("no such column")) {
                            System.err.println("  Warning: " + e.getMessage());
                            errors++;
                        }
                    }
                }
            }
            
            conn.commit();
            System.out.println("  ✓ Successfully loaded " + count + " garden demo records");
            if (errors > 0) {
                System.out.println("  ⚠ " + errors + " statements skipped (missing tables/columns)");
            }
            
            // Step 3: Verify what was loaded
            System.out.println("\nStep 3: Verifying loaded data...");
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM project_managers");
            if (rs.next()) System.out.println("  Project Managers: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM resources");
            if (rs.next()) System.out.println("  Resources: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
            if (rs.next()) System.out.println("  Projects: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM tasks");
            if (rs.next()) System.out.println("  Tasks: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM assignments");
            if (rs.next()) System.out.println("  Assignments: " + rs.getInt(1));
            
            System.out.println("\n=== Garden Data Loaded Successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Failed to load data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
    
    public static void main(String[] args) {
        clearAndLoadGardenData();
    }
}