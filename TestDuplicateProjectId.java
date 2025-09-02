import java.sql.*;

public class TestDuplicateProjectId {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("Connected to database: " + dbPath);
            
            // Check table structure
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='projects'")) {
                if (rs.next()) {
                    String createSql = rs.getString("sql");
                    System.out.println("\nCurrent table definition:");
                    System.out.println(createSql);
                    
                    if (createSql.contains("UNIQUE")) {
                        System.out.println("\nWARNING: UNIQUE constraint still exists!");
                    } else {
                        System.out.println("\nNo UNIQUE constraint found");
                    }
                }
            }
            
            // Test inserting duplicate project_id
            System.out.println("\n--- Testing duplicate project_id insertion ---");
            String insertSql = "INSERT INTO projects (project_id, description, start_date, end_date, status) VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                // First insert
                pstmt.setString(1, "TEST_DUP_" + System.currentTimeMillis());
                pstmt.setString(2, "Test Project 1");
                pstmt.setString(3, "2025-09-01");
                pstmt.setString(4, "2025-09-30");
                pstmt.setString(5, "ACTIVE");
                pstmt.executeUpdate();
                System.out.println("First insert successful");
                
                // Second insert with same project_id
                pstmt.setString(2, "Test Project 2 - Different dates");
                pstmt.setString(3, "2025-10-01");
                pstmt.setString(4, "2025-10-31");
                pstmt.executeUpdate();
                System.out.println("Second insert with same project_id successful!");
                
                // Clean up
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM projects WHERE project_id LIKE 'TEST_DUP_%'");
                }
                
            } catch (SQLException e) {
                System.out.println("Failed to insert duplicate: " + e.getMessage());
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}