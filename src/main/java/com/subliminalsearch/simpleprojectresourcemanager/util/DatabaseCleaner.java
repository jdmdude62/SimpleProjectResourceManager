package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

public class DatabaseCleaner {
    
    public static void main(String[] args) {
        System.out.println("=== Database Cleanup Tool ===");
        System.out.println("Removing old test data prior to " + LocalDate.now() + "...\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            // First, show what we're about to delete
            System.out.println("Current database contents:");
            showDatabaseStats(conn);
            
            // Delete old test data (keeping garden-themed data)
            System.out.println("\nDeleting old test data...");
            
            // Delete assignments for old projects
            String deleteOldAssignments = """
                DELETE FROM assignments 
                WHERE project_id IN (
                    SELECT id FROM projects 
                    WHERE name NOT LIKE '%Garden%' 
                    AND name NOT LIKE '%garden%'
                    AND name NOT LIKE '%Plant%'
                    AND name NOT LIKE '%plant%'
                    AND name NOT LIKE '%Seed%'
                    AND name NOT LIKE '%Rose%'
                    AND name NOT LIKE '%Herb%'
                    AND name NOT LIKE '%Tree%'
                    AND name NOT LIKE '%Flower%'
                    AND name NOT LIKE '%Landscape%'
                )
            """;
            
            PreparedStatement stmt = conn.prepareStatement(deleteOldAssignments);
            int deletedAssignments = stmt.executeUpdate();
            System.out.println("Deleted " + deletedAssignments + " old assignments");
            
            // Delete tasks for old projects
            String deleteOldTasks = """
                DELETE FROM tasks 
                WHERE project_id IN (
                    SELECT id FROM projects 
                    WHERE name NOT LIKE '%Garden%' 
                    AND name NOT LIKE '%garden%'
                    AND name NOT LIKE '%Plant%'
                    AND name NOT LIKE '%plant%'
                    AND name NOT LIKE '%Seed%'
                    AND name NOT LIKE '%Rose%'
                    AND name NOT LIKE '%Herb%'
                    AND name NOT LIKE '%Tree%'
                    AND name NOT LIKE '%Flower%'
                    AND name NOT LIKE '%Landscape%'
                )
            """;
            
            stmt = conn.prepareStatement(deleteOldTasks);
            int deletedTasks = stmt.executeUpdate();
            System.out.println("Deleted " + deletedTasks + " old tasks");
            
            // Delete old projects
            String deleteOldProjects = """
                DELETE FROM projects 
                WHERE name NOT LIKE '%Garden%' 
                AND name NOT LIKE '%garden%'
                AND name NOT LIKE '%Plant%'
                AND name NOT LIKE '%plant%'
                AND name NOT LIKE '%Seed%'
                AND name NOT LIKE '%Rose%'
                AND name NOT LIKE '%Herb%'
                AND name NOT LIKE '%Tree%'
                AND name NOT LIKE '%Flower%'
                AND name NOT LIKE '%Landscape%'
            """;
            
            stmt = conn.prepareStatement(deleteOldProjects);
            int deletedProjects = stmt.executeUpdate();
            System.out.println("Deleted " + deletedProjects + " old projects");
            
            // Delete old resources (non-garden themed)
            String deleteOldResources = """
                DELETE FROM resources 
                WHERE name NOT LIKE '%Landscape%'
                AND name NOT LIKE '%Flora%'
                AND name NOT LIKE '%Seedling%'
                AND name NOT LIKE '%Petal%'
                AND name NOT LIKE '%Stone%'
                AND name NOT LIKE '%Terra%'
                AND name NOT LIKE '%Grove%'
                AND name NOT LIKE '%Meadow%'
                AND name NOT LIKE '%Waters%'
                AND name NOT LIKE '%Rain%'
                AND name NOT LIKE '%Oak%'
                AND name NOT LIKE '%Birch%'
                AND name NOT LIKE '%Blade%'
                AND name NOT LIKE '%Hedge%'
                AND name NOT LIKE '%Vine%'
                AND name NOT LIKE '%Shade%'
                AND name NOT LIKE '%Harvest%'
                AND name NOT LIKE '%Rose%'
                AND name NOT LIKE '%Pond%'
                AND name NOT LIKE '%Roots%'
            """;
            
            stmt = conn.prepareStatement(deleteOldResources);
            int deletedResources = stmt.executeUpdate();
            System.out.println("Deleted " + deletedResources + " old resources");
            
            // Delete old project managers (non-garden themed)
            String deleteOldPMs = """
                DELETE FROM project_managers 
                WHERE name NOT LIKE '%Greenthumb%'
                AND name NOT LIKE '%Bloom%'
                AND name NOT LIKE '%Harvest%'
                AND name NOT LIKE '%Thorne%'
            """;
            
            stmt = conn.prepareStatement(deleteOldPMs);
            int deletedPMs = stmt.executeUpdate();
            System.out.println("Deleted " + deletedPMs + " old project managers");
            
            conn.commit();
            
            System.out.println("\n=== Cleanup Complete ===");
            System.out.println("\nRemaining database contents:");
            showDatabaseStats(conn);
            
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
    
    private static void showDatabaseStats(Connection conn) throws Exception {
        Statement stmt = conn.createStatement();
        
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
        if (rs.next()) {
            System.out.println("  Projects: " + rs.getInt(1));
        }
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM resources");
        if (rs.next()) {
            System.out.println("  Resources: " + rs.getInt(1));
        }
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM assignments");
        if (rs.next()) {
            System.out.println("  Assignments: " + rs.getInt(1));
        }
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM tasks");
        if (rs.next()) {
            System.out.println("  Tasks: " + rs.getInt(1));
        }
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM project_managers");
        if (rs.next()) {
            System.out.println("  Project Managers: " + rs.getInt(1));
        }
    }
}