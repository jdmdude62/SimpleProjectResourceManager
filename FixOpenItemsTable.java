import java.sql.*;

public class FixOpenItemsTable {
    public static void main(String[] args) {
        String dbPath = "C:\\Users\\mmiller\\.SimpleProjectResourceManager\\scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        System.out.println("Fixing open_items table in: " + dbPath);
        
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            
            // Drop the existing table
            System.out.println("Dropping existing open_items table...");
            stmt.execute("DROP TABLE IF EXISTS open_items");
            System.out.println("Table dropped successfully");
            
            // Also drop backup if it exists
            stmt.execute("DROP TABLE IF EXISTS open_items_backup");
            
            System.out.println("\nThe open_items table has been removed.");
            System.out.println("It will be recreated with the correct structure when you restart the application.");
            
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}