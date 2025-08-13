import java.sql.*;

public class FixDoghouseAssignments {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement stmt = conn.createStatement();
        
        System.out.println("=== FIXING DOGHOUSE ASSIGNMENTS ===\n");
        
        // First check what assignments exist
        System.out.println("Current assignments for doghouse projects:");
        ResultSet rs = stmt.executeQuery(
            "SELECT a.*, p.project_id, r.name as resource_name " +
            "FROM assignments a " +
            "JOIN projects p ON a.project_id = p.id " +
            "JOIN resources r ON a.resource_id = r.id " +
            "WHERE p.project_id LIKE 'DH-%'"
        );
        
        int existingCount = 0;
        while (rs.next()) {
            existingCount++;
            System.out.println("  Found: Project " + rs.getString("project_id") + 
                " -> " + rs.getString("resource_name"));
        }
        System.out.println("Total existing: " + existingCount);
        
        // Delete old assignments and recreate them properly
        System.out.println("\nCleaning up old assignments...");
        stmt.executeUpdate("DELETE FROM assignments WHERE project_id IN (SELECT id FROM projects WHERE project_id LIKE 'DH-%')");
        
        // Add comprehensive assignments for all doghouse projects
        System.out.println("Adding complete assignments for all doghouse projects...");
        int count = 0;
        
        // Week 1: Team Alpha builds (101), Team Bravo installs (102, 103)
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(101, 51, '2025-01-06', '2025-01-09', 0, 0, 0, 'Team lead - workshop', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(101, 52, '2025-01-06', '2025-01-09', 0, 0, 0, 'Build specialist', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(101, 53, '2025-01-06', '2025-01-09', 0, 0, 0, 'Assembly tech', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(101, 54, '2025-01-06', '2025-01-09', 0, 0, 0, 'Quality control', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(101, 55, '2025-01-06', '2025-01-09', 0, 0, 0, 'HVAC specialist', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(102, 56, '2025-01-06', '2025-01-08', 1, 0, 0, 'Install lead', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(102, 57, '2025-01-06', '2025-01-08', 1, 0, 0, 'Foundation', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(102, 58, '2025-01-06', '2025-01-08', 1, 0, 0, 'Assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(103, 59, '2025-01-09', '2025-01-11', 1, 0, 0, 'Install tech', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(103, 60, '2025-01-09', '2025-01-11', 1, 0, 0, 'HVAC install', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        
        // Week 2: Team Bravo builds (104), Team Alpha installs (105, 106)
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(104, 56, '2025-01-13', '2025-01-16', 0, 0, 0, 'Team lead - workshop', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(104, 57, '2025-01-13', '2025-01-16', 0, 0, 0, 'Build specialist', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(104, 58, '2025-01-13', '2025-01-16', 0, 0, 0, 'Assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(104, 59, '2025-01-13', '2025-01-16', 0, 0, 0, 'Quality control', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(104, 60, '2025-01-13', '2025-01-16', 0, 0, 0, 'HVAC specialist', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(105, 51, '2025-01-13', '2025-01-15', 1, 0, 0, 'Install lead', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(105, 52, '2025-01-13', '2025-01-15', 1, 0, 0, 'Foundation', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(105, 53, '2025-01-13', '2025-01-15', 1, 0, 0, 'Assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(106, 54, '2025-01-16', '2025-01-18', 1, 0, 0, 'Install tech', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        count += stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
            "(106, 55, '2025-01-16', '2025-01-18', 1, 0, 0, 'HVAC install', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
        
        System.out.println("Added " + count + " assignments");
        
        // Verify the assignments
        System.out.println("\n=== VERIFICATION ===");
        rs = stmt.executeQuery(
            "SELECT COUNT(*) FROM assignments a " +
            "JOIN projects p ON a.project_id = p.id " +
            "WHERE p.project_id LIKE 'DH-%'"
        );
        if (rs.next()) {
            System.out.println("Total doghouse assignments: " + rs.getInt(1));
        }
        
        // Check assignments in January 2025
        rs = stmt.executeQuery(
            "SELECT COUNT(*) FROM assignments " +
            "WHERE date(start_date) <= date('2025-01-31') " +
            "AND date(end_date) >= date('2025-01-01')"
        );
        if (rs.next()) {
            System.out.println("Total assignments in January 2025: " + rs.getInt(1));
        }
        
        // List some assignments
        System.out.println("\nSample assignments:");
        rs = stmt.executeQuery(
            "SELECT a.*, p.project_id, r.name as resource_name " +
            "FROM assignments a " +
            "JOIN projects p ON a.project_id = p.id " +
            "JOIN resources r ON a.resource_id = r.id " +
            "WHERE date(a.start_date) <= date('2025-01-31') " +
            "AND date(a.end_date) >= date('2025-01-01') " +
            "LIMIT 10"
        );
        
        while (rs.next()) {
            System.out.println("  " + rs.getString("project_id") + " -> " + 
                rs.getString("resource_name") + " (" + 
                rs.getString("start_date") + " to " + rs.getString("end_date") + ")");
        }
        
        conn.close();
        System.out.println("\n✓ Assignments fixed successfully!");
        System.out.println("Restart the application and navigate to January 2025.");
    }
}