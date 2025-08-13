import java.sql.*;

public class CheckSchema {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            DatabaseMetaData meta = conn.getMetaData();
            
            System.out.println("=== PROJECTS TABLE SCHEMA ===\n");
            ResultSet columns = meta.getColumns(null, null, "projects", null);
            
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                int nullable = columns.getInt("NULLABLE");
                
                System.out.printf("%-20s %-15s %s\n", 
                    columnName, 
                    columnType, 
                    nullable == DatabaseMetaData.columnNoNulls ? "NOT NULL" : "");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}