package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;

public class AddBudgetFieldsToDatabase {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        
        try {
            Class.forName("org.sqlite.JDBC");
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement()) {
                
                // Check if columns already exist
                DatabaseMetaData metadata = conn.getMetaData();
                ResultSet columns = metadata.getColumns(null, null, "projects", "budget_amount");
                
                if (!columns.next()) {
                    System.out.println("Adding budget and financial columns to projects table...");
                    
                    // Add budget and financial columns
                    String[] alterStatements = {
                        "ALTER TABLE projects ADD COLUMN budget_amount REAL",
                        "ALTER TABLE projects ADD COLUMN actual_cost REAL",
                        "ALTER TABLE projects ADD COLUMN revenue_amount REAL",
                        "ALTER TABLE projects ADD COLUMN currency_code TEXT DEFAULT 'USD'",
                        "ALTER TABLE projects ADD COLUMN labor_cost REAL",
                        "ALTER TABLE projects ADD COLUMN material_cost REAL",
                        "ALTER TABLE projects ADD COLUMN travel_cost REAL",
                        "ALTER TABLE projects ADD COLUMN other_cost REAL",
                        "ALTER TABLE projects ADD COLUMN cost_notes TEXT"
                    };
                    
                    for (String sql : alterStatements) {
                        try {
                            stmt.execute(sql);
                            System.out.println("  ✓ Added column: " + sql.substring(sql.indexOf("ADD COLUMN") + 11, sql.indexOf(" ", sql.indexOf("ADD COLUMN") + 11)));
                        } catch (Exception e) {
                            System.out.println("  Column may already exist: " + e.getMessage());
                        }
                    }
                    
                    System.out.println("\nBudget columns added successfully!");
                    
                    // Add sample budget data to some projects
                    System.out.println("\nAdding sample budget data to existing projects...");
                    
                    String[] sampleUpdates = {
                        "UPDATE projects SET budget_amount = 150000, revenue_amount = 175000, labor_cost = 80000, material_cost = 30000, travel_cost = 5000 WHERE project_id LIKE 'CH-PBLD-%' AND budget_amount IS NULL",
                        "UPDATE projects SET budget_amount = 250000, revenue_amount = 280000, labor_cost = 120000, material_cost = 60000, travel_cost = 10000, other_cost = 15000 WHERE project_id LIKE 'CH-INST-%' AND budget_amount IS NULL",
                        "UPDATE projects SET budget_amount = 75000, revenue_amount = 90000, labor_cost = 40000, material_cost = 15000, travel_cost = 3000 WHERE project_id LIKE 'CH-MAINT-%' AND budget_amount IS NULL",
                        "UPDATE projects SET budget_amount = 200000, revenue_amount = 225000, labor_cost = 100000, material_cost = 45000, travel_cost = 8000, other_cost = 12000 WHERE project_id LIKE 'CH-UPG-%' AND budget_amount IS NULL"
                    };
                    
                    for (String sql : sampleUpdates) {
                        int updated = stmt.executeUpdate(sql);
                        if (updated > 0) {
                            String projectType = sql.substring(sql.indexOf("LIKE '") + 6, sql.indexOf("-%'"));
                            System.out.println("  ✓ Updated " + updated + " " + projectType + " projects with sample budget data");
                        }
                    }
                    
                } else {
                    System.out.println("Budget columns already exist in the projects table.");
                    
                    // Check how many projects have budget data
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total, COUNT(budget_amount) as with_budget FROM projects");
                    if (rs.next()) {
                        int total = rs.getInt("total");
                        int withBudget = rs.getInt("with_budget");
                        System.out.println("Projects with budget data: " + withBudget + " out of " + total);
                        
                        if (withBudget == 0) {
                            System.out.println("\nAdding sample budget data to existing projects...");
                            
                            String[] sampleUpdates = {
                                "UPDATE projects SET budget_amount = 150000, revenue_amount = 175000, labor_cost = 80000, material_cost = 30000, travel_cost = 5000 WHERE project_id LIKE 'CH-PBLD-%' AND budget_amount IS NULL",
                                "UPDATE projects SET budget_amount = 250000, revenue_amount = 280000, labor_cost = 120000, material_cost = 60000, travel_cost = 10000, other_cost = 15000 WHERE project_id LIKE 'CH-INST-%' AND budget_amount IS NULL",
                                "UPDATE projects SET budget_amount = 75000, revenue_amount = 90000, labor_cost = 40000, material_cost = 15000, travel_cost = 3000 WHERE project_id LIKE 'CH-MAINT-%' AND budget_amount IS NULL",
                                "UPDATE projects SET budget_amount = 200000, revenue_amount = 225000, labor_cost = 100000, material_cost = 45000, travel_cost = 8000, other_cost = 12000 WHERE project_id LIKE 'CH-UPG-%' AND budget_amount IS NULL"
                            };
                            
                            for (String sql : sampleUpdates) {
                                int updated = stmt.executeUpdate(sql);
                                if (updated > 0) {
                                    String projectType = sql.substring(sql.indexOf("LIKE '") + 6, sql.indexOf("-%'"));
                                    System.out.println("  ✓ Updated " + updated + " " + projectType + " projects with sample budget data");
                                }
                            }
                        }
                    }
                }
                
                System.out.println("\nDatabase update complete!");
                
            }
        } catch (Exception e) {
            System.err.println("Error updating database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}