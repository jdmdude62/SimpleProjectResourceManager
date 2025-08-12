package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;
import java.time.LocalDate;

public class DebugRevenueReport {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        
        try {
            Class.forName("org.sqlite.JDBC");
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement()) {
                
                LocalDate startDate = LocalDate.of(2024, 1, 1);
                LocalDate endDate = LocalDate.of(2025, 8, 12);
                
                System.out.println("=== Debugging Revenue Report Date Range ===");
                System.out.println("Looking for projects between: " + startDate + " and " + endDate);
                
                // Check all projects with budget data
                String query = """
                    SELECT project_id, start_date, end_date, budget_amount, revenue_amount, labor_cost, material_cost
                    FROM projects
                    WHERE budget_amount IS NOT NULL OR revenue_amount IS NOT NULL
                    ORDER BY start_date
                    LIMIT 20
                """;
                
                System.out.println("\n=== Projects with Budget Data (first 20) ===");
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    String startStr = rs.getString("start_date");
                    String endStr = rs.getString("end_date");
                    System.out.printf("%s: %s to %s - Budget: $%.0f, Revenue: $%.0f%n",
                        rs.getString("project_id"),
                        startStr,
                        endStr,
                        rs.getDouble("budget_amount"),
                        rs.getDouble("revenue_amount"));
                }
                
                // Count projects that would be included in the date range
                String countQuery = """
                    SELECT COUNT(*) as matching_projects,
                           SUM(revenue_amount) as total_revenue,
                           SUM(COALESCE(labor_cost, 0) + COALESCE(material_cost, 0) + COALESCE(travel_cost, 0) + COALESCE(other_cost, 0)) as total_cost
                    FROM projects
                    WHERE (budget_amount IS NOT NULL OR revenue_amount IS NOT NULL)
                    AND date(end_date) >= date(?)
                    AND date(start_date) <= date(?)
                """;
                
                PreparedStatement pstmt = conn.prepareStatement(countQuery);
                pstmt.setString(1, startDate.toString());
                pstmt.setString(2, endDate.toString());
                
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    System.out.println("\n=== Projects Matching Date Filter ===");
                    System.out.println("Projects in range: " + rs.getInt("matching_projects"));
                    System.out.println("Total Revenue: $" + String.format("%,.0f", rs.getDouble("total_revenue")));
                    System.out.println("Total Cost: $" + String.format("%,.0f", rs.getDouble("total_cost")));
                }
                
                // Test the exact filter logic used in the report
                String testQuery = """
                    SELECT project_id, start_date, end_date, revenue_amount
                    FROM projects
                    WHERE (budget_amount IS NOT NULL OR revenue_amount IS NOT NULL)
                    AND NOT date(end_date) < date(?)
                    AND NOT date(start_date) > date(?)
                    LIMIT 10
                """;
                
                pstmt = conn.prepareStatement(testQuery);
                pstmt.setString(1, startDate.toString());
                pstmt.setString(2, endDate.toString());
                
                System.out.println("\n=== Testing Exact Filter Logic (first 10) ===");
                rs = pstmt.executeQuery();
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.printf("%s: %s to %s - Revenue: $%.0f%n",
                        rs.getString("project_id"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getDouble("revenue_amount"));
                }
                System.out.println("Found " + count + " projects using exact filter logic");
                
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}