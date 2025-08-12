package com.subliminalsearch.simpleprojectresourcemanager.data;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility to clear project data from the database while preserving base resources
 */
public class DatabaseCleaner {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleaner.class);
    
    public static void main(String[] args) {
        try {
            DatabaseCleaner cleaner = new DatabaseCleaner();
            cleaner.clearProjectData();
            System.out.println("Database cleared successfully!");
            System.exit(0);
        } catch (Exception e) {
            logger.error("Failed to clear database", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    public void clearProjectData() throws Exception {
        DatabaseConfig dbConfig = new DatabaseConfig();
        
        try (Connection conn = dbConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            
            try (Statement stmt = conn.createStatement()) {
                // Get counts before deletion
                var projectCount = getCount(stmt, "projects");
                var assignmentCount = getCount(stmt, "assignments");
                var taskCount = getCount(stmt, "tasks");
                
                System.out.println("Current data:");
                System.out.println("  Projects: " + projectCount);
                System.out.println("  Assignments: " + assignmentCount);
                System.out.println("  Tasks: " + taskCount);
                
                // Clear project-related data
                logger.info("Clearing tasks...");
                stmt.executeUpdate("DELETE FROM tasks");
                
                logger.info("Clearing assignments...");
                stmt.executeUpdate("DELETE FROM assignments");
                
                logger.info("Clearing projects...");
                stmt.executeUpdate("DELETE FROM projects");
                
                // Reset auto-increment counters
                stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('tasks', 'assignments', 'projects')");
                
                conn.commit();
                
                // Get resource and PM counts (preserved)
                var resourceCount = getCount(stmt, "resources");
                var pmCount = getCount(stmt, "project_managers");
                
                System.out.println("\nData after clearing:");
                System.out.println("  Projects: 0");
                System.out.println("  Assignments: 0");
                System.out.println("  Tasks: 0");
                System.out.println("  Resources (preserved): " + resourceCount);
                System.out.println("  Project Managers (preserved): " + pmCount);
                
                logger.info("Database cleared successfully");
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    private int getCount(Statement stmt, String tableName) throws Exception {
        var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }
}