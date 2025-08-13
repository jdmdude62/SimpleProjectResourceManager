package com.subliminalsearch.simpleprojectresourcemanager.data;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Quick database checker utility
 */
public class DatabaseChecker {
    
    public static void main(String[] args) {
        try {
            DatabaseConfig dbConfig = new DatabaseConfig();
            
            try (Connection conn = dbConfig.getDataSource().getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Get project count and list
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
                if (rs.next()) {
                    System.out.println("Total Projects: " + rs.getInt(1));
                }
                
                System.out.println("\nProject List:");
                rs = stmt.executeQuery("SELECT id, project_id, description FROM projects ORDER BY id");
                while (rs.next()) {
                    System.out.println("  ID: " + rs.getInt("id") + 
                                     " | " + rs.getString("project_id") + 
                                     " | " + rs.getString("description"));
                }
                
                rs = stmt.executeQuery("SELECT COUNT(*) FROM assignments");
                if (rs.next()) {
                    System.out.println("\nTotal Assignments: " + rs.getInt(1));
                }
                
                rs = stmt.executeQuery("SELECT COUNT(*) FROM tasks");
                if (rs.next()) {
                    System.out.println("Total Tasks: " + rs.getInt(1));
                }
            }
            
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}