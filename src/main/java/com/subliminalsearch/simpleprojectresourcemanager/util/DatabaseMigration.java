package com.subliminalsearch.simpleprojectresourcemanager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import java.sql.*;

/**
 * Utility to apply database migrations
 */
public class DatabaseMigration {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);
    
    /**
     * Remove UNIQUE constraint from project_id column
     */
    public static void removeProjectIdUniqueConstraint(DataSource dataSource) {
        logger.info("Starting migration to remove UNIQUE constraint from project_id...");
        
        try (Connection conn = dataSource.getConnection()) {
            // Begin transaction
            conn.setAutoCommit(false);
            
            try (Statement stmt = conn.createStatement()) {
                // Check if migration is needed by testing if we can insert duplicate project_ids
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='projects'")) {
                    if (rs.next()) {
                        String createStatement = rs.getString("sql");
                        if (createStatement.contains("project_id VARCHAR(50) UNIQUE") || 
                            createStatement.contains("project_id TEXT UNIQUE")) {
                            logger.info("UNIQUE constraint found on project_id, proceeding with migration...");
                            
                            // Get all column information from existing table
                            ResultSet columns = conn.getMetaData().getColumns(null, null, "projects", null);
                            StringBuilder columnDefs = new StringBuilder();
                            while (columns.next()) {
                                if (columnDefs.length() > 0) columnDefs.append(", ");
                                columnDefs.append(columns.getString("COLUMN_NAME"));
                            }
                            String columnList = columnDefs.toString();
                            
                            // Step 1: Create new table without UNIQUE constraint
                            stmt.execute("""
                                CREATE TABLE projects_new (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    project_id VARCHAR(50) NOT NULL,
                                    description TEXT,
                                    project_manager_id INTEGER,
                                    start_date DATE,
                                    end_date DATE,
                                    status VARCHAR(20) DEFAULT 'ACTIVE',
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    contact_name VARCHAR(100),
                                    contact_email VARCHAR(255),
                                    contact_phone VARCHAR(20),
                                    contact_company VARCHAR(100),
                                    contact_role VARCHAR(50),
                                    contact_address TEXT,
                                    send_reports BOOLEAN DEFAULT 1,
                                    report_frequency VARCHAR(20) DEFAULT 'WEEKLY',
                                    last_report_sent TIMESTAMP,
                                    budget_amount DECIMAL(15,2),
                                    actual_cost DECIMAL(15,2),
                                    revenue_amount DECIMAL(15,2),
                                    currency_code VARCHAR(3) DEFAULT 'USD',
                                    labor_cost DECIMAL(15,2),
                                    material_cost DECIMAL(15,2),
                                    travel_cost DECIMAL(15,2),
                                    other_cost DECIMAL(15,2),
                                    cost_notes TEXT,
                                    FOREIGN KEY (project_manager_id) REFERENCES project_managers(id)
                                )
                            """);
                            
                            // Step 2: Copy data
                            stmt.execute("INSERT INTO projects_new SELECT * FROM projects");
                            
                            // Step 3: Drop old table
                            stmt.execute("DROP TABLE projects");
                            
                            // Step 4: Rename new table
                            stmt.execute("ALTER TABLE projects_new RENAME TO projects");
                            
                            // Step 5: Create indexes
                            stmt.execute("CREATE INDEX idx_projects_project_id ON projects(project_id)");
                            stmt.execute("CREATE INDEX idx_projects_status ON projects(status)");
                            stmt.execute("CREATE INDEX idx_projects_dates ON projects(start_date, end_date)");
                            stmt.execute("CREATE INDEX idx_projects_manager ON projects(project_manager_id)");
                            
                            conn.commit();
                            logger.info("Migration completed successfully - UNIQUE constraint removed from project_id");
                        } else {
                            logger.info("No UNIQUE constraint found on project_id, skipping migration");
                        }
                    }
                } catch (SQLException e) {
                    // Check if it's because the column doesn't exist or table structure is different
                    logger.warn("Could not check table structure: " + e.getMessage());
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Migration failed", e);
            throw new RuntimeException("Failed to remove UNIQUE constraint from project_id", e);
        }
    }
    
    /**
     * Check if project_id has UNIQUE constraint
     */
    public static boolean hasProjectIdUniqueConstraint(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT sql FROM sqlite_master WHERE type='table' AND name='projects'")) {
            
            if (rs.next()) {
                String createStatement = rs.getString("sql");
                return createStatement != null && 
                       (createStatement.contains("project_id VARCHAR(50) UNIQUE") || 
                        createStatement.contains("project_id TEXT UNIQUE"));
            }
        } catch (SQLException e) {
            logger.error("Error checking for UNIQUE constraint", e);
        }
        return false;
    }
}