import java.sql.*;

public class CheckOpenItemsData {
    public static void main(String[] args) {
        String dbPath = "C:\\Users\\mmiller\\.SimpleProjectResourceManager\\scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        System.out.println("Checking open_items data in database: " + dbPath);
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, item_number, title, priority, status, health_status FROM open_items");
            
            System.out.println("\n=== Open Items Data ===");
            while (rs.next()) {
                System.out.println("ID: " + rs.getLong("id"));
                System.out.println("  Item Number: " + rs.getString("item_number"));
                System.out.println("  Title: " + rs.getString("title"));
                System.out.println("  Priority: '" + rs.getString("priority") + "'");
                System.out.println("  Status: '" + rs.getString("status") + "'");
                System.out.println("  Health Status: '" + rs.getString("health_status") + "'");
                System.out.println("---");
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}