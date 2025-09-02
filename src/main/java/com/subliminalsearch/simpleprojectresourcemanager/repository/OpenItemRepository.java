package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.OpenItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OpenItemRepository {
    private static final Logger logger = LoggerFactory.getLogger(OpenItemRepository.class);
    private final DataSource dataSource;
    
    public OpenItemRepository(DatabaseConfig databaseConfig) {
        this.dataSource = databaseConfig.getDataSource();
        initializeTable();
    }
    
    private void initializeTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // TEMPORARY: Force recreation of table to fix structure issues
            boolean forceRecreate = false; // Set to true if you need to recreate the table
            
            // First, check if we need to fix the table structure
            boolean needsRecreate = forceRecreate;
            if (!forceRecreate) {
                try {
                    // Try to access task_id column
                    ResultSet rs = stmt.executeQuery("SELECT task_id FROM open_items LIMIT 0");
                    rs.close();
                    logger.info("open_items table structure is OK");
                } catch (SQLException e) {
                    // Column doesn't exist - need to recreate table
                    logger.warn("open_items table has missing columns, will recreate it");
                    needsRecreate = true;
                }
            }
            
            if (needsRecreate) {
                // Backup existing data if any
                try {
                    logger.info("Backing up existing open_items data...");
                    stmt.execute("ALTER TABLE open_items RENAME TO open_items_backup");
                } catch (SQLException e) {
                    logger.info("No existing table to backup");
                }
                
                // Create new table with correct structure
                String createTableSql = """
                    CREATE TABLE open_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        project_id INTEGER NOT NULL,
                        task_id INTEGER,
                        item_number VARCHAR(50),
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        category VARCHAR(100),
                        priority VARCHAR(20) DEFAULT 'MEDIUM',
                        estimated_start_date DATE,
                        estimated_end_date DATE,
                        actual_start_date DATE,
                        actual_end_date DATE,
                        progress_percentage INTEGER DEFAULT 0,
                        status VARCHAR(50) DEFAULT 'NOT_STARTED',
                        health_status VARCHAR(50) DEFAULT 'ON_TRACK',
                        assigned_to VARCHAR(255),
                        assigned_resource_id INTEGER,
                        depends_on_item_id INTEGER,
                        blocks_item_ids TEXT,
                        notes TEXT,
                        tags TEXT,
                        estimated_hours DECIMAL(10,2),
                        actual_hours DECIMAL(10,2),
                        created_by VARCHAR(100),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_by VARCHAR(100),
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        is_deleted BOOLEAN DEFAULT 0,
                        deleted_at TIMESTAMP,
                        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                        FOREIGN KEY (assigned_resource_id) REFERENCES resources(id) ON DELETE SET NULL,
                        FOREIGN KEY (depends_on_item_id) REFERENCES open_items(id) ON DELETE SET NULL
                    )
                    """;
                stmt.execute(createTableSql);
                logger.info("Created new open_items table with correct structure");
                
                // Try to restore data from backup (only columns that exist in both)
                try {
                    String restoreSql = """
                        INSERT INTO open_items (
                            id, project_id, item_number, title, description, category,
                            priority, estimated_start_date, estimated_end_date,
                            actual_start_date, actual_end_date, progress_percentage,
                            status, health_status, assigned_to, assigned_resource_id,
                            depends_on_item_id, blocks_item_ids, notes, tags,
                            estimated_hours, actual_hours, created_by, created_at,
                            updated_by, updated_at, is_deleted, deleted_at
                        )
                        SELECT 
                            id, project_id, item_number, title, description, category,
                            priority, estimated_start_date, estimated_end_date,
                            actual_start_date, actual_end_date, progress_percentage,
                            status, health_status, assigned_to, assigned_resource_id,
                            depends_on_item_id, blocks_item_ids, notes, tags,
                            estimated_hours, actual_hours, created_by, created_at,
                            updated_by, updated_at, is_deleted, deleted_at
                        FROM open_items_backup
                        """;
                    int restored = stmt.executeUpdate(restoreSql);
                    logger.info("Restored {} items from backup", restored);
                    
                    // Drop the backup table
                    stmt.execute("DROP TABLE open_items_backup");
                } catch (SQLException e) {
                    logger.warn("Could not restore data from backup: {}", e.getMessage());
                }
            } else {
                // Table doesn't exist at all, create it
                String createTableSql = """
                    CREATE TABLE IF NOT EXISTS open_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        project_id INTEGER NOT NULL,
                        task_id INTEGER,
                        item_number VARCHAR(50),
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        category VARCHAR(100),
                        priority VARCHAR(20) DEFAULT 'MEDIUM',
                        estimated_start_date DATE,
                        estimated_end_date DATE,
                        actual_start_date DATE,
                        actual_end_date DATE,
                        progress_percentage INTEGER DEFAULT 0,
                        status VARCHAR(50) DEFAULT 'NOT_STARTED',
                        health_status VARCHAR(50) DEFAULT 'ON_TRACK',
                        assigned_to VARCHAR(255),
                        assigned_resource_id INTEGER,
                        depends_on_item_id INTEGER,
                        blocks_item_ids TEXT,
                        notes TEXT,
                        tags TEXT,
                        estimated_hours DECIMAL(10,2),
                        actual_hours DECIMAL(10,2),
                        created_by VARCHAR(100),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_by VARCHAR(100),
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        is_deleted BOOLEAN DEFAULT 0,
                        deleted_at TIMESTAMP,
                        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                        FOREIGN KEY (assigned_resource_id) REFERENCES resources(id) ON DELETE SET NULL,
                        FOREIGN KEY (depends_on_item_id) REFERENCES open_items(id) ON DELETE SET NULL
                    )
                    """;
                stmt.execute(createTableSql);
            }
            
            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_open_items_project_id ON open_items(project_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_open_items_status ON open_items(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_open_items_health_status ON open_items(health_status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_open_items_assigned_resource ON open_items(assigned_resource_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_open_items_priority ON open_items(priority)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_open_items_is_deleted ON open_items(is_deleted)");
            
            logger.info("Open items table initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize open items table", e);
        }
    }
    
    public OpenItem save(OpenItem item) {
        if (item.getId() == null) {
            return insert(item);
        } else {
            return update(item);
        }
    }
    
    private OpenItem insert(OpenItem item) {
        String sql = """
            INSERT INTO open_items (
                project_id, task_id, item_number, title, description, category, priority,
                estimated_start_date, estimated_end_date, actual_start_date, actual_end_date,
                progress_percentage, status, health_status, assigned_to, assigned_resource_id,
                depends_on_item_id, blocks_item_ids, notes, tags, estimated_hours, actual_hours,
                created_by, created_at, updated_by, updated_at, is_deleted
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        logger.info("Attempting to insert open item: title={}, projectId={}, status={}, priority={}", 
                    item.getTitle(), item.getProjectId(), item.getStatus(), item.getPriority());
        
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);  // Start transaction
            
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int paramCount = setParameters(ps, item);
                logger.info("Set {} parameters for insert", paramCount);
                
                int rowsAffected = ps.executeUpdate();
                logger.info("Insert affected {} rows", rowsAffected);
                
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        item.setId(rs.getLong(1));
                        logger.info("Successfully created open item: {} (ID: {})", item.getTitle(), item.getId());
                    } else {
                        logger.error("No generated keys returned after insert!");
                        conn.rollback();
                        return null;
                    }
                }
                
                conn.commit();  // Commit the transaction
                logger.info("Transaction committed successfully for item ID: {}", item.getId());
                
                // Verify the insert by reading it back
                String verifySql = "SELECT COUNT(*) FROM open_items WHERE id = ?";
                try (PreparedStatement verifyPs = conn.prepareStatement(verifySql)) {
                    verifyPs.setLong(1, item.getId());
                    try (ResultSet verifyRs = verifyPs.executeQuery()) {
                        if (verifyRs.next() && verifyRs.getInt(1) > 0) {
                            logger.info("Verified: Item with ID {} exists in database", item.getId());
                        } else {
                            logger.error("Verification failed: Item with ID {} not found in database!", item.getId());
                        }
                    }
                }
                
                return item;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.error("Transaction rolled back due to error");
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            logger.error("SQL Error during insert: " + e.getMessage() + " SQLState: " + e.getSQLState(), e);
            throw new RuntimeException("Failed to insert open item: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);  // Reset auto-commit
                    conn.close();
                } catch (SQLException closeEx) {
                    logger.error("Failed to close connection", closeEx);
                }
            }
        }
    }
    
    private OpenItem update(OpenItem item) {
        String sql = """
            UPDATE open_items SET
                project_id = ?, task_id = ?, item_number = ?, title = ?, description = ?,
                category = ?, priority = ?, estimated_start_date = ?, estimated_end_date = ?,
                actual_start_date = ?, actual_end_date = ?, progress_percentage = ?,
                status = ?, health_status = ?, assigned_to = ?, assigned_resource_id = ?,
                depends_on_item_id = ?, blocks_item_ids = ?, notes = ?, tags = ?,
                estimated_hours = ?, actual_hours = ?, updated_by = ?, updated_at = ?,
                is_deleted = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            int index = setParameters(ps, item);
            ps.setBoolean(index++, item.isDeleted()); // Add is_deleted for update
            ps.setLong(index, item.getId());
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Updated open item: {} (ID: {})", item.getTitle(), item.getId());
            }
            
            return item;
        } catch (SQLException e) {
            logger.error("Failed to update open item", e);
            throw new RuntimeException("Failed to update open item", e);
        }
    }
    
    private int setParameters(PreparedStatement ps, OpenItem item) throws SQLException {
        int index = 1;
        ps.setLong(index++, item.getProjectId());
        ps.setObject(index++, item.getTaskId());
        ps.setString(index++, item.getItemNumber());
        ps.setString(index++, item.getTitle());
        ps.setString(index++, item.getDescription());
        ps.setString(index++, item.getCategory());
        ps.setString(index++, item.getPriority() != null ? item.getPriority().name() : null);
        ps.setObject(index++, item.getEstimatedStartDate());
        ps.setObject(index++, item.getEstimatedEndDate());
        ps.setObject(index++, item.getActualStartDate());
        ps.setObject(index++, item.getActualEndDate());
        ps.setInt(index++, item.getProgressPercentage());
        ps.setString(index++, item.getStatus() != null ? item.getStatus().name() : null);
        ps.setString(index++, item.getHealthStatus() != null ? item.getHealthStatus().name() : null);
        ps.setString(index++, item.getAssignedTo());
        ps.setObject(index++, item.getAssignedResourceId());
        ps.setObject(index++, item.getDependsOnItemId());
        ps.setString(index++, item.getBlocksItemIds());
        ps.setString(index++, item.getNotes());
        ps.setString(index++, item.getTags());
        ps.setObject(index++, item.getEstimatedHours());
        ps.setObject(index++, item.getActualHours());
        
        // For insert, include created_by and created_at
        if (item.getId() == null) {
            ps.setString(index++, item.getCreatedBy());
            ps.setTimestamp(index++, Timestamp.valueOf(item.getCreatedAt()));
        }
        
        ps.setString(index++, item.getUpdatedBy());
        ps.setTimestamp(index++, Timestamp.valueOf(LocalDateTime.now()));
        
        // For insert, also set is_deleted to false
        if (item.getId() == null) {
            ps.setBoolean(index++, false);
            logger.info("Setting is_deleted to false at parameter index {}", index - 1);
        }
        
        return index;
    }
    
    public Optional<OpenItem> findById(Long id) {
        String sql = "SELECT * FROM open_items WHERE id = ? AND is_deleted = 0";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOpenItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find open item by id: " + id, e);
        }
        
        return Optional.empty();
    }
    
    public List<OpenItem> findByProjectId(Long projectId) {
        String sql = "SELECT * FROM open_items WHERE project_id = ? AND is_deleted = 0 ORDER BY priority DESC, estimated_start_date";
        List<OpenItem> items = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, projectId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToOpenItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find open items by project id: " + projectId, e);
        }
        
        return items;
    }
    
    public List<OpenItem> findByStatus(OpenItem.ItemStatus status) {
        String sql = "SELECT * FROM open_items WHERE status = ? AND is_deleted = 0 ORDER BY priority DESC, estimated_end_date";
        List<OpenItem> items = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, status.name());
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToOpenItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find open items by status: " + status, e);
        }
        
        return items;
    }
    
    public List<OpenItem> findByResourceId(Long resourceId) {
        String sql = "SELECT * FROM open_items WHERE assigned_resource_id = ? AND is_deleted = 0 ORDER BY priority DESC, estimated_start_date";
        List<OpenItem> items = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, resourceId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToOpenItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find open items by resource id: " + resourceId, e);
        }
        
        return items;
    }
    
    public List<OpenItem> findAll() {
        String sql = "SELECT * FROM open_items WHERE is_deleted = 0 ORDER BY project_id, priority DESC, estimated_start_date";
        List<OpenItem> items = new ArrayList<>();
        
        logger.info("Executing findAll query: {}", sql);
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                try {
                    OpenItem item = mapResultSetToOpenItem(rs);
                    items.add(item);
                    logger.info("Loaded item #{}: {} (ID: {}, ProjectID: {}, isDeleted: {})", 
                               count, item.getTitle(), item.getId(), item.getProjectId(), item.isDeleted());
                } catch (Exception e) {
                    logger.error("Failed to map row #{} to OpenItem", count, e);
                }
            }
            
            logger.info("findAll processed {} rows, loaded {} open items from database", count, items.size());
        } catch (SQLException e) {
            logger.error("Failed to find all open items", e);
        }
        
        return items;
    }
    
    public List<OpenItem> findOverdueItems() {
        String sql = """
            SELECT * FROM open_items 
            WHERE is_deleted = 0 
            AND status NOT IN ('COMPLETED', 'CANCELLED')
            AND estimated_end_date < ?
            ORDER BY estimated_end_date, priority DESC
            """;
        List<OpenItem> items = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToOpenItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find overdue items", e);
        }
        
        return items;
    }
    
    public List<OpenItem> findAtRiskItems() {
        String sql = """
            SELECT * FROM open_items 
            WHERE is_deleted = 0 
            AND health_status IN ('AT_RISK', 'DELAYED', 'CRITICAL')
            ORDER BY 
                CASE health_status 
                    WHEN 'CRITICAL' THEN 1 
                    WHEN 'DELAYED' THEN 2 
                    WHEN 'AT_RISK' THEN 3 
                END,
                priority DESC
            """;
        List<OpenItem> items = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                items.add(mapResultSetToOpenItem(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find at-risk items", e);
        }
        
        return items;
    }
    
    public void delete(Long id) {
        String sql = "UPDATE open_items SET is_deleted = 1, deleted_at = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, id);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Soft deleted open item with id: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete open item with id: " + id, e);
            throw new RuntimeException("Failed to delete open item", e);
        }
    }
    
    public void hardDelete(Long id) {
        String sql = "DELETE FROM open_items WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, id);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Hard deleted open item with id: {}", id);
            }
        } catch (SQLException e) {
            logger.error("Failed to hard delete open item with id: " + id, e);
            throw new RuntimeException("Failed to hard delete open item", e);
        }
    }
    
    private OpenItem mapResultSetToOpenItem(ResultSet rs) throws SQLException {
        OpenItem item = new OpenItem();
        
        item.setId(rs.getLong("id"));
        item.setProjectId(rs.getLong("project_id"));
        
        // Safely handle task_id which might be null
        try {
            long taskIdValue = rs.getLong("task_id");
            if (!rs.wasNull()) {
                item.setTaskId(taskIdValue);
            }
        } catch (SQLException e) {
            // Column might not exist in old data
            logger.debug("task_id column issue: {}", e.getMessage());
        }
        
        item.setItemNumber(rs.getString("item_number"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setCategory(rs.getString("category"));
        
        String priority = rs.getString("priority");
        if (priority != null && !priority.isEmpty()) {
            try {
                item.setPriority(OpenItem.Priority.valueOf(priority.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid priority value: '{}', setting to MEDIUM", priority);
                item.setPriority(OpenItem.Priority.MEDIUM);
            }
        }
        
        try {
            Date estStartDate = rs.getDate("estimated_start_date");
            if (estStartDate != null) item.setEstimatedStartDate(estStartDate.toLocalDate());
        } catch (SQLException e) {
            logger.debug("No estimated_start_date for item");
        }
        
        try {
            Date estEndDate = rs.getDate("estimated_end_date");
            if (estEndDate != null) item.setEstimatedEndDate(estEndDate.toLocalDate());
        } catch (SQLException e) {
            logger.debug("No estimated_end_date for item");
        }
        
        try {
            Date actStartDate = rs.getDate("actual_start_date");
            if (actStartDate != null) item.setActualStartDate(actStartDate.toLocalDate());
        } catch (SQLException e) {
            logger.debug("No actual_start_date for item");
        }
        
        try {
            Date actEndDate = rs.getDate("actual_end_date");
            if (actEndDate != null) item.setActualEndDate(actEndDate.toLocalDate());
        } catch (SQLException e) {
            logger.debug("No actual_end_date for item");
        }
        
        item.setProgressPercentage(rs.getInt("progress_percentage"));
        
        String status = rs.getString("status");
        if (status != null && !status.isEmpty()) {
            try {
                item.setStatus(OpenItem.ItemStatus.valueOf(status.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status value: '{}', setting to NOT_STARTED", status);
                item.setStatus(OpenItem.ItemStatus.NOT_STARTED);
            }
        }
        
        String healthStatus = rs.getString("health_status");
        if (healthStatus != null && !healthStatus.isEmpty()) {
            try {
                item.setHealthStatus(OpenItem.HealthStatus.valueOf(healthStatus.toUpperCase().replace(" ", "_")));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid health status value: '{}', setting to ON_TRACK", healthStatus);
                item.setHealthStatus(OpenItem.HealthStatus.ON_TRACK);
            }
        }
        
        item.setAssignedTo(rs.getString("assigned_to"));
        
        // Safely handle assigned_resource_id
        long assignedResourceId = rs.getLong("assigned_resource_id");
        if (!rs.wasNull()) {
            item.setAssignedResourceId(assignedResourceId);
        }
        
        // Safely handle depends_on_item_id
        long dependsOnId = rs.getLong("depends_on_item_id");
        if (!rs.wasNull()) {
            item.setDependsOnItemId(dependsOnId);
        }
        
        item.setBlocksItemIds(rs.getString("blocks_item_ids"));
        item.setNotes(rs.getString("notes"));
        item.setTags(rs.getString("tags"));
        
        // Safely handle estimated_hours
        double estHours = rs.getDouble("estimated_hours");
        if (!rs.wasNull()) {
            item.setEstimatedHours(estHours);
        }
        
        // Safely handle actual_hours
        double actHours = rs.getDouble("actual_hours");
        if (!rs.wasNull()) {
            item.setActualHours(actHours);
        }
        
        item.setCreatedBy(rs.getString("created_by"));
        
        try {
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                item.setCreatedAt(createdAt.toLocalDateTime());
            } else {
                item.setCreatedAt(LocalDateTime.now()); // Default to now if null
            }
        } catch (SQLException e) {
            logger.debug("Error reading created_at: {}", e.getMessage());
            item.setCreatedAt(LocalDateTime.now());
        }
        
        item.setUpdatedBy(rs.getString("updated_by"));
        
        try {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                item.setUpdatedAt(updatedAt.toLocalDateTime());
            } else {
                item.setUpdatedAt(LocalDateTime.now()); // Default to now if null
            }
        } catch (SQLException e) {
            logger.debug("Error reading updated_at: {}", e.getMessage());
            item.setUpdatedAt(LocalDateTime.now());
        }
        
        item.setDeleted(rs.getBoolean("is_deleted"));
        
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) item.setDeletedAt(deletedAt.toLocalDateTime());
        
        return item;
    }
    
    // Generate next item number
    public String generateItemNumber(Long projectId) {
        String sql = "SELECT COUNT(*) + 1 FROM open_items WHERE project_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, projectId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return String.format("OI-%d-%03d", projectId, count);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to generate item number for project: " + projectId, e);
        }
        
        return String.format("OI-%d-%03d", projectId, 1);
    }
}