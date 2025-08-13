import java.sql.*;

public class CompleteDBReset {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== COMPLETE DATABASE RESET ===");
            
            // Drop all project-related tables and recreate them properly
            stmt.executeUpdate("DROP TABLE IF EXISTS tasks");
            stmt.executeUpdate("DROP TABLE IF EXISTS assignments");  
            stmt.executeUpdate("DROP TABLE IF EXISTS projects");
            
            // Recreate projects table with proper date columns
            stmt.executeUpdate("""
                CREATE TABLE projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id TEXT NOT NULL UNIQUE,
                    description TEXT,
                    project_manager_id INTEGER,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    status TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_by TEXT,
                    updated_by TEXT,
                    FOREIGN KEY (project_manager_id) REFERENCES project_managers(id)
                )
            """);
            
            // Recreate assignments table
            stmt.executeUpdate("""
                CREATE TABLE assignments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    resource_id INTEGER NOT NULL,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    travel_out_days INTEGER DEFAULT 0,
                    travel_back_days INTEGER DEFAULT 0,
                    is_override BOOLEAN DEFAULT 0,
                    override_reason TEXT,
                    notes TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id),
                    FOREIGN KEY (resource_id) REFERENCES resources(id)
                )
            """);
            
            // Recreate tasks table
            stmt.executeUpdate("""
                CREATE TABLE tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    task_code TEXT UNIQUE,
                    title TEXT NOT NULL,
                    description TEXT,
                    priority TEXT DEFAULT 'MEDIUM',
                    status TEXT DEFAULT 'NOT_STARTED',
                    assigned_to INTEGER,
                    estimated_hours REAL,
                    actual_hours REAL,
                    planned_start DATE,
                    planned_end DATE,
                    actual_start DATE,
                    actual_end DATE,
                    completed_at TIMESTAMP,
                    dependencies TEXT,
                    notes TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_by TEXT,
                    updated_by TEXT,
                    FOREIGN KEY (project_id) REFERENCES projects(id),
                    FOREIGN KEY (assigned_to) REFERENCES resources(id)
                )
            """);
            
            System.out.println("✓ All tables recreated successfully!");
            
            // Verify
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
            if (rs.next()) {
                System.out.println("Projects: " + rs.getInt(1));
            }
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM assignments");
            if (rs.next()) {
                System.out.println("Assignments: " + rs.getInt(1));
            }
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM tasks");
            if (rs.next()) {
                System.out.println("Tasks: " + rs.getInt(1));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}