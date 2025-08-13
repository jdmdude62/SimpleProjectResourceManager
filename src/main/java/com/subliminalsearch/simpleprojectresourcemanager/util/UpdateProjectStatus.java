package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import java.sql.*;
import java.time.LocalDate;

public class UpdateProjectStatus {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try {
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite driver not found: " + e.getMessage());
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // First, let's see what we're updating
            String selectSql = """
                SELECT COUNT(*) as count, 
                       MIN(date(start_date)) as earliest,
                       MAX(date(start_date)) as latest
                FROM projects 
                WHERE date(start_date) >= '2025-10-01'
            """;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(selectSql);
            
            if (rs.next()) {
                int count = rs.getInt("count");
                String earliest = rs.getString("earliest");
                String latest = rs.getString("latest");
                
                System.out.println("Found " + count + " projects starting from October 2025 or later");
                if (count > 0) {
                    System.out.println("Date range: " + earliest + " to " + latest);
                    
                    // Update the projects
                    String updateSql = """
                        UPDATE projects 
                        SET status = 'PLANNED' 
                        WHERE date(start_date) >= '2025-10-01'
                    """;
                    
                    int updated = stmt.executeUpdate(updateSql);
                    System.out.println("Successfully updated " + updated + " projects to PLANNED status");
                    
                    // Show a sample of updated projects
                    String sampleSql = """
                        SELECT project_id, name, start_date, status
                        FROM projects
                        WHERE date(start_date) >= '2025-10-01'
                        ORDER BY start_date
                        LIMIT 10
                    """;
                    
                    rs = stmt.executeQuery(sampleSql);
                    System.out.println("\nSample of updated projects:");
                    System.out.println("----------------------------------------");
                    while (rs.next()) {
                        System.out.printf("%-15s %-30s %s [%s]%n",
                            rs.getString("project_id"),
                            rs.getString("name"),
                            rs.getString("start_date").substring(0, 10),
                            rs.getString("status"));
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}