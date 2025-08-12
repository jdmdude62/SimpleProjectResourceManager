import java.sql.*;

public class ListTables {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});
            
            System.out.println("=== TABLES IN DATABASE ===\n");
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (!tableName.startsWith("sqlite_")) {
                    System.out.println("- " + tableName);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}