import java.sql.*;

public class FixProjectsTable {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== FIXING PROJECTS TABLE ===");
            
            // Get the schema from assignments table as reference
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(assignments)");
            System.out.println("\nAssignments table columns (for reference):");
            while (rs.next()) {
                System.out.println("  - " + rs.getString("name") + " (" + rs.getString("type") + ")");
            }
            
            // Drop and recreate with all necessary columns
            stmt.executeUpdate("DROP TABLE IF EXISTS projects");
            
            stmt.executeUpdate("""
                CREATE TABLE projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id TEXT NOT NULL UNIQUE,
                    description TEXT,
                    project_manager_id INTEGER,
                    start_date TEXT NOT NULL,
                    end_date TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    created_by TEXT,
                    updated_by TEXT,
                    FOREIGN KEY (project_manager_id) REFERENCES project_managers(id)
                )
            """);
            
            System.out.println("\n✓ Projects table recreated with all columns!");
            
            // Verify structure
            rs = stmt.executeQuery("PRAGMA table_info(projects)");
            System.out.println("\nNew projects table columns:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString("name") + " (" + rs.getString("type") + ")");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}