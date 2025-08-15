package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;

public class DiagnoseProjectIssue {
    public static void main(String[] args) {
        DatabaseConfig dbConfig = new DatabaseConfig();
        DataSource dataSource = dbConfig.getDataSource();
        
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("=== PROJECT MANAGER DIAGNOSIS ===\n");
            
            // 1. Check all project managers
            System.out.println("Project Managers in Database:");
            System.out.println("------------------------------");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM project_managers ORDER BY id")) {
                while (rs.next()) {
                    System.out.printf("ID: %d, Name: %s, Email: %s%n", 
                        rs.getLong("id"), rs.getString("name"), rs.getString("email"));
                }
            }
            
            // 2. Check for Paula Poodle specifically
            System.out.println("\nPaula Poodle Check:");
            System.out.println("-------------------");
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM project_managers WHERE name LIKE '%Paula%'")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    long paulaId = rs.getLong("id");
                    System.out.printf("Found Paula Poodle - ID: %d, Name: %s%n", 
                        paulaId, rs.getString("name"));
                    
                    // Check projects assigned to Paula
                    System.out.println("\nProjects assigned to Paula (ID " + paulaId + "):");
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT project_id, description, project_manager_id, start_date, end_date " +
                            "FROM projects WHERE project_manager_id = ?")) {
                        ps.setLong(1, paulaId);
                        ResultSet projRs = ps.executeQuery();
                        while (projRs.next()) {
                            System.out.printf("  - %s: %s (PM ID: %d, Dates: %s to %s)%n",
                                projRs.getString("project_id"),
                                projRs.getString("description"),
                                projRs.getLong("project_manager_id"),
                                projRs.getString("start_date"),
                                projRs.getString("end_date"));
                        }
                    }
                } else {
                    System.out.println("Paula Poodle NOT found!");
                }
            }
            
            // 3. Check for Project 091
            System.out.println("\nProject 091 Check:");
            System.out.println("------------------");
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM projects WHERE project_id = '091' OR project_id = 'P-091'")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.printf("Found Project 091:%n");
                    System.out.printf("  ID: %d%n", rs.getLong("id"));
                    System.out.printf("  Project ID: %s%n", rs.getString("project_id"));
                    System.out.printf("  Description: %s%n", rs.getString("description"));
                    System.out.printf("  PM ID: %s%n", rs.getObject("project_manager_id"));
                    System.out.printf("  Start: %s, End: %s%n", 
                        rs.getString("start_date"), rs.getString("end_date"));
                    System.out.printf("  Status: %s%n", rs.getString("status"));
                    
                    // Check if PM exists
                    Long pmId = rs.getObject("project_manager_id") != null ? 
                        rs.getLong("project_manager_id") : null;
                    if (pmId != null) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT name FROM project_managers WHERE id = ?")) {
                            ps.setLong(1, pmId);
                            ResultSet pmRs = ps.executeQuery();
                            if (pmRs.next()) {
                                System.out.printf("  PM Name: %s%n", pmRs.getString("name"));
                            } else {
                                System.out.printf("  WARNING: PM ID %d does not exist!%n", pmId);
                            }
                        }
                    }
                } else {
                    System.out.println("Project 091 NOT found!");
                }
            }
            
            // 4. Check for Project 093
            System.out.println("\nProject 093 Check:");
            System.out.println("------------------");
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM projects WHERE project_id = '093' OR project_id = 'P-093'")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.printf("Found Project 093:%n");
                    System.out.printf("  ID: %d%n", rs.getLong("id"));
                    System.out.printf("  Project ID: %s%n", rs.getString("project_id"));
                    System.out.printf("  Description: %s%n", rs.getString("description"));
                    System.out.printf("  PM ID: %s%n", rs.getObject("project_manager_id"));
                    System.out.printf("  Start: %s, End: %s%n", 
                        rs.getString("start_date"), rs.getString("end_date"));
                }
            }
            
            // 5. Check for orphaned projects (PM ID doesn't exist)
            System.out.println("\nOrphaned Projects (invalid PM ID):");
            System.out.println("-----------------------------------");
            String orphanQuery = 
                "SELECT p.project_id, p.description, p.project_manager_id " +
                "FROM projects p " +
                "WHERE p.project_manager_id IS NOT NULL " +
                "AND p.project_manager_id NOT IN (SELECT id FROM project_managers)";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(orphanQuery)) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("  - %s: %s (Invalid PM ID: %d)%n",
                        rs.getString("project_id"),
                        rs.getString("description"),
                        rs.getLong("project_manager_id"));
                }
                if (!found) {
                    System.out.println("  None found - all PM IDs are valid");
                }
            }
            
            // 6. Check August 2025 projects
            System.out.println("\nProjects in August 2025:");
            System.out.println("------------------------");
            String augustQuery = 
                "SELECT project_id, description, project_manager_id, start_date, end_date " +
                "FROM projects " +
                "WHERE (start_date <= '2025-08-31' AND end_date >= '2025-08-01') " +
                "OR (start_date >= '2025-08-01' AND start_date <= '2025-08-31') " +
                "ORDER BY project_id";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(augustQuery)) {
                while (rs.next()) {
                    System.out.printf("  %s: %s (PM ID: %s)%n",
                        rs.getString("project_id"),
                        rs.getString("description"),
                        rs.getObject("project_manager_id"));
                }
            }
            
            System.out.println("\n=== DIAGNOSIS COMPLETE ===");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}