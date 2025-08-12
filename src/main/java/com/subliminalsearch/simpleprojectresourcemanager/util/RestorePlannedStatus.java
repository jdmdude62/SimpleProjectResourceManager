package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RestorePlannedStatus {
    
    public static void main(String[] args) {
        System.out.println("=== Restore PLANNED Status ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            
            System.out.println("Setting appropriate projects to PLANNED status...");
            
            // Get today's date in the right format
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00.000";
            
            // Set projects with future start dates to PLANNED
            int updated = stmt.executeUpdate(
                "UPDATE projects SET status = 'PLANNED' " +
                "WHERE start_date > '" + today + "' " +
                "AND project_id NOT IN ('Urban Rooftop Garden', 'Community Herb Spiral', 'Permaculture Demo', 'Memorial Garden')"
            );
            System.out.println("  ✓ Set " + updated + " future projects to PLANNED status");
            
            // Keep these specific projects as ACTIVE (they're currently underway)
            String[] activeProjects = {
                "Urban Rooftop Garden",
                "Community Herb Spiral", 
                "Permaculture Demo",
                "Memorial Garden"
            };
            
            System.out.println("\nKeeping these projects as ACTIVE:");
            for (String project : activeProjects) {
                System.out.println("  • " + project);
            }
            
            // Show the distribution
            System.out.println("\nFinal project status distribution:");
            ResultSet rs = stmt.executeQuery("SELECT status, COUNT(*) as count FROM projects GROUP BY status ORDER BY status");
            while (rs.next()) {
                System.out.println("  " + rs.getString("status") + ": " + rs.getInt("count") + " projects");
            }
            
            // Show some examples
            System.out.println("\nSample projects:");
            rs = stmt.executeQuery("SELECT project_id, start_date, status FROM projects ORDER BY start_date LIMIT 10");
            while (rs.next()) {
                String date = rs.getString("start_date").substring(0, 10); // Just the date part
                System.out.println("  • " + rs.getString("project_id") + " (" + date + ") - " + rs.getString("status"));
            }
            
            System.out.println("\n=== Status Distribution Complete! ===");
            System.out.println("Please restart the application.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}