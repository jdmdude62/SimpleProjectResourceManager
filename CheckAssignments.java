import java.sql.*;

public class CheckAssignments {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== SAMPLE ASSIGNMENTS ===\n");
            ResultSet rs = stmt.executeQuery("SELECT * FROM assignments LIMIT 5");
            ResultSetMetaData rsmd = rs.getMetaData();
            
            // Print column info
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                System.out.println("Column " + i + ": " + rsmd.getColumnName(i) + " (" + rsmd.getColumnTypeName(i) + ")");
            }
            
            System.out.println("\nData:");
            while (rs.next()) {
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    System.out.println("  " + rsmd.getColumnName(i) + ": " + rs.getString(i));
                }
                System.out.println("---");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}