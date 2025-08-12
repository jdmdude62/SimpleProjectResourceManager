package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;

public class LoadDoghouseDataDirect {
    
    static {
        System.setProperty("java.awt.headless", "true");
    }
    
    public static void main(String[] args) {
        System.exit(doLoad());
    }
    
    private static int doLoad() {
        System.out.println("=== Loading Doghouse Data Directly ===\n");
        
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement stmt = conn.createStatement();
            
            // First check if data already exists
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM resources WHERE id = 51");
            boolean dataExists = false;
            if (rs.next() && rs.getInt(1) > 0) {
                dataExists = true;
                System.out.println("Doghouse technicians already exist!");
            }
            
            if (!dataExists) {
                System.out.println("Adding 10 field technicians...");
                
                // Add Team Alpha (IDs 51-55)
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(51, 'Tech Team Alpha - Jake Morrison', 'jake.morrison@company.com', '555-0151', 'Field Services', 'FIELD_TECHNICIAN', 'SENIOR', 85.00, 85.0)");
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(52, 'Tech Team Alpha - Maria Santos', 'maria.santos@company.com', '555-0152', 'Field Services', 'FIELD_TECHNICIAN', 'MID', 75.00, 85.0)");
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(53, 'Tech Team Alpha - David Chen', 'david.chen@company.com', '555-0153', 'Field Services', 'FIELD_TECHNICIAN', 'MID', 75.00, 85.0)");
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(54, 'Tech Team Alpha - Sarah Johnson', 'sarah.johnson@company.com', '555-0154', 'Field Services', 'FIELD_TECHNICIAN', 'JUNIOR', 65.00, 85.0)");
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(55, 'Tech Team Alpha - Mike Williams', 'mike.williams@company.com', '555-0155', 'Field Services', 'FIELD_TECHNICIAN', 'JUNIOR', 65.00, 85.0)");
                
                // Add Team Bravo (IDs 56-60)
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(56, 'Tech Team Bravo - Tom Anderson', 'tom.anderson@company.com', '555-0156', 'Field Services', 'FIELD_TECHNICIAN', 'SENIOR', 85.00, 85.0)");
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(57, 'Tech Team Bravo - Lisa Park', 'lisa.park@company.com', '555-0157', 'Field Services', 'FIELD_TECHNICIAN', 'MID', 75.00, 85.0)");
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(58, 'Tech Team Bravo - James Wilson', 'james.wilson@company.com', '555-0158', 'Field Services', 'FIELD_TECHNICIAN', 'MID', 75.00, 85.0)");
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(59, 'Tech Team Bravo - Emily Davis', 'emily.davis@company.com', '555-0159', 'Field Services', 'FIELD_TECHNICIAN', 'JUNIOR', 65.00, 85.0)");
                stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES " +
                    "(60, 'Tech Team Bravo - Chris Martinez', 'chris.martinez@company.com', '555-0160', 'Field Services', 'FIELD_TECHNICIAN', 'JUNIOR', 65.00, 85.0)");
                
                System.out.println("✓ Added 10 field technicians");
            }
            
            // Check if doghouse projects exist
            rs = stmt.executeQuery("SELECT COUNT(*) FROM projects WHERE project_id LIKE 'DH-%'");
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("\nAdding doghouse projects for January 2025...");
                
                // Add some January doghouse projects
                // Week 1: Team Alpha builds, Team Bravo installs
                stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(101, 'DH-2025-001-BUILD', 'Doghouse Build - Batch 001 (5 units) - Workshop', 2, '2025-01-06 08:00:00.000', '2025-01-09 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'First batch of temperature-controlled doghouses for Q1')");
                stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(102, 'DH-2025-001-INSTALL', 'Doghouse Install - Johnson @ Oak Street', 3, '2025-01-06 08:00:00.000', '2025-01-08 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC')");
                
                // Week 2: Team Bravo builds, Team Alpha installs  
                stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(104, 'DH-2025-002-BUILD', 'Doghouse Build - Batch 002 (5 units) - Workshop', 2, '2025-01-13 08:00:00.000', '2025-01-16 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'Second batch for January deliveries')");
                stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(105, 'DH-2025-003-INSTALL', 'Doghouse Install - Davis @ Pine Road', 3, '2025-01-13 08:00:00.000', '2025-01-15 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC')");
                
                System.out.println("✓ Added 4 sample doghouse projects");
                
                // Add some assignments
                System.out.println("\nAdding assignments for doghouse projects...");
                
                // Week 1: Alpha builds
                stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(101, 51, '2025-01-06', '2025-01-09', 0, 0, 0, 'Team lead - workshop build', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(101, 52, '2025-01-06', '2025-01-09', 0, 0, 0, 'Build specialist - 1st floor', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                
                // Week 1: Bravo installs
                stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(102, 56, '2025-01-06', '2025-01-08', 1, 0, 0, 'Installation lead', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(102, 57, '2025-01-06', '2025-01-08', 1, 0, 0, 'Foundation and assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                
                System.out.println("✓ Added sample assignments");
            } else if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Doghouse projects already exist!");
            }
            
            // Now check what's in January 2025
            System.out.println("\n=== January 2025 Data Summary ===");
            
            rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM projects " +
                "WHERE date(start_date) <= date('2025-01-31') " +
                "AND date(end_date) >= date('2025-01-01')"
            );
            if (rs.next()) {
                System.out.println("Total projects in January 2025: " + rs.getInt(1));
            }
            
            rs = stmt.executeQuery(
                "SELECT project_id, description FROM projects " +
                "WHERE date(start_date) <= date('2025-01-31') " +
                "AND date(end_date) >= date('2025-01-01') " +
                "ORDER BY start_date LIMIT 10"
            );
            System.out.println("\nProjects:");
            while (rs.next()) {
                String desc = rs.getString("description");
                if (desc.length() > 40) desc = desc.substring(0, 40) + "...";
                System.out.println("  - " + rs.getString("project_id") + ": " + desc);
            }
            
            conn.close();
            System.out.println("\n✓ Data loading complete!");
            System.out.println("Restart the application and navigate to January 2025.");
            return 0;
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}