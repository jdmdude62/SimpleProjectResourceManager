package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;

public class AddLdapColumnUtil {
    public static void main(String[] args) {
        try {
            DatabaseConfig dbConfig = new DatabaseConfig();
            DataSource dataSource = dbConfig.getDataSource();
            
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Try to add the column
                try {
                    stmt.execute("ALTER TABLE resources ADD COLUMN ldap_username TEXT");
                    System.out.println("Successfully added ldap_username column to resources table");
                } catch (Exception e) {
                    if (e.getMessage().contains("duplicate column") || e.getMessage().contains("already exists")) {
                        System.out.println("ldap_username column already exists");
                    } else {
                        System.out.println("Note: " + e.getMessage());
                    }
                }
                
                // Verify the column exists
                stmt.execute("SELECT ldap_username FROM resources LIMIT 1");
                System.out.println("Verified: ldap_username column is available");
                
            }
            
            System.out.println("\nPlease restart the application for changes to take effect.");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}