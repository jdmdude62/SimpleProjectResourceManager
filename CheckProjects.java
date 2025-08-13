import java.sql.*;

public class CheckProjects {
    public static void main(String[] args) {
        // Load SQLite driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
            return;
        }
        
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== PROJECTS IN DATABASE ===\n");
            
            ResultSet rs = stmt.executeQuery("""
                SELECT id, project_id, description, start_date, end_date, status 
                FROM projects 
                ORDER BY id
            """);
            
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id"));
                System.out.println("Project ID: " + rs.getString("project_id"));
                System.out.println("Description: " + rs.getString("description"));
                System.out.println("Start: " + rs.getString("start_date"));
                System.out.println("End: " + rs.getString("end_date"));
                System.out.println("Status: " + rs.getString("status"));
                
                // Count tasks for this project
                PreparedStatement taskStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM tasks WHERE project_id = ?"
                );
                taskStmt.setInt(1, rs.getInt("id"));
                ResultSet taskRs = taskStmt.executeQuery();
                if (taskRs.next()) {
                    System.out.println("Tasks: " + taskRs.getInt(1));
                }
                
                System.out.println("---");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}