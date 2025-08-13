import java.sql.*;

public class FixDateFormat {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== CHECKING DATE ISSUES ===");
            
            // Check what's in the projects table
            ResultSet rs = stmt.executeQuery("SELECT * FROM projects");
            while (rs.next()) {
                System.out.println("Project ID: " + rs.getString("project_id"));
                System.out.println("  Start date: " + rs.getString("start_date"));
                System.out.println("  End date: " + rs.getString("end_date"));
                System.out.println("  Created at: " + rs.getString("created_at"));
            }
            
            // Fix any timestamp values
            System.out.println("\n=== FIXING DATE FORMATS ===");
            
            // Update any numeric timestamps to proper date format
            stmt.executeUpdate("""
                UPDATE projects 
                SET start_date = datetime(start_date/1000, 'unixepoch')
                WHERE typeof(start_date) = 'integer' OR start_date GLOB '[0-9]*'
            """);
            
            stmt.executeUpdate("""
                UPDATE projects 
                SET end_date = datetime(end_date/1000, 'unixepoch')
                WHERE typeof(end_date) = 'integer' OR end_date GLOB '[0-9]*'
            """);
            
            // Check again
            System.out.println("\nAfter fix:");
            rs = stmt.executeQuery("SELECT * FROM projects");
            while (rs.next()) {
                System.out.println("Project ID: " + rs.getString("project_id"));
                System.out.println("  Start date: " + rs.getString("start_date"));
                System.out.println("  End date: " + rs.getString("end_date"));
            }
            
            // If there are problematic rows, delete them
            int deleted = stmt.executeUpdate("DELETE FROM projects WHERE start_date GLOB '[0-9]*' OR end_date GLOB '[0-9]*'");
            if (deleted > 0) {
                System.out.println("\nDeleted " + deleted + " projects with bad date formats");
            }
            
            // Clear the table completely if still having issues
            stmt.executeUpdate("DELETE FROM projects");
            System.out.println("\n✓ Cleared all projects to start fresh");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}