import java.sql.*;

public class DropAndRecreate {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            // Check what's in projects table
            System.out.println("=== CHECKING DATABASE ===");
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
            if (rs.next()) {
                System.out.println("Projects in database: " + rs.getInt(1));
            }
            
            rs = stmt.executeQuery("SELECT id, project_id, description FROM projects LIMIT 10");
            while (rs.next()) {
                System.out.println("  - ID: " + rs.getInt("id") + " | " + rs.getString("project_id") + " | " + rs.getString("description"));
            }
            
            System.out.println("\n=== FORCEFULLY CLEARING ===");
            
            // Drop and recreate projects table
            stmt.executeUpdate("DROP TABLE IF EXISTS projects_backup");
            stmt.executeUpdate("ALTER TABLE projects RENAME TO projects_backup");
            
            // Recreate projects table
            stmt.executeUpdate("""
                CREATE TABLE projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id TEXT NOT NULL UNIQUE,
                    description TEXT,
                    project_manager_id INTEGER,
                    start_date TEXT NOT NULL,
                    end_date TEXT NOT NULL,
                    status TEXT NOT NULL,
                    FOREIGN KEY (project_manager_id) REFERENCES project_managers(id)
                )
            """);
            
            // Clear assignments and tasks
            stmt.executeUpdate("DELETE FROM assignments");
            stmt.executeUpdate("DELETE FROM tasks");
            
            // Verify
            rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
            if (rs.next()) {
                System.out.println("\nProjects after reset: " + rs.getInt(1));
            }
            
            System.out.println("✓ Database forcefully reset!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}