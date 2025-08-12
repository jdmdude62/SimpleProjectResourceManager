package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;
import java.util.Random;

public class AddBudgetDataToProjects {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        Random random = new Random();
        
        try {
            Class.forName("org.sqlite.JDBC");
            
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                // First, check current state
                String checkQuery = "SELECT COUNT(*) as total, COUNT(budget_amount) as with_budget FROM projects";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(checkQuery)) {
                    if (rs.next()) {
                        System.out.println("Total projects: " + rs.getInt("total"));
                        System.out.println("Projects with budget data: " + rs.getInt("with_budget"));
                    }
                }
                
                // Update all projects with financial data
                String updateQuery = """
                    UPDATE projects 
                    SET budget_amount = ?,
                        revenue_amount = ?,
                        labor_cost = ?,
                        material_cost = ?,
                        travel_cost = ?,
                        other_cost = ?,
                        actual_cost = ?
                    WHERE project_id = ?
                """;
                
                String selectQuery = "SELECT project_id FROM projects";
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(selectQuery);
                     PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                    
                    int count = 0;
                    while (rs.next()) {
                        String projectId = rs.getString("project_id");
                        
                        // Generate realistic financial data
                        double budget = 50000 + random.nextDouble() * 450000; // $50k - $500k
                        double revenue = budget * (0.9 + random.nextDouble() * 0.3); // 90% - 120% of budget
                        
                        // Cost breakdown
                        double laborCost = budget * (0.4 + random.nextDouble() * 0.2); // 40-60% for labor
                        double materialCost = budget * (0.2 + random.nextDouble() * 0.2); // 20-40% for materials
                        double travelCost = budget * (0.05 + random.nextDouble() * 0.1); // 5-15% for travel
                        double otherCost = budget * (0.05 + random.nextDouble() * 0.1); // 5-15% for other
                        
                        double actualCost = laborCost + materialCost + travelCost + otherCost;
                        
                        pstmt.setDouble(1, budget);
                        pstmt.setDouble(2, revenue);
                        pstmt.setDouble(3, laborCost);
                        pstmt.setDouble(4, materialCost);
                        pstmt.setDouble(5, travelCost);
                        pstmt.setDouble(6, otherCost);
                        pstmt.setDouble(7, actualCost);
                        pstmt.setString(8, projectId);
                        
                        pstmt.executeUpdate();
                        count++;
                        
                        System.out.printf("Updated %s - Budget: $%.0f, Revenue: $%.0f, Cost: $%.0f%n",
                            projectId, budget, revenue, actualCost);
                    }
                    
                    System.out.println("\nSuccessfully updated " + count + " projects with financial data");
                }
                
                // Verify the updates
                String verifyQuery = """
                    SELECT COUNT(*) as total,
                           SUM(revenue_amount) as total_revenue,
                           SUM(budget_amount) as total_budget,
                           AVG(revenue_amount) as avg_revenue
                    FROM projects
                    WHERE budget_amount IS NOT NULL
                """;
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(verifyQuery)) {
                    if (rs.next()) {
                        System.out.println("\n=== Verification ===");
                        System.out.println("Projects with budget data: " + rs.getInt("total"));
                        System.out.printf("Total Revenue: $%,.0f%n", rs.getDouble("total_revenue"));
                        System.out.printf("Total Budget: $%,.0f%n", rs.getDouble("total_budget"));
                        System.out.printf("Average Revenue: $%,.0f%n", rs.getDouble("avg_revenue"));
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}