import java.sql.*;

public class FixProjectStatus {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== FIXING PROJECT STATUS VALUES ===\n");
            
            // Update IN_PROGRESS to ACTIVE
            int updated = stmt.executeUpdate(
                "UPDATE projects SET status = 'ACTIVE' WHERE status = 'IN_PROGRESS'"
            );
            
            System.out.println("✓ Updated " + updated + " projects from IN_PROGRESS to ACTIVE");
            
            // Show status distribution
            ResultSet rs = stmt.executeQuery(
                "SELECT status, COUNT(*) as count FROM projects GROUP BY status ORDER BY count DESC"
            );
            
            System.out.println("\nCurrent status distribution:");
            while (rs.next()) {
                System.out.printf("  %-15s: %d projects\n", 
                    rs.getString("status"), 
                    rs.getInt("count"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}