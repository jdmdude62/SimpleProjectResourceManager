package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DoghouseDataLoader {
    
    public static void main(String[] args) {
        System.out.println("=== Loading Doghouse Installation Sample Data ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        
        try (Connection conn = dataSource.getConnection()) {
            // Read SQL file
            String sql = Files.readString(Paths.get("doghouse_data_generator.sql"));
            
            // Remove comments and split into individual statements
            String[] lines = sql.split("\n");
            StringBuilder cleanSql = new StringBuilder();
            for (String line : lines) {
                if (!line.trim().startsWith("--") && !line.trim().isEmpty()) {
                    cleanSql.append(line).append("\n");
                }
            }
            
            // Split into individual statements
            String[] statements = cleanSql.toString().split(";");
            
            Statement stmt = conn.createStatement();
            int successCount = 0;
            
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        stmt.executeUpdate(trimmed);
                        successCount++;
                    } catch (SQLException e) {
                        // Skip if duplicate (already exists)
                        if (!e.getMessage().contains("UNIQUE constraint failed")) {
                            System.err.println("Error executing: " + trimmed.substring(0, Math.min(50, trimmed.length())) + "...");
                            System.err.println("  " + e.getMessage());
                        }
                    }
                }
            }
            
            System.out.println("✓ Executed " + successCount + " SQL statements successfully!");
            
            // Show summary
            System.out.println("\nData Summary:");
            System.out.println("=============");
            
            // Count new technicians
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM resources WHERE id BETWEEN 51 AND 60");
            if (rs.next()) {
                System.out.println("New Field Technicians: " + rs.getInt(1));
            }
            
            // Count doghouse projects
            rs = stmt.executeQuery("SELECT COUNT(*) FROM projects WHERE project_id LIKE 'DH-%'");
            if (rs.next()) {
                System.out.println("Doghouse Projects: " + rs.getInt(1));
            }
            
            // Count January assignments
            rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM assignments a " +
                "JOIN projects p ON a.project_id = p.id " +
                "WHERE p.project_id LIKE 'DH-%' " +
                "AND date(a.start_date) >= date('2025-01-01') " +
                "AND date(a.start_date) <= date('2025-01-31')"
            );
            if (rs.next()) {
                System.out.println("January Doghouse Assignments: " + rs.getInt(1));
            }
            
            // Show some sample projects
            System.out.println("\nSample Doghouse Projects:");
            rs = stmt.executeQuery(
                "SELECT project_id, description, start_date " +
                "FROM projects WHERE project_id LIKE 'DH-%' LIMIT 5"
            );
            while (rs.next()) {
                System.out.println("  " + rs.getString("project_id") + ": " + 
                    rs.getString("description").substring(0, Math.min(40, rs.getString("description").length())) + "..." +
                    " (" + rs.getString("start_date").substring(0, 10) + ")");
            }
            
            // Show team assignments
            System.out.println("\nJanuary 2025 Team Rotation Schedule:");
            System.out.println("====================================");
            System.out.println("Week 1 (Jan 6-10):  Team Alpha builds, Team Bravo installs");
            System.out.println("Week 2 (Jan 13-17): Team Bravo builds, Team Alpha installs");
            System.out.println("Week 3 (Jan 20-24): Team Alpha builds, Team Bravo installs");
            System.out.println("Week 4 (Jan 27-31): Team Bravo builds, Team Alpha installs");
            
            System.out.println("\n✓ Doghouse installation data loaded successfully!");
            System.out.println("Navigate to January 2025 in the application to see the new assignments.");
            
        } catch (Exception e) {
            System.err.println("Error loading doghouse data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}