package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class DateFixer {
    
    public static void main(String[] args) {
        System.out.println("=== Date Format Fixer ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            
            System.out.println("Fixing date formats...");
            
            // Fix projects table dates
            int projectsFixed = stmt.executeUpdate(
                "UPDATE projects SET " +
                "start_date = start_date || ' 00:00:00.000', " +
                "end_date = end_date || ' 00:00:00.000' " + 
                "WHERE start_date NOT LIKE '% %'"
            );
            System.out.println("  ✓ Fixed " + projectsFixed + " project dates");
            
            // Fix assignments table dates
            int assignmentsFixed = stmt.executeUpdate(
                "UPDATE assignments SET " +
                "start_date = start_date || ' 00:00:00.000', " +
                "end_date = end_date || ' 00:00:00.000' " + 
                "WHERE start_date NOT LIKE '% %'"
            );
            System.out.println("  ✓ Fixed " + assignmentsFixed + " assignment dates");
            
            // Verify the fix
            System.out.println("\nVerifying date formats...");
            ResultSet rs = stmt.executeQuery("SELECT id, project_id, start_date, end_date FROM projects LIMIT 3");
            while (rs.next()) {
                System.out.println("  Project " + rs.getString("project_id") + 
                    ": " + rs.getString("start_date") + " to " + rs.getString("end_date"));
            }
            
            System.out.println("\n=== Date Formats Fixed! ===");
            System.out.println("Please restart the application.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}