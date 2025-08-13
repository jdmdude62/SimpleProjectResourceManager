import java.sql.*;

public class FixAssignmentDates {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== FIXING ASSIGNMENT DATE FORMATS ===\n");
            
            // Update start_date to include time component
            int updated = stmt.executeUpdate("""
                UPDATE assignments 
                SET start_date = start_date || ' 00:00:00.000'
                WHERE start_date NOT LIKE '%:%'
            """);
            
            System.out.println("✓ Updated " + updated + " start_date values");
            
            // Update end_date to include time component
            updated = stmt.executeUpdate("""
                UPDATE assignments 
                SET end_date = end_date || ' 00:00:00.000'
                WHERE end_date NOT LIKE '%:%'
            """);
            
            System.out.println("✓ Updated " + updated + " end_date values");
            
            // Verify the fix
            ResultSet rs = stmt.executeQuery("SELECT start_date, end_date FROM assignments LIMIT 5");
            System.out.println("\nSample updated dates:");
            while (rs.next()) {
                System.out.println("  Start: " + rs.getString("start_date") + " | End: " + rs.getString("end_date"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}