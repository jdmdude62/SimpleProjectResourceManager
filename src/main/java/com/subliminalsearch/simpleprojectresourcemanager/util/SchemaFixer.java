package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class SchemaFixer {
    
    public static void main(String[] args) {
        System.out.println("=== Schema Checker & Fixer ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            
            System.out.println("Step 1: Checking current schema...\n");
            
            // Check what tables exist
            ResultSet rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
            );
            
            System.out.println("Existing tables:");
            while (rs.next()) {
                String tableName = rs.getString(1);
                System.out.println("  - " + tableName);
                
                // Show columns for each table
                ResultSet cols = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");
                System.out.println("    Columns:");
                while (cols.next()) {
                    System.out.println("      • " + cols.getString("name") + " (" + cols.getString("type") + ")");
                }
                cols.close();
            }
            rs.close();
            
            System.out.println("\nStep 2: Creating/updating schema...\n");
            
            // Create tables with proper schema
            String[] createStatements = {
                // Project Managers table
                "CREATE TABLE IF NOT EXISTS project_managers (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    name TEXT NOT NULL," +
                "    email TEXT," +
                "    phone TEXT" +
                ")",
                
                // Resources table  
                "CREATE TABLE IF NOT EXISTS resources (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    name TEXT NOT NULL," +
                "    role TEXT," +
                "    email TEXT," +
                "    phone TEXT," +
                "    hourly_rate REAL," +
                "    max_hours_per_week INTEGER DEFAULT 40" +
                ")",
                
                // Projects table
                "CREATE TABLE IF NOT EXISTS projects (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    name TEXT NOT NULL," +
                "    description TEXT," +
                "    project_manager_id INTEGER," +
                "    start_date DATE," +
                "    end_date DATE," +
                "    budget REAL," +
                "    status TEXT DEFAULT 'PLANNED'," +
                "    priority TEXT DEFAULT 'MEDIUM'," +
                "    location TEXT," +
                "    travel_days INTEGER DEFAULT 0," +
                "    client_name TEXT," +
                "    FOREIGN KEY (project_manager_id) REFERENCES project_managers(id)" +
                ")",
                
                // Tasks table
                "CREATE TABLE IF NOT EXISTS tasks (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    project_id INTEGER NOT NULL," +
                "    name TEXT NOT NULL," +
                "    description TEXT," +
                "    start_date DATE," +
                "    end_date DATE," +
                "    estimated_hours REAL," +
                "    actual_hours REAL," +
                "    status TEXT DEFAULT 'NOT_STARTED'," +
                "    priority INTEGER DEFAULT 5," +
                "    dependencies TEXT," +
                "    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE" +
                ")",
                
                // Assignments table
                "CREATE TABLE IF NOT EXISTS assignments (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    task_id INTEGER," +
                "    project_id INTEGER," +
                "    resource_id INTEGER NOT NULL," +
                "    start_date DATE NOT NULL," +
                "    end_date DATE NOT NULL," +
                "    allocation_percentage INTEGER DEFAULT 100," +
                "    notes TEXT," +
                "    status TEXT DEFAULT 'ASSIGNED'," +
                "    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE," +
                "    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE," +
                "    FOREIGN KEY (resource_id) REFERENCES resources(id)" +
                ")"
            };
            
            for (String sql : createStatements) {
                try {
                    stmt.execute(sql);
                    System.out.println("  ✓ Table created/verified");
                } catch (Exception e) {
                    System.out.println("  ⚠ " + e.getMessage());
                }
            }
            
            // Now clear all data
            System.out.println("\nStep 3: Clearing all existing data...\n");
            stmt.executeUpdate("DELETE FROM assignments");
            stmt.executeUpdate("DELETE FROM tasks");
            stmt.executeUpdate("DELETE FROM projects");
            stmt.executeUpdate("DELETE FROM resources");
            stmt.executeUpdate("DELETE FROM project_managers");
            System.out.println("  ✓ All data cleared");
            
            // Load garden data
            System.out.println("\nStep 4: Loading garden demo data...\n");
            
            // Insert project managers
            stmt.executeUpdate("INSERT INTO project_managers (id, name, email, phone) VALUES " +
                "(1, 'Sarah Green', 'sarah.green@gardens.com', '555-0101')," +
                "(2, 'Michael Bloom', 'michael.bloom@gardens.com', '555-0102')," +
                "(3, 'Jennifer Rose', 'jennifer.rose@gardens.com', '555-0103')," +
                "(4, 'David Meadows', 'david.meadows@gardens.com', '555-0104')");
            
            // Insert resources
            stmt.executeUpdate("INSERT INTO resources (id, name, role, email, phone, hourly_rate, max_hours_per_week) VALUES " +
                "(1, 'Tom Hardy', 'Landscape Architect', 'tom.hardy@gardens.com', '555-1001', 125.00, 40)," +
                "(2, 'Emma Stone', 'Garden Designer', 'emma.stone@gardens.com', '555-1002', 95.00, 40)," +
                "(3, 'Chris Pine', 'Horticulturist', 'chris.pine@gardens.com', '555-1003', 85.00, 40)," +
                "(4, 'Maya Petal', 'Irrigation Specialist', 'maya.petal@gardens.com', '555-1004', 90.00, 40)," +
                "(5, 'Alex Rivers', 'Soil Engineer', 'alex.rivers@gardens.com', '555-1005', 110.00, 40)," +
                "(6, 'Lisa Fern', 'Master Gardener', 'lisa.fern@gardens.com', '555-1006', 75.00, 40)," +
                "(7, 'James Oak', 'Arborist', 'james.oak@gardens.com', '555-1007', 95.00, 40)," +
                "(8, 'Sophie Vine', 'Permaculture Expert', 'sophie.vine@gardens.com', '555-1008', 105.00, 40)," +
                "(9, 'Robert Moss', 'Hardscape Specialist', 'robert.moss@gardens.com', '555-1009', 100.00, 40)," +
                "(10, 'Diana Lily', 'Plant Pathologist', 'diana.lily@gardens.com', '555-1010', 115.00, 40)");
            
            // Insert projects (sample of 12)
            stmt.executeUpdate("INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES " +
                "(1, 'Urban Rooftop Garden', 'Transform 5000 sq ft rooftop into productive garden', 1, '2025-01-06', '2025-04-30', 125000, 'ACTIVE', 'HIGH', 'Downtown Office Tower', 0, 'Metro Development Corp')," +
                "(2, 'Community Herb Spiral', 'Create educational herb garden for local school', 2, '2025-01-15', '2025-02-28', 35000, 'ACTIVE', 'MEDIUM', 'Riverside Elementary School', 1, 'School District 42')," +
                "(3, 'Japanese Zen Garden', 'Traditional Japanese garden with koi pond', 3, '2025-02-01', '2025-05-31', 85000, 'PLANNED', 'HIGH', 'Private Estate - Hills', 2, 'The Yamamoto Family')," +
                "(4, 'Pollinator Paradise', 'Native plant garden to support local pollinators', 4, '2025-02-15', '2025-04-15', 45000, 'PLANNED', 'MEDIUM', 'City Park North', 0, 'Parks Department')," +
                "(5, 'Victorian Rose Garden', 'Restoration of historic rose garden', 1, '2025-03-01', '2025-06-30', 95000, 'PLANNED', 'HIGH', 'Heritage Museum', 1, 'Historical Society')," +
                "(6, 'Edible Forest Garden', 'Food forest with fruit trees and perennials', 2, '2025-03-15', '2025-07-31', 78000, 'PLANNED', 'MEDIUM', 'Community Center', 0, 'Green Future Foundation')," +
                "(7, 'Desert Xeriscape', 'Water-wise landscaping for arid climate', 3, '2025-04-01', '2025-06-15', 56000, 'PLANNED', 'LOW', 'Desert Ridge HOA', 3, 'Desert Ridge Association')," +
                "(8, 'Healing Garden', 'Therapeutic garden for hospital patients', 4, '2025-04-15', '2025-07-15', 68000, 'PLANNED', 'HIGH', 'General Hospital', 0, 'Regional Medical Center')," +
                "(9, 'School Garden Lab', 'Outdoor classroom with raised beds', 1, '2025-05-01', '2025-06-30', 42000, 'PLANNED', 'MEDIUM', 'Oakwood Middle School', 1, 'Oakwood School Board')," +
                "(10, 'Rain Garden System', 'Bioswales and rain gardens for stormwater', 2, '2025-05-15', '2025-08-31', 89000, 'PLANNED', 'HIGH', 'Tech Campus', 0, 'InnovateTech Inc')," +
                "(11, 'Mediterranean Courtyard', 'Drought-tolerant Mediterranean landscape', 3, '2025-06-01', '2025-08-15', 52000, 'PLANNED', 'MEDIUM', 'Villa Rosa', 2, 'The Martinez Family')," +
                "(12, 'Butterfly Sanctuary', 'Native habitat for monarch butterflies', 4, '2025-06-15', '2025-09-30', 71000, 'PLANNED', 'HIGH', 'Nature Preserve', 1, 'Wildlife Conservation')");
            
            // Insert sample tasks
            stmt.executeUpdate("INSERT INTO tasks (project_id, name, description, start_date, end_date, estimated_hours, status, priority) VALUES " +
                "(1, 'Site Assessment', 'Structural analysis and measurements', '2025-01-06', '2025-01-10', 40, 'NOT_STARTED', 1)," +
                "(1, 'Design Development', 'Create detailed garden layout', '2025-01-13', '2025-01-24', 80, 'NOT_STARTED', 2)," +
                "(1, 'Soil Preparation', 'Import and prepare growing medium', '2025-01-27', '2025-02-07', 120, 'NOT_STARTED', 3)," +
                "(2, 'Site Clearing', 'Clear and level the area', '2025-01-15', '2025-01-17', 24, 'NOT_STARTED', 1)," +
                "(2, 'Spiral Construction', 'Build the herb spiral structure', '2025-01-20', '2025-01-24', 40, 'NOT_STARTED', 2)");
            
            // Insert sample assignments
            stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, allocation_percentage, notes, status) VALUES " +
                "(1, 1, '2025-01-06', '2025-01-24', 100, 'Lead architect for rooftop project', 'ASSIGNED')," +
                "(1, 5, '2025-01-27', '2025-02-07', 100, 'Soil engineering and preparation', 'ASSIGNED')," +
                "(2, 2, '2025-01-15', '2025-01-24', 50, 'Design herb spiral layout', 'ASSIGNED')," +
                "(2, 6, '2025-01-20', '2025-02-28', 75, 'Plant selection and installation', 'ASSIGNED')");
            
            conn.commit();
            
            System.out.println("  ✓ Garden demo data loaded successfully");
            
            // Verify counts
            System.out.println("\nStep 5: Verifying data...\n");
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM project_managers");
            if (rs.next()) System.out.println("  Project Managers: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM resources");
            if (rs.next()) System.out.println("  Resources: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
            if (rs.next()) System.out.println("  Projects: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM tasks");
            if (rs.next()) System.out.println("  Tasks: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM assignments");
            if (rs.next()) System.out.println("  Assignments: " + rs.getInt(1));
            
            System.out.println("\n=== Schema Fixed & Data Loaded! ===");
            System.out.println("Please restart the application to see the garden projects.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}