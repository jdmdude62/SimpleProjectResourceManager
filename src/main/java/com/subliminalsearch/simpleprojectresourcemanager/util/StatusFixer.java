package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class StatusFixer {
    
    public static void main(String[] args) {
        System.out.println("=== Status Value Fixer ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            
            System.out.println("Fixing project status values...");
            
            // Change PLANNED to ACTIVE
            int updated = stmt.executeUpdate(
                "UPDATE projects SET status = 'ACTIVE' WHERE status = 'PLANNED'"
            );
            System.out.println("  ✓ Updated " + updated + " projects from PLANNED to ACTIVE");
            
            // Change any LOW/MEDIUM/HIGH in status to ACTIVE (those are priority values)
            updated = stmt.executeUpdate(
                "UPDATE projects SET status = 'ACTIVE' WHERE status IN ('LOW', 'MEDIUM', 'HIGH')"
            );
            if (updated > 0) {
                System.out.println("  ✓ Fixed " + updated + " projects with incorrect status values");
            }
            
            // Verify the fix
            System.out.println("\nVerifying status values...");
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT status FROM projects");
            System.out.println("Current status values in database:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString("status"));
            }
            
            // Show project distribution
            System.out.println("\nProject distribution:");
            rs = stmt.executeQuery("SELECT status, COUNT(*) as count FROM projects GROUP BY status");
            while (rs.next()) {
                System.out.println("  " + rs.getString("status") + ": " + rs.getInt("count") + " projects");
            }
            
            System.out.println("\n=== Status Values Fixed! ===");
            System.out.println("Please restart the application.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}