import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LoadDoghouseData {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Loading Doghouse Data ===");
        
        // Load SQLite driver
        Class.forName("org.sqlite.JDBC");
        
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        
        // Read and clean SQL
        String sql = Files.readString(Paths.get("doghouse_data_generator.sql"));
        String[] lines = sql.split("\n");
        StringBuilder cleanSql = new StringBuilder();
        for (String line : lines) {
            if (!line.trim().startsWith("--") && !line.trim().isEmpty()) {
                cleanSql.append(line).append("\n");
            }
        }
        
        // Execute statements
        Statement stmt = conn.createStatement();
        String[] statements = cleanSql.toString().split(";");
        int count = 0;
        
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                try {
                    stmt.executeUpdate(trimmed);
                    count++;
                    System.out.print(".");
                } catch (SQLException e) {
                    if (!e.getMessage().contains("UNIQUE constraint")) {
                        System.err.println("\nError: " + e.getMessage());
                    }
                }
            }
        }
        
        System.out.println("\n✓ Loaded " + count + " statements");
        
        // Verify
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM resources WHERE id BETWEEN 51 AND 60");
        if (rs.next()) {
            System.out.println("New Technicians: " + rs.getInt(1));
        }
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM projects WHERE project_id LIKE 'DH-%'");
        if (rs.next()) {
            System.out.println("Doghouse Projects: " + rs.getInt(1));
        }
        
        conn.close();
        System.out.println("✓ Doghouse data loaded successfully!");
    }
}