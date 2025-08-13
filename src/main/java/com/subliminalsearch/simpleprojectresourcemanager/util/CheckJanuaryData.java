package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class CheckJanuaryData {
    
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        System.out.println("=== January 2025 Data Check ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            
            // Check projects for January 2025
            System.out.println("Projects in January 2025:");
            ResultSet rs = stmt.executeQuery(
                "SELECT project_id, description, start_date, end_date, status " +
                "FROM projects " +
                "WHERE date(start_date) <= date('2025-01-31') " +
                "AND date(end_date) >= date('2025-01-01') " +
                "ORDER BY start_date"
            );
            
            int projectCount = 0;
            while (rs.next()) {
                projectCount++;
                String desc = rs.getString("description");
                if (desc != null && desc.length() > 40) {
                    desc = desc.substring(0, 40) + "...";
                }
                System.out.println("  " + rs.getString("project_id") + ": " + desc +
                    " (" + rs.getString("start_date").substring(0, 10) + " to " + 
                    rs.getString("end_date").substring(0, 10) + ")");
            }
            System.out.println("Total projects: " + projectCount);
            
            // Check assignments for January 2025
            System.out.println("\nAssignments in January 2025:");
            rs = stmt.executeQuery(
                "SELECT a.*, r.name as resource_name, p.project_id " +
                "FROM assignments a " +
                "JOIN resources r ON a.resource_id = r.id " +
                "JOIN projects p ON a.project_id = p.id " +
                "WHERE date(a.start_date) <= date('2025-01-31') " +
                "AND date(a.end_date) >= date('2025-01-01') " +
                "ORDER BY a.start_date " +
                "LIMIT 10"
            );
            
            int assignmentCount = 0;
            while (rs.next()) {
                assignmentCount++;
                System.out.println("  " + rs.getString("project_id") + " - " + 
                    rs.getString("resource_name") + 
                    " (" + rs.getString("start_date").substring(0, 10) + " to " + 
                    rs.getString("end_date").substring(0, 10) + ")");
            }
            
            // Get total count
            rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM assignments a " +
                "WHERE date(a.start_date) <= date('2025-01-31') " +
                "AND date(a.end_date) >= date('2025-01-01')"
            );
            if (rs.next()) {
                int total = rs.getInt(1);
                System.out.println("Total assignments: " + total);
            }
            
            // Check for doghouse projects specifically
            System.out.println("\nDoghouse Projects (DH-*):");
            rs = stmt.executeQuery("SELECT COUNT(*) FROM projects WHERE project_id LIKE 'DH-%'");
            if (rs.next()) {
                System.out.println("  Total doghouse projects in database: " + rs.getInt(1));
            }
            
            // Check for new technicians
            System.out.println("\nField Technicians (IDs 51-60):");
            rs = stmt.executeQuery("SELECT id, name FROM resources WHERE id BETWEEN 51 AND 60");
            int techCount = 0;
            while (rs.next()) {
                techCount++;
                System.out.println("  " + rs.getInt("id") + ": " + rs.getString("name"));
            }
            System.out.println("Total new technicians: " + techCount);
            
            System.out.println("\n=== Check Complete ===");
            
            // Now let's load the doghouse data if it's not there
            if (techCount == 0) {
                System.out.println("\n!!! Doghouse data not found. Loading now...");
                
                // Load the SQL
                java.nio.file.Path sqlPath = java.nio.file.Paths.get("doghouse_data_generator.sql");
                String sql = java.nio.file.Files.readString(sqlPath);
                
                // Remove comments
                String[] lines = sql.split("\n");
                StringBuilder cleanSql = new StringBuilder();
                for (String line : lines) {
                    if (!line.trim().startsWith("--") && !line.trim().isEmpty()) {
                        cleanSql.append(line).append("\n");
                    }
                }
                
                // Execute statements
                String[] statements = cleanSql.toString().split(";");
                int loaded = 0;
                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            stmt.executeUpdate(trimmed);
                            loaded++;
                        } catch (SQLException e) {
                            // Skip duplicates
                            if (!e.getMessage().contains("UNIQUE constraint")) {
                                System.err.println("Error: " + e.getMessage());
                            }
                        }
                    }
                }
                
                System.out.println("✓ Loaded " + loaded + " SQL statements");
                System.out.println("✓ Doghouse data loaded successfully!");
                System.out.println("\nRestart the application and navigate to January 2025 to see the new data.");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}