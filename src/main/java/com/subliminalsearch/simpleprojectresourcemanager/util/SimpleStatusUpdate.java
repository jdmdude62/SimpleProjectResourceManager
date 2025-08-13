package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;

public class SimpleStatusUpdate {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        Connection conn = null;
        Statement stmt = null;
        
        try {
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            
            // First, let's see what we're updating
            String selectSql = """
                SELECT COUNT(*) as count, 
                       MIN(date(start_date)) as earliest,
                       MAX(date(start_date)) as latest
                FROM projects 
                WHERE date(start_date) >= '2025-10-01'
            """;
            
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
                    
                    // Show a count of updated projects by month
                    String monthCountSql = """
                        SELECT strftime('%Y-%m', start_date) as month, COUNT(*) as count
                        FROM projects
                        WHERE date(start_date) >= '2025-10-01'
                        GROUP BY month
                        ORDER BY month
                        LIMIT 10
                    """;
                    
                    rs = stmt.executeQuery(monthCountSql);
                    System.out.println("\nProjects updated by month:");
                    System.out.println("----------------------------------------");
                    while (rs.next()) {
                        System.out.printf("%s: %d projects%n",
                            rs.getString("month"),
                            rs.getInt("count"));
                    }
                } else {
                    System.out.println("No projects found starting from October 2025 or later");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}