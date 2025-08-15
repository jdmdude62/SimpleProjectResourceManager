import java.sql.*;

public class CheckProjectManagers {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // Check project_managers table
            System.out.println("Project Managers:");
            System.out.println("-----------------");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM project_managers")) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getLong("id") + ", Name: " + rs.getString("name") + 
                                     ", Email: " + rs.getString("email"));
                }
            }
            
            System.out.println("\nProjects with their PM IDs:");
            System.out.println("---------------------------");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT project_id, description, project_manager_id FROM projects WHERE project_manager_id IS NOT NULL")) {
                while (rs.next()) {
                    System.out.println("Project: " + rs.getString("project_id") + " - " + 
                                     rs.getString("description") + 
                                     ", PM ID: " + rs.getLong("project_manager_id"));
                }
            }
            
            // Check for any invalid PM IDs
            System.out.println("\nProjects with invalid PM IDs:");
            System.out.println("-----------------------------");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT p.project_id, p.description, p.project_manager_id " +
                     "FROM projects p " +
                     "WHERE p.project_manager_id IS NOT NULL " +
                     "AND p.project_manager_id NOT IN (SELECT id FROM project_managers)")) {
                while (rs.next()) {
                    System.out.println("Project: " + rs.getString("project_id") + " - " + 
                                     rs.getString("description") + 
                                     ", Invalid PM ID: " + rs.getLong("project_manager_id"));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}