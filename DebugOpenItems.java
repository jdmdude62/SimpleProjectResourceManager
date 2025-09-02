import java.sql.*;

public class DebugOpenItems {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = "C:\\Users\\mmiller\\.SimpleProjectResourceManager\\scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        System.out.println("=== DEBUGGING OPEN ITEMS DATABASE ===\n");
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // Check table structure
            System.out.println("1. Checking open_items table structure:");
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "open_items", null);
            while (columns.next()) {
                System.out.printf("   Column: %s, Type: %s, Nullable: %s\n",
                    columns.getString("COLUMN_NAME"),
                    columns.getString("TYPE_NAME"),
                    columns.getString("IS_NULLABLE"));
            }
            
            // Count total items
            System.out.println("\n2. Total items in open_items table:");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total, COUNT(CASE WHEN is_deleted = 0 THEN 1 END) as active FROM open_items");
            if (rs.next()) {
                System.out.printf("   Total: %d, Active (not deleted): %d\n", 
                    rs.getInt("total"), rs.getInt("active"));
            }
            
            // Show all items with key fields
            System.out.println("\n3. All open items (including deleted):");
            rs = stmt.executeQuery("""
                SELECT id, project_id, item_number, title, priority, status, 
                       health_status, is_deleted, created_at, updated_at
                FROM open_items 
                ORDER BY id DESC 
                LIMIT 20
            """);
            
            while (rs.next()) {
                System.out.printf("   ID: %d, Project: %d, Number: %s, Title: %s\n",
                    rs.getLong("id"),
                    rs.getLong("project_id"),
                    rs.getString("item_number"),
                    rs.getString("title"));
                System.out.printf("      Priority: '%s', Status: '%s', Health: '%s', Deleted: %s\n",
                    rs.getString("priority"),
                    rs.getString("status"),
                    rs.getString("health_status"),
                    rs.getBoolean("is_deleted"));
                System.out.printf("      Created: %s, Updated: %s\n",
                    rs.getString("created_at"),
                    rs.getString("updated_at"));
                System.out.println();
            }
            
            // Check for specific project
            System.out.println("\n4. Items for specific projects:");
            rs = stmt.executeQuery("""
                SELECT project_id, COUNT(*) as count 
                FROM open_items 
                WHERE is_deleted = 0 
                GROUP BY project_id 
                ORDER BY count DESC
            """);
            while (rs.next()) {
                System.out.printf("   Project %d: %d items\n",
                    rs.getLong("project_id"),
                    rs.getInt("count"));
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}