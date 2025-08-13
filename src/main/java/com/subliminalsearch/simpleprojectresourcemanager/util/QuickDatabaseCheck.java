package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;
import java.time.LocalDate;

public class QuickDatabaseCheck {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        
        try {
            Class.forName("org.sqlite.JDBC");
            
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                System.out.println("=== Quick Database Check ===");
                
                // Check total projects with budget data
                String query1 = "SELECT COUNT(*) as total FROM projects WHERE budget_amount IS NOT NULL OR revenue_amount IS NOT NULL";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query1)) {
                    if (rs.next()) {
                        System.out.println("Projects with budget/revenue data: " + rs.getInt("total"));
                    }
                }
                
                // Show sample projects with their dates
                String query2 = """
                    SELECT project_id, start_date, end_date, budget_amount, revenue_amount
                    FROM projects
                    WHERE budget_amount IS NOT NULL OR revenue_amount IS NOT NULL
                    ORDER BY start_date DESC
                    LIMIT 10
                """;
                
                System.out.println("\nSample projects with budget data:");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query2)) {
                    while (rs.next()) {
                        System.out.printf("  %s: %s to %s - Budget: $%.0f, Revenue: $%.0f%n",
                            rs.getString("project_id"),
                            rs.getString("start_date"),
                            rs.getString("end_date"),
                            rs.getDouble("budget_amount"),
                            rs.getDouble("revenue_amount"));
                    }
                }
                
                // Check date filter
                LocalDate startDate = LocalDate.of(2024, 1, 1);
                LocalDate endDate = LocalDate.of(2025, 8, 12);
                
                String query3 = """
                    SELECT COUNT(*) as matching,
                           SUM(revenue_amount) as total_revenue,
                           SUM(budget_amount) as total_budget
                    FROM projects
                    WHERE (budget_amount IS NOT NULL OR revenue_amount IS NOT NULL)
                    AND date(end_date) >= date(?)
                    AND date(start_date) <= date(?)
                """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(query3)) {
                    pstmt.setString(1, startDate.toString());
                    pstmt.setString(2, endDate.toString());
                    
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("\n=== Date Range: " + startDate + " to " + endDate + " ===");
                            System.out.println("Matching projects: " + rs.getInt("matching"));
                            System.out.printf("Total Revenue: $%,.0f%n", rs.getDouble("total_revenue"));
                            System.out.printf("Total Budget: $%,.0f%n", rs.getDouble("total_budget"));
                        }
                    }
                }
                
                // Show projects that would match the filter
                String query4 = """
                    SELECT project_id, start_date, end_date, revenue_amount
                    FROM projects
                    WHERE (budget_amount IS NOT NULL OR revenue_amount IS NOT NULL)
                    AND date(end_date) >= date(?)
                    AND date(start_date) <= date(?)
                    LIMIT 5
                """;
                
                System.out.println("\nProjects matching date filter:");
                try (PreparedStatement pstmt = conn.prepareStatement(query4)) {
                    pstmt.setString(1, startDate.toString());
                    pstmt.setString(2, endDate.toString());
                    
                    try (ResultSet rs = pstmt.executeQuery()) {
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            System.out.printf("  %s: %s to %s - Revenue: $%.0f%n",
                                rs.getString("project_id"),
                                rs.getString("start_date"),
                                rs.getString("end_date"),
                                rs.getDouble("revenue_amount"));
                        }
                        if (count == 0) {
                            System.out.println("  No projects found matching the date filter!");
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}