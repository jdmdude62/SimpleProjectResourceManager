import java.sql.*;

public class CheckJanuaryData {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement stmt = conn.createStatement();
        
        System.out.println("=== January 2025 Data Check ===\n");
        
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
            System.out.println("  " + rs.getString("project_id") + ": " + 
                rs.getString("description").substring(0, Math.min(40, rs.getString("description").length())) +
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
            "ORDER BY a.start_date"
        );
        
        int assignmentCount = 0;
        while (rs.next()) {
            assignmentCount++;
            System.out.println("  " + rs.getString("project_id") + " - " + 
                rs.getString("resource_name") + 
                " (" + rs.getString("start_date").substring(0, 10) + " to " + 
                rs.getString("end_date").substring(0, 10) + ")");
        }
        System.out.println("Total assignments: " + assignmentCount);
        
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
        
        conn.close();
        System.out.println("\n=== Check Complete ===");
    }
}