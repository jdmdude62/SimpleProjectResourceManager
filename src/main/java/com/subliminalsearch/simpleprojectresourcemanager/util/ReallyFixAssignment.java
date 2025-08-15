package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;

public class ReallyFixAssignment {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // Update with proper date format
            String updateSql = "UPDATE assignments SET " +
                "start_date = '2025-08-08 00:00:00.000', " +
                "end_date = '2025-08-13 00:00:00.000' " +
                "WHERE id = 217";
            
            try (Statement stmt = conn.createStatement()) {
                int rows = stmt.executeUpdate(updateSql);
                System.out.println("Updated " + rows + " row(s)");
            }
            
            // Verify
            String query = "SELECT id, project_id, resource_id, start_date, end_date FROM assignments WHERE id = 217";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    System.out.println("\nAfter update:");
                    System.out.println("Start Date: " + rs.getString("start_date"));
                    System.out.println("End Date: " + rs.getString("end_date"));
                }
            }
            
            System.out.println("\n✓ Assignment 217 has been fixed!");
            System.out.println("Restart the application to see CH-PBLD-2025-091 on the timeline.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}