import java.sql.*;

public class CheckHolidays {
    public static void main(String[] args) {
        String dbPath = "C:/Users/mmiller/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found!");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("Connected to database");
            
            // Check if table exists
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "company_holidays", null);
            if (!tables.next()) {
                System.out.println("Table 'company_holidays' does not exist!");
                return;
            }
            
            // Count holidays
            String countSql = "SELECT COUNT(*) FROM company_holidays";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next()) {
                    System.out.println("Total holidays in database: " + rs.getInt(1));
                }
            }
            
            // List holidays
            String sql = "SELECT id, name, date, type, description, active FROM company_holidays ORDER BY date";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                System.out.println("\nHolidays:");
                System.out.println("ID | Name | Date | Type | Description | Active");
                System.out.println("---|------|------|------|-------------|-------");
                
                while (rs.next()) {
                    System.out.printf("%d | %s | %s | %s | %s | %s%n",
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("date"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getBoolean("active"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}