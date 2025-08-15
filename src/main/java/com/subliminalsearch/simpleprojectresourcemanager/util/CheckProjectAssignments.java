package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import javax.sql.DataSource;
import java.sql.*;

public class CheckProjectAssignments {
    public static void main(String[] args) {
        try {
            DatabaseConfig dbConfig = new DatabaseConfig();
            DataSource dataSource = dbConfig.getDataSource();
            
            try (Connection conn = dataSource.getConnection()) {
                System.out.println("Checking assignments for CH-PBLD-2025-091:");
                System.out.println("============================================\n");
                
                // First, get the project's internal ID
                String projectQuery = "SELECT id, project_id, description, start_date, end_date " +
                                     "FROM projects WHERE project_id = 'CH-PBLD-2025-091'";
                
                Long projectInternalId = null;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(projectQuery)) {
                    if (rs.next()) {
                        projectInternalId = rs.getLong("id");
                        System.out.println("Project found:");
                        System.out.printf("  Internal ID: %d\n", projectInternalId);
                        System.out.printf("  Project ID: %s\n", rs.getString("project_id"));
                        System.out.printf("  Description: %s\n", rs.getString("description"));
                        System.out.printf("  Dates: %s to %s\n\n", 
                            rs.getString("start_date"), rs.getString("end_date"));
                    } else {
                        System.out.println("Project CH-PBLD-2025-091 not found!");
                        return;
                    }
                }
                
                // Now check for assignments for this project
                String assignmentQuery = 
                    "SELECT a.*, r.name as resource_name " +
                    "FROM assignments a " +
                    "LEFT JOIN resources r ON a.resource_id = r.id " +
                    "WHERE a.project_id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(assignmentQuery)) {
                    stmt.setLong(1, projectInternalId);
                    ResultSet rs = stmt.executeQuery();
                    
                    int count = 0;
                    System.out.println("Assignments for this project:");
                    System.out.println("------------------------------");
                    while (rs.next()) {
                        count++;
                        System.out.printf("Assignment ID: %d\n", rs.getLong("id"));
                        System.out.printf("  Resource: %s (ID: %d)\n", 
                            rs.getString("resource_name"), rs.getLong("resource_id"));
                        System.out.printf("  Dates: %s to %s\n", 
                            rs.getString("start_date"), rs.getString("end_date"));
                        System.out.printf("  Travel: %d days out, %d days back\n",
                            rs.getInt("travel_out_days"), rs.getInt("travel_back_days"));
                        System.out.println();
                    }
                    
                    if (count == 0) {
                        System.out.println("NO ASSIGNMENTS FOUND for this project!");
                        System.out.println("\nThis explains why the project doesn't appear on the timeline.");
                        System.out.println("The timeline only shows projects that have assignments.");
                    } else {
                        System.out.printf("Total assignments: %d\n", count);
                    }
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}