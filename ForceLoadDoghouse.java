import java.sql.*;

public class ForceLoadDoghouse {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement stmt = conn.createStatement();
        
        System.out.println("=== FORCE LOADING DOGHOUSE PROJECTS ===\n");
        
        // First delete any existing doghouse projects to start fresh
        System.out.println("Cleaning up old doghouse data...");
        stmt.executeUpdate("DELETE FROM assignments WHERE project_id IN (SELECT id FROM projects WHERE project_id LIKE 'DH-%')");
        stmt.executeUpdate("DELETE FROM projects WHERE project_id LIKE 'DH-%'");
        
        // Add doghouse projects for January 2025
        System.out.println("Adding doghouse projects...");
        int count = 0;
        
        // Week 1: Team Alpha builds, Team Bravo installs
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(101, 'DH-2025-001-BUILD', 'Doghouse Build - Batch 001 (5 units) - Workshop', 2, '2025-01-06 08:00:00.000', '2025-01-09 17:00:00.000', 'PLANNED')");
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(102, 'DH-2025-001-INSTALL', 'Doghouse Install - Johnson @ Oak Street', 3, '2025-01-06 08:00:00.000', '2025-01-08 17:00:00.000', 'PLANNED')");
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(103, 'DH-2025-002-INSTALL', 'Doghouse Install - Smith @ Maple Ave', 3, '2025-01-09 08:00:00.000', '2025-01-11 17:00:00.000', 'PLANNED')");
        
        // Week 2: Team Bravo builds, Team Alpha installs
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(104, 'DH-2025-002-BUILD', 'Doghouse Build - Batch 002 (5 units) - Workshop', 2, '2025-01-13 08:00:00.000', '2025-01-16 17:00:00.000', 'PLANNED')");
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(105, 'DH-2025-003-INSTALL', 'Doghouse Install - Davis @ Pine Road', 3, '2025-01-13 08:00:00.000', '2025-01-15 17:00:00.000', 'PLANNED')");
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(106, 'DH-2025-004-INSTALL', 'Doghouse Install - Wilson @ Elm Drive', 3, '2025-01-16 08:00:00.000', '2025-01-18 17:00:00.000', 'PLANNED')");
        
        // Week 3: Team Alpha builds, Team Bravo installs
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(107, 'DH-2025-003-BUILD', 'Doghouse Build - Batch 003 (6 units) - Workshop', 2, '2025-01-20 08:00:00.000', '2025-01-23 17:00:00.000', 'PLANNED')");
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(108, 'DH-2025-005-INSTALL', 'Doghouse Install - Martinez @ Cedar Blvd', 3, '2025-01-20 08:00:00.000', '2025-01-22 17:00:00.000', 'PLANNED')");
        
        // Week 4: Team Bravo builds, Team Alpha installs
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(109, 'DH-2025-004-BUILD', 'Doghouse Build - Batch 004 (5 units) - Workshop', 2, '2025-01-27 08:00:00.000', '2025-01-30 17:00:00.000', 'PLANNED')");
        count += stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " +
            "(110, 'DH-2025-006-INSTALL', 'Doghouse Install - Thompson @ Birch Lane', 3, '2025-01-27 08:00:00.000', '2025-01-29 17:00:00.000', 'PLANNED')");
        
        System.out.println("Added " + count + " doghouse projects");
        
        // Add assignments for the projects
        System.out.println("\nAdding assignments...");
        int assignCount = 0;
        
        // Week 1 assignments - Team Alpha builds
        assignCount += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(101, 51, '2025-01-06', '2025-01-09', 0, 0, 0, 'Team lead - workshop', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        assignCount += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(101, 52, '2025-01-06', '2025-01-09', 0, 0, 0, 'Build specialist', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        assignCount += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(101, 53, '2025-01-06', '2025-01-09', 0, 0, 0, 'Assembly technician', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        
        // Week 1 - Team Bravo installs
        assignCount += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(102, 56, '2025-01-06', '2025-01-08', 1, 0, 0, 'Installation lead', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        assignCount += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(102, 57, '2025-01-06', '2025-01-08', 1, 0, 0, 'Foundation specialist', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        
        System.out.println("Added " + assignCount + " assignments");
        
        // Verify the data
        System.out.println("\n=== VERIFICATION ===");
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM projects WHERE project_id LIKE 'DH-%'");
        if (rs.next()) {
            System.out.println("Doghouse projects in database: " + rs.getInt(1));
        }
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM projects WHERE date(start_date) <= date('2025-01-31') AND date(end_date) >= date('2025-01-01')");
        if (rs.next()) {
            System.out.println("Total January 2025 projects: " + rs.getInt(1));
        }
        
        System.out.println("\nProjects in January:");
        rs = stmt.executeQuery("SELECT project_id, description FROM projects WHERE date(start_date) <= date('2025-01-31') AND date(end_date) >= date('2025-01-01') ORDER BY start_date");
        while (rs.next()) {
            String desc = rs.getString("description");
            if (desc.length() > 40) desc = desc.substring(0, 40) + "...";
            System.out.println("  " + rs.getString("project_id") + ": " + desc);
        }
        
        conn.close();
        System.out.println("\n✓ Doghouse data force-loaded successfully!");
        System.out.println("Restart the application and navigate to January 2025.");
    }
}