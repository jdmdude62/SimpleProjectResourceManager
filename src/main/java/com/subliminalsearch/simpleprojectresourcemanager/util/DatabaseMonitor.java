package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class DatabaseMonitor {
    
    public static void printDatabaseStats() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        HikariDataSource dataSource = dbConfig.getDataSource();
        
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("\n=== DATABASE STATISTICS ===");
            
            printCount(conn, "Projects", "SELECT COUNT(*) FROM projects");
            printCount(conn, "Resources", "SELECT COUNT(*) FROM resources");
            printCount(conn, "Assignments", "SELECT COUNT(*) FROM assignments");
            printCount(conn, "Tasks", "SELECT COUNT(*) FROM tasks");
            printCount(conn, "Task Dependencies", "SELECT COUNT(*) FROM task_dependencies");
            printCount(conn, "Resource Skills", "SELECT COUNT(*) FROM resource_skills");
            printCount(conn, "Resource Certifications", "SELECT COUNT(*) FROM resource_certifications");
            printCount(conn, "Purchase Orders", "SELECT COUNT(*) FROM purchase_orders");
            printCount(conn, "Actual Costs", "SELECT COUNT(*) FROM actual_costs");
            
            System.out.println("\n=== ORPHAN CHECK ===");
            
            // Check for orphaned assignments (no matching project)
            String orphanAssignments = "SELECT COUNT(*) FROM assignments a LEFT JOIN projects p ON a.project_id = p.id WHERE p.id IS NULL";
            printCount(conn, "Orphaned Assignments", orphanAssignments);
            
            // Check for orphaned tasks (no matching project)
            String orphanTasks = "SELECT COUNT(*) FROM tasks t LEFT JOIN projects p ON t.project_id = p.id WHERE p.id IS NULL";
            printCount(conn, "Orphaned Tasks", orphanTasks);
            
            // Check for orphaned resource skills (no matching resource)
            String orphanSkills = "SELECT COUNT(*) FROM resource_skills rs LEFT JOIN resources r ON rs.resource_id = r.id WHERE r.id IS NULL";
            printCount(conn, "Orphaned Resource Skills", orphanSkills);
            
            System.out.println("===========================\n");
            
        } catch (SQLException e) {
            System.err.println("Error monitoring database: " + e.getMessage());
        }
    }
    
    private static void printCount(Connection conn, String label, String query) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                System.out.printf("%-25s: %d%n", label, rs.getInt(1));
            }
        }
    }
    
    public static void main(String[] args) {
        printDatabaseStats();
    }
}