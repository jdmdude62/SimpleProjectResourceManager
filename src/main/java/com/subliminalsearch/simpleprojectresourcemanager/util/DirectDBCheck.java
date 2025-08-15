package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;

public class DirectDBCheck {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            String query = "SELECT id, project_id, resource_id, start_date, end_date FROM assignments WHERE id = 217";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    System.out.println("Assignment 217 raw data:");
                    System.out.println("ID: " + rs.getLong("id"));
                    System.out.println("Project ID: " + rs.getLong("project_id"));
                    System.out.println("Resource ID: " + rs.getLong("resource_id"));
                    System.out.println("Start Date (raw): " + rs.getString("start_date"));
                    System.out.println("End Date (raw): " + rs.getString("end_date"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}