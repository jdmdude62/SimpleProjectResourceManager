import java.sql.*;

public class CompleteTasksTable {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== CREATING COMPLETE TASKS TABLE ===");
            
            // Drop and recreate tasks table with ALL columns from TaskRepository
            stmt.executeUpdate("DROP TABLE IF EXISTS tasks");
            
            stmt.executeUpdate("""
                CREATE TABLE tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    phase_id INTEGER,
                    parent_task_id INTEGER,
                    task_code TEXT UNIQUE,
                    title TEXT NOT NULL,
                    description TEXT,
                    task_type TEXT DEFAULT 'GENERAL',
                    priority TEXT DEFAULT 'MEDIUM',
                    status TEXT DEFAULT 'NOT_STARTED',
                    progress_percentage INTEGER DEFAULT 0,
                    planned_start DATE,
                    planned_end DATE,
                    actual_start DATE,
                    actual_end DATE,
                    estimated_hours REAL,
                    actual_hours REAL,
                    assigned_to INTEGER,
                    assigned_by INTEGER,
                    reviewer_id INTEGER,
                    location TEXT,
                    equipment_required TEXT,
                    safety_requirements TEXT,
                    site_access_notes TEXT,
                    risk_level TEXT,
                    risk_notes TEXT,
                    dependencies TEXT,
                    notes TEXT,
                    is_milestone BOOLEAN DEFAULT 0,
                    milestone_date DATE,
                    percent_complete INTEGER DEFAULT 0,
                    completed_at TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_by TEXT,
                    updated_by TEXT,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                    FOREIGN KEY (phase_id) REFERENCES project_phases(id),
                    FOREIGN KEY (parent_task_id) REFERENCES tasks(id),
                    FOREIGN KEY (assigned_to) REFERENCES resources(id),
                    FOREIGN KEY (assigned_by) REFERENCES users(id),
                    FOREIGN KEY (reviewer_id) REFERENCES users(id)
                )
            """);
            
            // Create indexes for better performance
            stmt.executeUpdate("CREATE INDEX idx_tasks_project_id ON tasks(project_id)");
            stmt.executeUpdate("CREATE INDEX idx_tasks_parent_task_id ON tasks(parent_task_id)");
            stmt.executeUpdate("CREATE INDEX idx_tasks_assigned_to ON tasks(assigned_to)");
            stmt.executeUpdate("CREATE INDEX idx_tasks_status ON tasks(status)");
            
            System.out.println("✓ Tasks table created with all columns!");
            
            // Verify structure
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(tasks)");
            System.out.println("\nTasks table columns:");
            int count = 0;
            while (rs.next()) {
                count++;
                String name = rs.getString("name");
                String type = rs.getString("type");
                System.out.printf("  %2d. %-25s %s\n", count, name, type);
            }
            System.out.println("\nTotal columns: " + count);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}