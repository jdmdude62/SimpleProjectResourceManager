package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;

/**
 * Standalone utility to run the database migration
 */
public class RunDatabaseMigration {
    private static final Logger logger = LoggerFactory.getLogger(RunDatabaseMigration.class);
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("Database Migration Utility");
        System.out.println("===========================================");
        
        // Setup database connection
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        
        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            System.out.println("Connected to database: " + dbPath);
            
            // Check current constraint status
            boolean hasConstraint = DatabaseMigration.hasProjectIdUniqueConstraint(dataSource);
            System.out.println("Current status: UNIQUE constraint on project_id = " + hasConstraint);
            
            // Force migration regardless
            System.out.println("\nForcing migration to ensure no UNIQUE constraint...");
            if (true) {
                System.out.println("\nRemoving UNIQUE constraint...");
                
                // First, let's do it directly here to ensure it works
                try (Connection conn = dataSource.getConnection()) {
                    conn.setAutoCommit(false);
                    
                    try (Statement stmt = conn.createStatement()) {
                        // Check current table structure
                        ResultSet rs = stmt.executeQuery(
                            "SELECT sql FROM sqlite_master WHERE type='table' AND name='projects'");
                        if (rs.next()) {
                            String createSql = rs.getString("sql");
                            System.out.println("Current table definition:\n" + createSql);
                        }
                        
                        System.out.println("\nCreating new table without UNIQUE constraint...");
                        
                        // Create new table - matching ALL columns from existing table
                        stmt.execute("""
                            CREATE TABLE IF NOT EXISTS projects_new (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                project_id TEXT NOT NULL,
                                description TEXT,
                                project_manager_id INTEGER,
                                start_date DATE NOT NULL,
                                end_date DATE NOT NULL,
                                status TEXT NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                created_by TEXT,
                                updated_by TEXT,
                                contact_name TEXT,
                                contact_email TEXT,
                                contact_phone TEXT,
                                contact_company TEXT,
                                contact_role TEXT,
                                send_reports INTEGER DEFAULT 1,
                                report_frequency TEXT DEFAULT 'WEEKLY',
                                last_report_sent TEXT,
                                budget_amount REAL,
                                actual_cost REAL,
                                revenue_amount REAL,
                                currency_code TEXT DEFAULT 'USD',
                                labor_cost REAL,
                                material_cost REAL,
                                travel_cost REAL,
                                other_cost REAL,
                                cost_notes TEXT,
                                contact_address TEXT,
                                FOREIGN KEY (project_manager_id) REFERENCES project_managers(id)
                            )
                        """);
                        
                        System.out.println("Copying data from old table...");
                        int rowsCopied = stmt.executeUpdate(
                            "INSERT INTO projects_new SELECT * FROM projects");
                        System.out.println("Copied " + rowsCopied + " projects");
                        
                        System.out.println("Dropping old table...");
                        stmt.execute("DROP TABLE projects");
                        
                        System.out.println("Renaming new table...");
                        stmt.execute("ALTER TABLE projects_new RENAME TO projects");
                        
                        System.out.println("Creating indexes...");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_projects_project_id ON projects(project_id)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_projects_dates ON projects(start_date, end_date)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_projects_manager ON projects(project_manager_id)");
                        
                        conn.commit();
                        System.out.println("\n✓ Migration completed successfully!");
                        
                        // Verify the change
                        rs = stmt.executeQuery(
                            "SELECT sql FROM sqlite_master WHERE type='table' AND name='projects'");
                        if (rs.next()) {
                            String newCreateSql = rs.getString("sql");
                            System.out.println("\nNew table definition:\n" + newCreateSql);
                        }
                        
                        // Test by trying to insert duplicate project_id
                        System.out.println("\nTesting duplicate project_id insertion...");
                        try {
                            stmt.execute("INSERT INTO projects (project_id, description, start_date, end_date) " +
                                       "VALUES ('TEST_DUP', 'Test 1', '2025-01-01', '2025-01-31')");
                            stmt.execute("INSERT INTO projects (project_id, description, start_date, end_date) " +
                                       "VALUES ('TEST_DUP', 'Test 2', '2025-02-01', '2025-02-28')");
                            System.out.println("✓ Successfully inserted duplicate project_id!");
                            
                            // Clean up test data
                            stmt.execute("DELETE FROM projects WHERE project_id = 'TEST_DUP'");
                        } catch (SQLException e) {
                            System.err.println("✗ Failed to insert duplicate: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        conn.rollback();
                        throw e;
                    }
                }
            } else {
                System.out.println("No UNIQUE constraint found - migration not needed.");
            }
            
            System.out.println("\n===========================================");
            System.out.println("Migration check complete!");
            System.out.println("You can now create multiple projects with the same Project ID.");
            System.out.println("===========================================");
            
        } catch (Exception e) {
            System.err.println("ERROR: Migration failed!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}