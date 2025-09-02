package com.subliminalsearch.simpleprojectresourcemanager.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    private static final String DB_NAME = "scheduler.db";
    private static final String DEFAULT_DB_PATH = System.getProperty("user.home") + "/.SimpleProjectResourceManager/";
    
    private final HikariDataSource dataSource;
    private final String dbPath;

    public DatabaseConfig() {
        this(DEFAULT_DB_PATH);
    }

    public DatabaseConfig(String customPath) {
        this.dbPath = customPath;
        this.dataSource = initializeDataSource();
        initializeDatabase();
    }

    private HikariDataSource initializeDataSource() {
        try {
            Path dbDir = Paths.get(dbPath);
            if (!Files.exists(dbDir)) {
                Files.createDirectories(dbDir);
                logger.info("Created database directory: {}", dbDir);
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbPath + DB_NAME);
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setConnectionTestQuery("SELECT 1");

            logger.info("Initialized SQLite database at: {}", dbPath + DB_NAME);
            return new HikariDataSource(config);
            
        } catch (IOException e) {
            logger.error("Failed to create database directory", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            if (isDatabaseEmpty(conn)) {
                logger.info("Database is empty, running initial migration...");
                runMigration(conn, "001_initial_schema.sql");
                logger.info("Initial migration completed successfully");
            } else {
                logger.info("Database already initialized");
            }
            
            // Always check for doghouse data
            loadDoghouseDataIfMissing(conn);
            
            // Check for and run task management migration
            if (!tableExists(conn, "tasks")) {
                logger.info("Tasks table not found, running task management migration...");
                runMigration(conn, "002_task_management.sql");
                logger.info("Task management migration completed successfully");
            }
            
            // Check for and add contact_address column if missing
            if (!columnExists(conn, "projects", "contact_address")) {
                logger.info("Adding contact_address column to projects table...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE projects ADD COLUMN contact_address TEXT");
                    logger.info("Successfully added contact_address column");
                } catch (SQLException e) {
                    logger.warn("Could not add contact_address column: " + e.getMessage());
                }
            }
            
            // Check for and add location column to assignments if missing
            if (!columnExists(conn, "assignments", "location")) {
                logger.info("Adding location column to assignments table for multi-location support...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE assignments ADD COLUMN location VARCHAR(255)");
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_assignments_location ON assignments(location)");
                    logger.info("Successfully added location column to assignments");
                } catch (SQLException e) {
                    logger.warn("Could not add location column to assignments: " + e.getMessage());
                }
            }
            
            // Check for and add is_travel column to projects if missing
            if (!columnExists(conn, "projects", "is_travel")) {
                logger.info("Adding is_travel column to projects table...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE projects ADD COLUMN is_travel BOOLEAN DEFAULT 0");
                    logger.info("Successfully added is_travel column");
                } catch (SQLException e) {
                    logger.warn("Could not add is_travel column: " + e.getMessage());
                }
            }
            
            // Check for and add client_project_id column to projects if missing
            if (!columnExists(conn, "projects", "client_project_id")) {
                logger.info("Adding client_project_id column to projects table...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE projects ADD COLUMN client_project_id TEXT");
                    logger.info("Successfully added client_project_id column");
                } catch (SQLException e) {
                    logger.warn("Could not add client_project_id column: " + e.getMessage());
                }
            }
            
            // Check for and add client_project_description column to projects if missing
            if (!columnExists(conn, "projects", "client_project_description")) {
                logger.info("Adding client_project_description column to projects table...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE projects ADD COLUMN client_project_description TEXT");
                    logger.info("Successfully added client_project_description column");
                } catch (SQLException e) {
                    logger.warn("Could not add client_project_description column: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void loadDoghouseDataIfMissing(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Check if doghouse technicians exist
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM resources WHERE id = 51");
            boolean hasDoghouseData = false;
            if (rs.next() && rs.getInt(1) > 0) {
                hasDoghouseData = true;
            }
            
            if (!hasDoghouseData) {
                logger.info("Loading doghouse sample data...");
                
                // Add 10 field technicians (matching actual schema)
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(51, 'Tech Team Alpha - Jake Morrison', 'jake.morrison@company.com', '555-0151', 1, 1)");
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(52, 'Tech Team Alpha - Maria Santos', 'maria.santos@company.com', '555-0152', 1, 1)");
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(53, 'Tech Team Alpha - David Chen', 'david.chen@company.com', '555-0153', 1, 1)");
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(54, 'Tech Team Alpha - Sarah Johnson', 'sarah.johnson@company.com', '555-0154', 1, 1)");
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(55, 'Tech Team Alpha - Mike Williams', 'mike.williams@company.com', '555-0155', 1, 1)");
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(56, 'Tech Team Bravo - Tom Anderson', 'tom.anderson@company.com', '555-0156', 1, 1)");
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(57, 'Tech Team Bravo - Lisa Park', 'lisa.park@company.com', '555-0157', 1, 1)");
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(58, 'Tech Team Bravo - James Wilson', 'james.wilson@company.com', '555-0158', 1, 1)");
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(59, 'Tech Team Bravo - Emily Davis', 'emily.davis@company.com', '555-0159', 1, 1)");
                stmt.executeUpdate("INSERT OR IGNORE INTO resources (id, name, email, phone, resource_type_id, is_active) VALUES " +
                    "(60, 'Tech Team Bravo - Chris Martinez', 'chris.martinez@company.com', '555-0160', 1, 1)");
                
                // Add comprehensive January doghouse projects
                // Week 1: Team Alpha builds, Team Bravo installs
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(101, 'DH-2025-001-BUILD', 'Doghouse Build - Batch 001 (5 units) - Workshop', 2, '2025-01-06 08:00:00.000', '2025-01-09 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'First batch of temperature-controlled doghouses')");
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(102, 'DH-2025-001-INSTALL', 'Doghouse Install - Johnson @ Oak Street', 3, '2025-01-06 08:00:00.000', '2025-01-08 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model')");
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(103, 'DH-2025-002-INSTALL', 'Doghouse Install - Smith @ Maple Ave', 3, '2025-01-09 08:00:00.000', '2025-01-11 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, 'Standard model with heating')");
                
                // Week 2: Team Bravo builds, Team Alpha installs
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(104, 'DH-2025-002-BUILD', 'Doghouse Build - Batch 002 (5 units) - Workshop', 2, '2025-01-13 08:00:00.000', '2025-01-16 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'Second batch for January deliveries')");
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(105, 'DH-2025-003-INSTALL', 'Doghouse Install - Davis @ Pine Road', 3, '2025-01-13 08:00:00.000', '2025-01-15 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC')");
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(106, 'DH-2025-004-INSTALL', 'Doghouse Install - Wilson @ Elm Drive', 3, '2025-01-16 08:00:00.000', '2025-01-18 17:00:00.000', 'PLANNED', 4500.00, 'MEDIUM', 0, 'Standard model')");
                
                // Week 3: Team Alpha builds, Team Bravo installs
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(107, 'DH-2025-003-BUILD', 'Doghouse Build - Batch 003 (6 units) - Workshop', 2, '2025-01-20 08:00:00.000', '2025-01-23 17:00:00.000', 'PLANNED', 15000.00, 'HIGH', 0, 'Increased production batch')");
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(108, 'DH-2025-005-INSTALL', 'Doghouse Install - Martinez @ Cedar Blvd', 3, '2025-01-20 08:00:00.000', '2025-01-22 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, 'Premium model with smart features')");
                
                // Week 4: Team Bravo builds, Team Alpha installs
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(109, 'DH-2025-004-BUILD', 'Doghouse Build - Batch 004 (5 units) - Workshop', 2, '2025-01-27 08:00:00.000', '2025-01-30 17:00:00.000', 'PLANNED', 12500.00, 'MEDIUM', 0, 'End of month production')");
                stmt.executeUpdate("INSERT OR IGNORE INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES " +
                    "(110, 'DH-2025-006-INSTALL', 'Doghouse Install - Thompson @ Birch Lane', 3, '2025-01-27 08:00:00.000', '2025-01-29 17:00:00.000', 'PLANNED', 4500.00, 'MEDIUM', 0, 'Standard deluxe model')");
                
                // Add comprehensive assignments for all projects
                // Week 1 assignments
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(101, 51, '2025-01-06', '2025-01-09', 0, 0, 0, 'Team lead - workshop', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(101, 52, '2025-01-06', '2025-01-09', 0, 0, 0, 'Build specialist', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(101, 53, '2025-01-06', '2025-01-09', 0, 0, 0, 'Assembly technician', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(101, 54, '2025-01-06', '2025-01-09', 0, 0, 0, 'Quality control', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(101, 55, '2025-01-06', '2025-01-09', 0, 0, 0, 'HVAC specialist', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(102, 56, '2025-01-06', '2025-01-08', 1, 0, 0, 'Installation lead', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(102, 57, '2025-01-06', '2025-01-08', 1, 0, 0, 'Foundation specialist', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(102, 58, '2025-01-06', '2025-01-08', 1, 0, 0, 'Assembly technician', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(103, 59, '2025-01-09', '2025-01-11', 1, 0, 0, 'Installation technician', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                stmt.executeUpdate("INSERT OR IGNORE INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES " +
                    "(103, 60, '2025-01-09', '2025-01-11', 1, 0, 0, 'HVAC installation', '2025-01-01 00:00:00', '2025-01-01 00:00:00')");
                
                logger.info("✓ Loaded doghouse sample data successfully");
            }
        } catch (SQLException e) {
            logger.warn("Could not load doghouse data: " + e.getMessage());
        }
    }
    
    private boolean isDatabaseEmpty(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'");
            return rs.next() && rs.getInt(1) == 0;
        }
    }
    
    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='" + tableName + "'");
            return rs.next() && rs.getInt(1) > 0;
        }
    }
    
    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private void runMigration(Connection conn, String migrationFile) throws SQLException {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("/db/migrations/" + migrationFile))) {
            // Read the entire file content
            scanner.useDelimiter("\\Z");
            String fullContent = scanner.next();
            
            // Split on semicolon but preserve the order
            String[] statements = fullContent.split(";");
            
            for (String statement : statements) {
                // Clean up the statement
                String[] lines = statement.split("\n");
                StringBuilder cleanSql = new StringBuilder();
                
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    // Skip comment lines but preserve non-comment content
                    if (!trimmedLine.startsWith("--") && !trimmedLine.isEmpty()) {
                        cleanSql.append(line).append("\n");
                    }
                }
                
                String finalSql = cleanSql.toString().trim();
                if (!finalSql.isEmpty()) {
                    try (Statement stmt = conn.createStatement()) {
                        logger.debug("Executing migration SQL: {}", 
                            finalSql.length() > 100 ? finalSql.substring(0, 100) + "..." : finalSql);
                        stmt.execute(finalSql);
                    }
                }
            }
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public String getDatabasePath() {
        return dbPath + DB_NAME;
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool shutdown completed");
        }
    }
}