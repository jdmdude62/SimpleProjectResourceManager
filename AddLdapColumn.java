import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class AddLdapColumn {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found!");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            
            // Try to add the column - will fail silently if it already exists
            try {
                stmt.execute("ALTER TABLE resources ADD COLUMN ldap_username TEXT");
                System.out.println("Successfully added ldap_username column to resources table");
            } catch (Exception e) {
                if (e.getMessage().contains("duplicate column")) {
                    System.out.println("ldap_username column already exists");
                } else {
                    System.out.println("Note: " + e.getMessage());
                }
            }
            
            // Verify the column exists
            stmt.execute("SELECT ldap_username FROM resources LIMIT 1");
            System.out.println("Verified: ldap_username column is available");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}