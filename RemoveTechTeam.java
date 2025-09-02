import java.sql.*;

public class RemoveTechTeam {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = "jdbc:sqlite:C:\\Users\\mmiller\\.SimpleProjectResourceManager\\scheduler.db";
        
        try (Connection conn = DriverManager.getConnection(dbPath)) {
            // List Tech Team resources
            String selectSql = "SELECT id, name, active FROM resources WHERE name LIKE 'Tech Team%' ORDER BY name";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql)) {
                
                System.out.println("Tech Team resources found:");
                int count = 0;
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String name = rs.getString("name");
                    boolean active = rs.getBoolean("active");
                    System.out.printf("ID: %d, Active: %s, Name: %s%n", id, active ? "Yes" : "No", name);
                    count++;
                }
                
                if (count == 0) {
                    System.out.println("No Tech Team resources found.");
                    return;
                }
                
                System.out.println("\nTotal found: " + count);
            }
            
            // Delete them
            String deleteSql = "DELETE FROM resources WHERE name LIKE 'Tech Team%'";
            try (Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate(deleteSql);
                System.out.printf("Deleted %d Tech Team resources.%n", deleted);
            }
            
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}