import java.sql.*;

public class CheckResourcesSchema {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            DatabaseMetaData meta = conn.getMetaData();
            
            System.out.println("=== RESOURCES TABLE SCHEMA ===\n");
            ResultSet columns = meta.getColumns(null, null, "resources", null);
            
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                int nullable = columns.getInt("NULLABLE");
                
                System.out.printf("%-20s %-15s %s\n", 
                    columnName, 
                    columnType, 
                    nullable == DatabaseMetaData.columnNoNulls ? "NOT NULL" : "");
            }
            
            System.out.println("\n=== SAMPLE RESOURCES ===\n");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM resources LIMIT 5");
            ResultSetMetaData rsmd = rs.getMetaData();
            
            // Print column headers
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                System.out.print(rsmd.getColumnName(i) + "\t");
            }
            System.out.println();
            
            while (rs.next()) {
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}