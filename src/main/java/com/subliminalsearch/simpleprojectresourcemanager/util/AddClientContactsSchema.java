package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;

public class AddClientContactsSchema {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        Connection conn = null;
        Statement stmt = null;
        
        try {
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            
            System.out.println("Adding client contact fields to database...");
            
            // Check if columns already exist first
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "projects", "contact_name");
            
            if (!rs.next()) {
                // Add new columns to projects table
                String[] alterStatements = {
                    "ALTER TABLE projects ADD COLUMN contact_name TEXT",
                    "ALTER TABLE projects ADD COLUMN contact_email TEXT",
                    "ALTER TABLE projects ADD COLUMN contact_phone TEXT",
                    "ALTER TABLE projects ADD COLUMN contact_company TEXT",
                    "ALTER TABLE projects ADD COLUMN contact_role TEXT",
                    "ALTER TABLE projects ADD COLUMN send_reports INTEGER DEFAULT 1",
                    "ALTER TABLE projects ADD COLUMN report_frequency TEXT DEFAULT 'WEEKLY'",
                    "ALTER TABLE projects ADD COLUMN last_report_sent TEXT"
                };
                
                for (String sql : alterStatements) {
                    try {
                        stmt.executeUpdate(sql);
                        System.out.println("✓ " + sql.substring(sql.lastIndexOf("COLUMN") + 7));
                    } catch (SQLException e) {
                        if (!e.getMessage().contains("duplicate column")) {
                            System.err.println("Failed: " + sql + " - " + e.getMessage());
                        }
                    }
                }
                
                // Create client communications table
                String createTableSql = """
                    CREATE TABLE IF NOT EXISTS client_communications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        project_id TEXT NOT NULL,
                        communication_type TEXT NOT NULL,
                        subject TEXT,
                        content TEXT,
                        sent_date TEXT NOT NULL,
                        sent_to TEXT,
                        sent_by TEXT,
                        status TEXT,
                        FOREIGN KEY (project_id) REFERENCES projects(project_id)
                    )
                """;
                stmt.executeUpdate(createTableSql);
                System.out.println("✓ Created client_communications table");
                
                // Create indexes
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_communications_project ON client_communications(project_id)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_communications_date ON client_communications(sent_date)");
                System.out.println("✓ Created indexes");
                
                // Add sample client data to a few existing projects
                String updateSampleSql = """
                    UPDATE projects 
                    SET contact_name = 'John Smith',
                        contact_email = 'john.smith@example.com;manager@example.com',
                        contact_phone = '555-0123',
                        contact_company = 'Smith Enterprises',
                        contact_role = 'Project Manager'
                    WHERE project_id IN (
                        SELECT project_id FROM projects 
                        WHERE status = 'ACTIVE' 
                        LIMIT 3
                    )
                """;
                int updated = stmt.executeUpdate(updateSampleSql);
                System.out.println("✓ Added sample contact data to " + updated + " active projects");
                
                System.out.println("\nDatabase schema updated successfully!");
                
            } else {
                System.out.println("Client contact fields already exist in the database.");
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