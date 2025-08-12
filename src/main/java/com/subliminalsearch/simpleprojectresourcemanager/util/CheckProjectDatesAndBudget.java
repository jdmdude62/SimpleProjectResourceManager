package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;
import java.time.LocalDate;

public class CheckProjectDatesAndBudget {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        
        try {
            Class.forName("org.sqlite.JDBC");
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement()) {
                
                // Check date ranges and budget data
                String query = """
                    SELECT 
                        MIN(start_date) as earliest_start,
                        MAX(end_date) as latest_end,
                        COUNT(*) as total_projects,
                        COUNT(budget_amount) as projects_with_budget,
                        SUM(CASE WHEN start_date >= '2024-01-01' AND start_date <= '2024-12-31' THEN 1 ELSE 0 END) as projects_2024,
                        SUM(CASE WHEN start_date >= '2025-01-01' AND start_date <= '2025-12-31' THEN 1 ELSE 0 END) as projects_2025
                    FROM projects
                """;
                
                ResultSet rs = stmt.executeQuery(query);
                if (rs.next()) {
                    System.out.println("=== Project Date and Budget Analysis ===");
                    System.out.println("Earliest start date: " + rs.getString("earliest_start"));
                    System.out.println("Latest end date: " + rs.getString("latest_end"));
                    System.out.println("Total projects: " + rs.getInt("total_projects"));
                    System.out.println("Projects with budget data: " + rs.getInt("projects_with_budget"));
                    System.out.println("Projects in 2024: " + rs.getInt("projects_2024"));
                    System.out.println("Projects in 2025: " + rs.getInt("projects_2025"));
                }
                
                // Show some sample projects with their dates
                System.out.println("\n=== Sample Projects with Budget Data ===");
                String sampleQuery = """
                    SELECT project_id, start_date, end_date, budget_amount, revenue_amount
                    FROM projects
                    WHERE budget_amount IS NOT NULL
                    ORDER BY start_date
                    LIMIT 10
                """;
                
                rs = stmt.executeQuery(sampleQuery);
                while (rs.next()) {
                    System.out.printf("%s: %s to %s - Budget: $%.0f, Revenue: $%.0f%n",
                        rs.getString("project_id"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getDouble("budget_amount"),
                        rs.getDouble("revenue_amount"));
                }
                
                // Update some projects to 2024 dates for testing
                System.out.println("\n=== Updating Projects to 2024 for Testing ===");
                
                // Update 30 projects to have 2024 dates
                String updateDates = """
                    UPDATE projects 
                    SET start_date = date('2024-' || substr(start_date, 6)),
                        end_date = date('2024-' || substr(end_date, 6))
                    WHERE budget_amount IS NOT NULL
                    AND project_id IN (
                        SELECT project_id FROM projects 
                        WHERE budget_amount IS NOT NULL 
                        LIMIT 30
                    )
                """;
                
                int updated = stmt.executeUpdate(updateDates);
                System.out.println("Updated " + updated + " projects to 2024 dates");
                
                // Verify the update
                rs = stmt.executeQuery(query);
                if (rs.next()) {
                    System.out.println("\n=== After Update ===");
                    System.out.println("Projects in 2024: " + rs.getInt("projects_2024"));
                    System.out.println("Projects in 2025: " + rs.getInt("projects_2025"));
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}