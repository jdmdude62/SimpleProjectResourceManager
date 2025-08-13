package com.subliminalsearch.simpleprojectresourcemanager.data;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Complete database reset - removes ALL projects, assignments, and tasks
 */
public class CompleteReset {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== COMPLETE DATABASE RESET ===");
            System.out.println("This will remove ALL project data from the database.");
            System.out.println();
            
            DatabaseConfig dbConfig = new DatabaseConfig();
            
            try (Connection conn = dbConfig.getDataSource().getConnection()) {
                conn.setAutoCommit(false);
                
                try (Statement stmt = conn.createStatement()) {
                    // Show current state
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
                    if (rs.next()) {
                        System.out.println("Projects before reset: " + rs.getInt(1));
                    }
                    
                    rs = stmt.executeQuery("SELECT id, project_id FROM projects LIMIT 10");
                    while (rs.next()) {
                        System.out.println("  - " + rs.getString("project_id"));
                    }
                    
                    // Delete EVERYTHING project-related
                    System.out.println("\nDeleting all project data...");
                    
                    int tasksDeleted = stmt.executeUpdate("DELETE FROM tasks");
                    System.out.println("  Deleted " + tasksDeleted + " tasks");
                    
                    int assignmentsDeleted = stmt.executeUpdate("DELETE FROM assignments");
                    System.out.println("  Deleted " + assignmentsDeleted + " assignments");
                    
                    int projectsDeleted = stmt.executeUpdate("DELETE FROM projects");
                    System.out.println("  Deleted " + projectsDeleted + " projects");
                    
                    // Reset auto-increment sequences
                    stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('projects', 'assignments', 'tasks')");
                    System.out.println("  Reset auto-increment counters");
                    
                    // Vacuum to clean up
                    stmt.executeUpdate("VACUUM");
                    
                    conn.commit();
                    
                    // Verify
                    rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count == 0) {
                            System.out.println("\n✓ SUCCESS: Database completely reset!");
                            System.out.println("  Projects: 0");
                            System.out.println("  Assignments: 0");
                            System.out.println("  Tasks: 0");
                        } else {
                            System.out.println("\n✗ ERROR: " + count + " projects still remain!");
                        }
                    }
                    
                    // Show preserved data
                    rs = stmt.executeQuery("SELECT COUNT(*) FROM resources");
                    if (rs.next()) {
                        System.out.println("\nPreserved:");
                        System.out.println("  Resources: " + rs.getInt(1));
                    }
                    
                    rs = stmt.executeQuery("SELECT COUNT(*) FROM project_managers");
                    if (rs.next()) {
                        System.out.println("  Project Managers: " + rs.getInt(1));
                    }
                    
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }
            
            System.out.println("\nDatabase reset complete. Restart the application to see empty state.");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("Reset failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}