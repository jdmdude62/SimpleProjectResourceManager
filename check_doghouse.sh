#!/bin/bash

echo "=== Checking Doghouse Data in Database ==="
echo

# Run a simple Java program to check the database
cat > QuickCheck.java << 'EOF'
import java.sql.*;

public class QuickCheck {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement stmt = conn.createStatement();
        
        // Check doghouse technicians
        System.out.println("Doghouse Technicians (IDs 51-60):");
        ResultSet rs = stmt.executeQuery("SELECT id, name FROM resources WHERE id BETWEEN 51 AND 60");
        int techCount = 0;
        while (rs.next()) {
            techCount++;
            System.out.println("  " + rs.getInt("id") + ": " + rs.getString("name"));
        }
        System.out.println("Total technicians: " + techCount);
        
        // Check doghouse projects  
        System.out.println("\nDoghouse Projects:");
        rs = stmt.executeQuery("SELECT project_id, description FROM projects WHERE project_id LIKE 'DH-%'");
        int projectCount = 0;
        while (rs.next()) {
            projectCount++;
            String desc = rs.getString("description");
            if (desc.length() > 50) desc = desc.substring(0, 50) + "...";
            System.out.println("  " + rs.getString("project_id") + ": " + desc);
        }
        System.out.println("Total doghouse projects: " + projectCount);
        
        // Check January 2025 data
        System.out.println("\nJanuary 2025 Summary:");
        rs = stmt.executeQuery(
            "SELECT COUNT(*) FROM projects " +
            "WHERE date(start_date) <= date('2025-01-31') " +
            "AND date(end_date) >= date('2025-01-01')"
        );
        if (rs.next()) {
            System.out.println("  Total projects in January: " + rs.getInt(1));
        }
        
        conn.close();
    }
}
EOF

# Compile and run
javac -cp "target/lib/*:." QuickCheck.java
java -cp "target/lib/*:." QuickCheck

# Clean up
rm QuickCheck.java QuickCheck.class