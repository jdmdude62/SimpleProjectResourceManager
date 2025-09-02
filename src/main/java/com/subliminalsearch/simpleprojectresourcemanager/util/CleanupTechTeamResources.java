package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CleanupTechTeamResources {
    
    public static void main(String[] args) {
        // Force non-GUI mode
        System.setProperty("javafx.headless", "true");
        System.setProperty("java.awt.headless", "true");
        
        DatabaseConfig dbConfig = new DatabaseConfig();
        
        try {
            // First, list all Tech Team resources
            List<Long> techTeamIds = new ArrayList<>();
            String selectSql = "SELECT id, name, active FROM resources WHERE name LIKE 'Tech Team%' ORDER BY name";
            
            try (Connection conn = dbConfig.getDataSource().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(selectSql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                System.out.println("Found Tech Team resources:");
                System.out.println("ID\tActive\tName");
                System.out.println("--\t------\t----");
                
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String name = rs.getString("name");
                    boolean active = rs.getBoolean("active");
                    
                    techTeamIds.add(id);
                    System.out.printf("%d\t%s\t%s%n", id, active ? "Yes" : "No", name);
                }
            }
            
            if (techTeamIds.isEmpty()) {
                System.out.println("No Tech Team resources found.");
                return;
            }
            
            // Check for assignments
            System.out.println("\nChecking for assignments...");
            String assignmentSql = "SELECT COUNT(*) FROM assignments WHERE resource_id = ?";
            
            for (Long id : techTeamIds) {
                try (Connection conn = dbConfig.getDataSource().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(assignmentSql)) {
                    
                    pstmt.setLong(1, id);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            int count = rs.getInt(1);
                            if (count > 0) {
                                System.out.printf("Resource ID %d has %d assignment(s)%n", id, count);
                            }
                        }
                    }
                }
            }
            
            // Delete the Tech Team resources
            System.out.println("\nDeleting Tech Team resources...");
            String deleteSql = "DELETE FROM resources WHERE id = ?";
            int totalDeleted = 0;
            
            try (Connection conn = dbConfig.getDataSource().getConnection()) {
                conn.setAutoCommit(false);
                
                try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                    for (Long id : techTeamIds) {
                        pstmt.setLong(1, id);
                        int deleted = pstmt.executeUpdate();
                        if (deleted > 0) {
                            totalDeleted++;
                            System.out.printf("Deleted resource ID %d%n", id);
                        }
                    }
                }
                
                conn.commit();
                System.out.printf("\nSuccessfully deleted %d Tech Team resources.%n", totalDeleted);
                
            } catch (SQLException e) {
                System.err.println("Error deleting resources: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConfig.shutdown();
        }
    }
}