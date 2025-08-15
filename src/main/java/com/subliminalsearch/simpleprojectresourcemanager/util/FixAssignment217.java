package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import javax.sql.DataSource;
import java.sql.*;

public class FixAssignment217 {
    public static void main(String[] args) {
        try {
            DatabaseConfig dbConfig = new DatabaseConfig();
            DataSource dataSource = dbConfig.getDataSource();
            
            try (Connection conn = dataSource.getConnection()) {
                // Fix the corrupted assignment dates
                String updateSql = "UPDATE assignments SET start_date = '2025-08-08', end_date = '2025-08-13' WHERE id = 217";
                
                try (Statement stmt = conn.createStatement()) {
                    int rowsUpdated = stmt.executeUpdate(updateSql);
                    System.out.println("Fixed assignment 217 dates: " + rowsUpdated + " row(s) updated");
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
                        System.out.println("\n✓ Assignment dates have been fixed!");
                        System.out.println("Project CH-PBLD-2025-091 should now appear on the timeline.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}