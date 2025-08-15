package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import javax.sql.DataSource;
import java.sql.*;

public class FixAssignmentDateFormat {
    public static void main(String[] args) {
        try {
            DatabaseConfig dbConfig = new DatabaseConfig();
            DataSource dataSource = dbConfig.getDataSource();
            
            try (Connection conn = dataSource.getConnection()) {
                // Fix assignment 217 with proper date format
                String updateSql = "UPDATE assignments SET " +
                    "start_date = '2025-08-08 00:00:00.000', " +
                    "end_date = '2025-08-13 00:00:00.000' " +
                    "WHERE id = 217";
                
                try (Statement stmt = conn.createStatement()) {
                    int rowsUpdated = stmt.executeUpdate(updateSql);
                    System.out.println("Fixed assignment 217 with proper date format: " + rowsUpdated + " row(s) updated");
                }
                
                // Verify the fix
                String verifySql = "SELECT id, project_id, resource_id, start_date, end_date FROM assignments WHERE id = 217";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(verifySql)) {
                    if (rs.next()) {
                        System.out.println("\nVerification:");
                        System.out.println("Assignment ID: " + rs.getLong("id"));
                        System.out.println("Project ID: " + rs.getLong("project_id"));
                        System.out.println("Resource ID: " + rs.getLong("resource_id"));
                        System.out.println("Start Date: " + rs.getString("start_date"));
                        System.out.println("End Date: " + rs.getString("end_date"));
                        System.out.println("\n✓ Assignment dates have been fixed with proper format!");
                        System.out.println("Navigate to August 2025 to see project CH-PBLD-2025-091.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}