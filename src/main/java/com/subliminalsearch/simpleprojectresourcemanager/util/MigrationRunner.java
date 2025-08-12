package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class MigrationRunner {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            
            // Check if created_at column already exists
            var rs = stmt.executeQuery("PRAGMA table_info(tasks)");
            boolean hasCreatedAt = false;
            while (rs.next()) {
                if ("created_at".equals(rs.getString("name"))) {
                    hasCreatedAt = true;
                    break;
                }
            }
            rs.close();
            
            if (!hasCreatedAt) {
                System.out.println("Adding created_at column to tasks table...");
                
                // Add created_at column
                stmt.execute("ALTER TABLE tasks ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                
                // Update existing tasks with created_at based on ID
                stmt.execute("UPDATE tasks SET created_at = datetime('2025-01-01 00:00:00', '+' || id || ' seconds') WHERE created_at IS NULL");
                
                // Create index for performance
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_tasks_planned_start_created ON tasks(planned_start, created_at)");
                
                System.out.println("Migration completed successfully!");
            } else {
                System.out.println("created_at column already exists, skipping migration.");
            }
            
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}