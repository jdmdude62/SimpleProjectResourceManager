package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class DataVerifier {
    
    public static void main(String[] args) {
        System.out.println("=== Database Data Verification ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            
            // Check project managers
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM project_managers");
            if (rs.next()) {
                System.out.println("Project Managers: " + rs.getInt(1));
            }
            
            // Check resources
            rs = stmt.executeQuery("SELECT COUNT(*) FROM resources");
            if (rs.next()) {
                System.out.println("Resources: " + rs.getInt(1));
            }
            
            // Check projects
            rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
            if (rs.next()) {
                System.out.println("Projects: " + rs.getInt(1));
            }
            
            // Show sample projects
            System.out.println("\nSample Projects:");
            rs = stmt.executeQuery("SELECT id, project_id, start_date, end_date, status FROM projects LIMIT 5");
            while (rs.next()) {
                System.out.println("  ID: " + rs.getInt("id") + 
                    ", Name: " + rs.getString("project_id") + 
                    ", Start: " + rs.getString("start_date").substring(0, 10) +
                    ", Status: " + rs.getString("status"));
            }
            
            // Check assignments
            rs = stmt.executeQuery("SELECT COUNT(*) FROM assignments");
            if (rs.next()) {
                System.out.println("\nAssignments: " + rs.getInt(1));
            }
            
            // Show sample assignments
            System.out.println("\nSample Assignments:");
            rs = stmt.executeQuery(
                "SELECT a.*, p.project_id, r.name as resource_name " +
                "FROM assignments a " +
                "JOIN projects p ON a.project_id = p.id " +
                "JOIN resources r ON a.resource_id = r.id " +
                "LIMIT 5"
            );
            while (rs.next()) {
                System.out.println("  Project: " + rs.getString("project_id") + 
                    ", Resource: " + rs.getString("resource_name") +
                    ", Dates: " + rs.getString("start_date").substring(0, 10) + 
                    " to " + rs.getString("end_date").substring(0, 10));
            }
            
            // Check for assignments in different months
            System.out.println("\nAssignments by Month:");
            rs = stmt.executeQuery(
                "SELECT substr(start_date, 1, 7) as month, COUNT(*) as count " +
                "FROM assignments " +
                "GROUP BY substr(start_date, 1, 7) " +
                "ORDER BY month"
            );
            while (rs.next()) {
                System.out.println("  " + rs.getString("month") + ": " + rs.getInt("count") + " assignments");
            }
            
            // Check tasks table
            rs = stmt.executeQuery("SELECT COUNT(*) FROM tasks");
            if (rs.next()) {
                System.out.println("\nTasks: " + rs.getInt(1));
            }
            
            System.out.println("\n=== Data Verification Complete ===");
            System.out.println("\nTo see the data in the app:");
            System.out.println("1. Navigate to January 2025 using the navigation buttons");
            System.out.println("2. The assignments should appear on the timeline");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}