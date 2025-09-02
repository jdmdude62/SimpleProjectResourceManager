import java.sql.*;

public class DebugProjectItems {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = "C:\\Users\\mmiller\\.SimpleProjectResourceManager\\scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        System.out.println("=== DEBUGGING PROJECT 3472 OPEN ITEMS ===\n");
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            // Show ALL items for project 3472 (including deleted)
            System.out.println("ALL items for project 3472:");
            ResultSet rs = stmt.executeQuery("""
                SELECT id, item_number, title, is_deleted, created_at, updated_at
                FROM open_items 
                WHERE project_id = 3472
                ORDER BY id DESC
            """);
            
            int total = 0;
            int deleted = 0;
            int active = 0;
            
            while (rs.next()) {
                total++;
                boolean isDeleted = rs.getBoolean("is_deleted");
                if (isDeleted) deleted++;
                else active++;
                
                System.out.printf("  ID: %d, Number: %s, Title: %s, Deleted: %s\n",
                    rs.getLong("id"),
                    rs.getString("item_number"),
                    rs.getString("title"),
                    isDeleted ? "YES" : "NO");
                System.out.printf("    Created: %s, Updated: %s\n",
                    rs.getString("created_at"),
                    rs.getString("updated_at"));
            }
            
            System.out.printf("\nSummary: Total: %d, Active: %d, Deleted: %d\n", total, active, deleted);
            
            // Check the actual is_deleted values
            System.out.println("\n=== Checking is_deleted field values ===");
            rs = stmt.executeQuery("""
                SELECT is_deleted, COUNT(*) as count
                FROM open_items
                WHERE project_id = 3472
                GROUP BY is_deleted
            """);
            
            while (rs.next()) {
                System.out.printf("  is_deleted = %s: %d items\n",
                    rs.getString("is_deleted"),
                    rs.getInt("count"));
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}