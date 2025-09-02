package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;

public class VerifyTravelColumn {
    public static void main(String[] args) throws Exception {
        DatabaseConfig dbConfig = new DatabaseConfig();
        HikariDataSource dataSource = dbConfig.getDataSource();
        
        try (Connection conn = dataSource.getConnection()) {
            // Check if is_travel column exists
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "projects", "is_travel");
            
            if (rs.next()) {
                System.out.println("is_travel column EXISTS");
                System.out.println("  Type: " + rs.getString("TYPE_NAME"));
                System.out.println("  Size: " + rs.getInt("COLUMN_SIZE"));
                System.out.println("  Nullable: " + rs.getString("IS_NULLABLE"));
                System.out.println("  Default: " + rs.getString("COLUMN_DEF"));
            } else {
                System.out.println("ERROR: is_travel column DOES NOT EXIST!");
                System.out.println("Adding is_travel column...");
                
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("ALTER TABLE projects ADD COLUMN is_travel BOOLEAN DEFAULT 0");
                System.out.println("Added is_travel column successfully");
            }
            
            // Test updating a project's travel field
            System.out.println("\n=== Testing Direct SQL Update ===");
            
            // Get a project to test
            Statement stmt = conn.createStatement();
            ResultSet projectRs = stmt.executeQuery("SELECT id, project_id, description, is_travel FROM projects LIMIT 1");
            
            if (projectRs.next()) {
                long id = projectRs.getLong("id");
                String projectId = projectRs.getString("project_id");
                String description = projectRs.getString("description");
                int currentTravel = projectRs.getInt("is_travel");
                
                System.out.println("Found project: " + projectId + " (ID: " + id + ")");
                System.out.println("  Description: " + description);
                System.out.println("  Current is_travel: " + currentTravel);
                
                // Toggle the value
                int newTravel = (currentTravel == 0) ? 1 : 0;
                System.out.println("\nUpdating to is_travel=" + newTravel);
                
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE projects SET is_travel = ? WHERE id = ?"
                );
                updateStmt.setInt(1, newTravel);
                updateStmt.setLong(2, id);
                
                int rows = updateStmt.executeUpdate();
                System.out.println("Rows updated: " + rows);
                
                // Read it back
                PreparedStatement readStmt = conn.prepareStatement(
                    "SELECT is_travel FROM projects WHERE id = ?"
                );
                readStmt.setLong(1, id);
                ResultSet readRs = readStmt.executeQuery();
                
                if (readRs.next()) {
                    int readValue = readRs.getInt("is_travel");
                    System.out.println("Read back is_travel: " + readValue);
                    
                    if (readValue == newTravel) {
                        System.out.println("✓ Direct SQL update successful!");
                    } else {
                        System.out.println("✗ Direct SQL update FAILED - value didn't persist!");
                    }
                }
                
                // Toggle back
                updateStmt.setInt(1, currentTravel);
                updateStmt.setLong(2, id);
                updateStmt.executeUpdate();
                System.out.println("Restored original value: " + currentTravel);
            }
            
            // Check all projects
            System.out.println("\n=== All Projects Travel Status ===");
            ResultSet allRs = stmt.executeQuery(
                "SELECT project_id, is_travel FROM projects ORDER BY id DESC LIMIT 20"
            );
            
            while (allRs.next()) {
                System.out.printf("  %s: is_travel=%d%n", 
                    allRs.getString("project_id"), 
                    allRs.getInt("is_travel"));
            }
            
            // Show table schema
            System.out.println("\n=== Table Schema ===");
            ResultSet schemaRs = stmt.executeQuery("PRAGMA table_info(projects)");
            while (schemaRs.next()) {
                String colName = schemaRs.getString("name");
                if (colName.contains("travel") || colName.contains("send") || colName.contains("report")) {
                    System.out.printf("Column: %s, Type: %s, NotNull: %d, Default: %s%n",
                        colName,
                        schemaRs.getString("type"),
                        schemaRs.getInt("notnull"),
                        schemaRs.getString("dflt_value"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
        
        dataSource.close();
        System.out.println("\nDone.");
    }
}