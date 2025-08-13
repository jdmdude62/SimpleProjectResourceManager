import java.sql.*;

public class FixTasksTable {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== FIXING TASKS TABLE ===");
            
            // Drop and recreate tasks table with ALL columns the app expects
            stmt.executeUpdate("DROP TABLE IF EXISTS tasks");
            
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
                    parent_task_id INTEGER,
                    phase_id INTEGER,
                    is_milestone BOOLEAN DEFAULT 0,
                    milestone_date DATE,
                    percent_complete INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_by TEXT,
                    updated_by TEXT,
                    FOREIGN KEY (project_id) REFERENCES projects(id),
                    FOREIGN KEY (assigned_to) REFERENCES resources(id),
                    FOREIGN KEY (parent_task_id) REFERENCES tasks(id)
                )
            """);
            
            // Create index for better performance
            stmt.executeUpdate("CREATE INDEX idx_tasks_project_id ON tasks(project_id)");
            stmt.executeUpdate("CREATE INDEX idx_tasks_parent_task_id ON tasks(parent_task_id)");
            
            System.out.println("✓ Tasks table recreated with all columns!");
            
            // Verify structure
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(tasks)");
            System.out.println("\nTasks table columns:");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println("  " + count + ". " + rs.getString("name") + " (" + rs.getString("type") + ")");
            }
            System.out.println("Total columns: " + count);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}