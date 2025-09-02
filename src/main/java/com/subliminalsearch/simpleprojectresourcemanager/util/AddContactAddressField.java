package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;

public class AddContactAddressField {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        Connection conn = null;
        Statement stmt = null;
        
        try {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                // Ignore - driver may be loaded automatically
            }
            
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            
            System.out.println("Adding contact_address field to database...");
            
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "projects", "contact_address");
            
            if (!rs.next()) {
                String sql = "ALTER TABLE projects ADD COLUMN contact_address TEXT";
                stmt.executeUpdate(sql);
                System.out.println("✓ Added contact_address column to projects table");
                
                System.out.println("\nDatabase schema updated successfully!");
            } else {
                System.out.println("contact_address field already exists in the database.");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}