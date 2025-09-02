import java.sql.*;

public class CheckOpenItems {
    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found!");
            return;
        }
        
        String dbPath = "C:\\Users\\mmiller\\.SimpleProjectResourceManager\\scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        System.out.println("Checking database: " + dbPath);
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // Check if open_items table exists
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "open_items", null);
            
            if (tables.next()) {
                System.out.println("✓ open_items table EXISTS");
                
                // Count rows
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM open_items");
                if (rs.next()) {
                    System.out.println("  Total rows: " + rs.getInt(1));
                }
                
                // Show first few rows
                rs = stmt.executeQuery("SELECT id, project_id, title, status FROM open_items LIMIT 5");
                while (rs.next()) {
                    System.out.println("  Row: ID=" + rs.getLong("id") + 
                                     ", ProjectID=" + rs.getLong("project_id") +
                                     ", Title=" + rs.getString("title") +
                                     ", Status=" + rs.getString("status"));
                }
            } else {
                System.out.println("✗ open_items table DOES NOT EXIST!");
            }
            
            // Check open_item_categories table
            tables = meta.getTables(null, null, "open_item_categories", null);
            if (tables.next()) {
                System.out.println("✓ open_item_categories table EXISTS");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM open_item_categories");
                if (rs.next()) {
                    System.out.println("  Total categories: " + rs.getInt(1));
                }
            }
            
            // Check open_item_tags table
            tables = meta.getTables(null, null, "open_item_tags", null);
            if (tables.next()) {
                System.out.println("✓ open_item_tags table EXISTS");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM open_item_tags");
                if (rs.next()) {
                    System.out.println("  Total tags: " + rs.getInt(1));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}