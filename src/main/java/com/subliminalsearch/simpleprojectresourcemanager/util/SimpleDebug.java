package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;
import java.time.LocalDate;

public class SimpleDebug {
    
    public static void main(String[] args) {
        System.out.println("=== Simple Debug ===\n");
        
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            Statement stmt = conn.createStatement();
            
            // Check resources
            System.out.println("Resources in database:");
            ResultSet rs = stmt.executeQuery("SELECT id, name FROM resources ORDER BY id");
            while (rs.next()) {
                System.out.println("  Resource ID: " + rs.getLong("id") + ", Name: " + rs.getString("name"));
            }
            
            // Check assignments for January 2025
            System.out.println("\nAssignments for January 2025:");
            rs = stmt.executeQuery(
                "SELECT a.*, r.name as resource_name, p.project_id as project_name " +
                "FROM assignments a " +
                "JOIN resources r ON a.resource_id = r.id " +
                "JOIN projects p ON a.project_id = p.id " +
                "WHERE date(a.start_date) <= date('2025-01-31') " +
                "AND date(a.end_date) >= date('2025-01-01')"
            );
            
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println("  Assignment: Project=" + rs.getString("project_name") + 
                    ", Resource=" + rs.getString("resource_name") +
                    ", ResourceID=" + rs.getLong("resource_id") +
                    ", Dates=" + rs.getString("start_date").substring(0, 10) + 
                    " to " + rs.getString("end_date").substring(0, 10));
            }
            System.out.println("Total assignments found: " + count);
            
            // Check what the app's query would return
            System.out.println("\nUsing app's date range query pattern:");
            rs = stmt.executeQuery(
                "SELECT * FROM assignments " +
                "WHERE date(start_date) <= date('2025-01-31') AND date(end_date) >= date('2025-01-01') " +
                "ORDER BY start_date ASC"
            );
            
            count = 0;
            while (rs.next()) {
                count++;
                System.out.println("  Found assignment ID: " + rs.getLong("id") + 
                    ", Resource ID: " + rs.getLong("resource_id"));
            }
            System.out.println("Total with app query: " + count);
            
            System.out.println("\n=== Debug Complete ===");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}