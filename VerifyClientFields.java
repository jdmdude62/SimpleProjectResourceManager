import java.sql.*;
import java.nio.file.Paths;

public class VerifyClientFields {
    public static void main(String[] args) {
        String dbPath = Paths.get(System.getProperty("user.home"), 
            ".SimpleProjectResourceManager", "scheduler.db").toString();
        String url = "jdbc:sqlite:" + dbPath;
        
        System.out.println("Checking database at: " + dbPath);
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // Check if columns exist
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "projects", null);
            
            System.out.println("\nColumns in projects table:");
            boolean hasClientProjectId = false;
            boolean hasClientProjectDesc = false;
            
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME");
                System.out.println("  - " + colName);
                if ("client_project_id".equals(colName)) hasClientProjectId = true;
                if ("client_project_description".equals(colName)) hasClientProjectDesc = true;
            }
            
            System.out.println("\nColumn check results:");
            System.out.println("  client_project_id exists: " + hasClientProjectId);
            System.out.println("  client_project_description exists: " + hasClientProjectDesc);
            
            // If columns don't exist, add them
            if (!hasClientProjectId) {
                System.out.println("\nAdding client_project_id column...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE projects ADD COLUMN client_project_id TEXT");
                    System.out.println("Successfully added client_project_id column");
                }
            }
            
            if (!hasClientProjectDesc) {
                System.out.println("\nAdding client_project_description column...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE projects ADD COLUMN client_project_description TEXT");
                    System.out.println("Successfully added client_project_description column");
                }
            }
            
            // Test with a sample project
            System.out.println("\nTesting with AMAZON project (ID 3470):");
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, project_id, client_project_id, client_project_description FROM projects WHERE id = 3470")) {
                ResultSet testRs = ps.executeQuery();
                if (testRs.next()) {
                    System.out.println("  Database ID: " + testRs.getInt("id"));
                    System.out.println("  Project ID: " + testRs.getString("project_id"));
                    System.out.println("  Client Project ID: " + testRs.getString("client_project_id"));
                    System.out.println("  Client Description: " + testRs.getString("client_project_description"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}