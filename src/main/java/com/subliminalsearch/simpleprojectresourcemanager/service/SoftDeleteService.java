package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SoftDeleteService {
    private static final Logger logger = LoggerFactory.getLogger(SoftDeleteService.class);
    
    private final HikariDataSource dataSource;
    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;
    private final ResourceRepository resourceRepository;
    private final TaskRepository taskRepository;
    
    // Store recent deletions for undo functionality (in-memory for session)
    private final Map<String, DeletedEntity> recentDeletions = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, DeletedEntity> eldest) {
            // Keep only last 10 deletions in memory for undo
            return size() > 10;
        }
    };
    
    public SoftDeleteService(HikariDataSource dataSource,
                            ProjectRepository projectRepository,
                            AssignmentRepository assignmentRepository,
                            ResourceRepository resourceRepository) {
        this.dataSource = dataSource;
        this.projectRepository = projectRepository;
        this.assignmentRepository = assignmentRepository;
        this.resourceRepository = resourceRepository;
        this.taskRepository = new TaskRepository(dataSource);
        
        // Ensure soft delete columns exist
        ensureSoftDeleteSchema();
    }
    
    private void ensureSoftDeleteSchema() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            
            // Check if deleted_at column exists in projects table
            ResultSet rs = meta.getColumns(null, null, "projects", "deleted_at");
            if (!rs.next()) {
                logger.info("Adding soft delete support to database...");
                executeSqlFile(conn, "/db/add_soft_delete.sql");
            }
            rs.close();
            
        } catch (SQLException e) {
            logger.error("Error checking soft delete schema", e);
        }
    }
    
    private void executeSqlFile(Connection conn, String resourcePath) {
        try {
            String sql = new String(getClass().getResourceAsStream(resourcePath).readAllBytes());
            String[] statements = sql.split(";");
            
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            logger.info("Soft delete schema applied successfully");
            
        } catch (Exception e) {
            logger.error("Error executing SQL file: " + resourcePath, e);
        }
    }
    
    /**
     * Soft delete a project and all its assignments
     * Returns a unique ID that can be used to undo the deletion
     */
    public String softDeleteProject(Long projectId, String deletedBy) {
        String undoId = "project_" + projectId + "_" + System.currentTimeMillis();
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            LocalDateTime now = LocalDateTime.now();
            List<Long> deletedAssignmentIds = new ArrayList<>();
            List<Long> deletedTaskIds = new ArrayList<>();
            
            // Get project details for the undo record
            Optional<Project> project = projectRepository.findById(projectId);
            if (project.isEmpty()) {
                throw new IllegalArgumentException("Project not found: " + projectId);
            }
            
            // Soft delete all assignments for this project
            String updateAssignments = "UPDATE assignments SET deleted_at = ?, deleted_by = ? WHERE project_id = ? AND deleted_at IS NULL";
            try (PreparedStatement stmt = conn.prepareStatement(updateAssignments)) {
                stmt.setTimestamp(1, Timestamp.valueOf(now));
                stmt.setString(2, deletedBy);
                stmt.setLong(3, projectId);
                int assignmentCount = stmt.executeUpdate();
                
                // Get the IDs of deleted assignments
                String getDeletedAssignments = "SELECT id FROM assignments WHERE project_id = ? AND deleted_at = ?";
                try (PreparedStatement getStmt = conn.prepareStatement(getDeletedAssignments)) {
                    getStmt.setLong(1, projectId);
                    getStmt.setTimestamp(2, Timestamp.valueOf(now));
                    ResultSet rs = getStmt.executeQuery();
                    while (rs.next()) {
                        deletedAssignmentIds.add(rs.getLong("id"));
                    }
                }
                
                logger.info("Soft deleted {} assignments for project {}", assignmentCount, projectId);
            }
            
            // Soft delete all tasks for this project
            String updateTasks = "UPDATE tasks SET deleted_at = ?, deleted_by = ? WHERE project_id = ? AND deleted_at IS NULL";
            try (PreparedStatement stmt = conn.prepareStatement(updateTasks)) {
                stmt.setTimestamp(1, Timestamp.valueOf(now));
                stmt.setString(2, deletedBy);
                stmt.setLong(3, projectId);
                int taskCount = stmt.executeUpdate();
                
                // Get the IDs of deleted tasks
                String getDeletedTasks = "SELECT id FROM tasks WHERE project_id = ? AND deleted_at = ?";
                try (PreparedStatement getStmt = conn.prepareStatement(getDeletedTasks)) {
                    getStmt.setLong(1, projectId);
                    getStmt.setTimestamp(2, Timestamp.valueOf(now));
                    ResultSet rs = getStmt.executeQuery();
                    while (rs.next()) {
                        deletedTaskIds.add(rs.getLong("id"));
                    }
                }
                
                logger.info("Soft deleted {} tasks for project {}", taskCount, projectId);
            }
            
            // Soft delete the project
            String updateProject = "UPDATE projects SET deleted_at = ?, deleted_by = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateProject)) {
                stmt.setTimestamp(1, Timestamp.valueOf(now));
                stmt.setString(2, deletedBy);
                stmt.setLong(3, projectId);
                stmt.executeUpdate();
            }
            
            // Add to trash metadata
            String insertTrash = "INSERT OR REPLACE INTO trash_metadata (entity_type, entity_id, entity_name, deleted_at, deleted_by, related_deletions) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertTrash)) {
                stmt.setString(1, "project");
                stmt.setLong(2, projectId);
                stmt.setString(3, project.get().getProjectId());
                stmt.setTimestamp(4, Timestamp.valueOf(now));
                stmt.setString(5, deletedBy);
                
                // Create JSON-like string for related deletions
                String related = String.format("{\"assignments\":%d,\"tasks\":%d}", 
                    deletedAssignmentIds.size(), deletedTaskIds.size());
                stmt.setString(6, related);
                stmt.executeUpdate();
            }
            
            conn.commit();
            
            // Store in recent deletions for undo
            DeletedEntity deleted = new DeletedEntity(
                "project", projectId, project.get().getProjectId(),
                now, deletedBy, deletedAssignmentIds, deletedTaskIds
            );
            recentDeletions.put(undoId, deleted);
            
            logger.info("Soft deleted project {} with undo ID: {}", project.get().getProjectId(), undoId);
            return undoId;
            
        } catch (SQLException e) {
            logger.error("Error soft deleting project", e);
            throw new RuntimeException("Failed to delete project: " + e.getMessage(), e);
        }
    }
    
    /**
     * Soft delete a single assignment
     */
    public String softDeleteAssignment(Long assignmentId, String deletedBy) {
        String undoId = "assignment_" + assignmentId + "_" + System.currentTimeMillis();
        
        try (Connection conn = dataSource.getConnection()) {
            LocalDateTime now = LocalDateTime.now();
            
            String updateAssignment = "UPDATE assignments SET deleted_at = ?, deleted_by = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateAssignment)) {
                stmt.setTimestamp(1, Timestamp.valueOf(now));
                stmt.setString(2, deletedBy);
                stmt.setLong(3, assignmentId);
                stmt.executeUpdate();
            }
            
            // Store for undo
            DeletedEntity deleted = new DeletedEntity(
                "assignment", assignmentId, "Assignment " + assignmentId,
                now, deletedBy, Collections.emptyList(), Collections.emptyList()
            );
            recentDeletions.put(undoId, deleted);
            
            logger.info("Soft deleted assignment {} with undo ID: {}", assignmentId, undoId);
            return undoId;
            
        } catch (SQLException e) {
            logger.error("Error soft deleting assignment", e);
            throw new RuntimeException("Failed to delete assignment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Restore a soft-deleted entity
     */
    public boolean restore(String undoId) {
        DeletedEntity deleted = recentDeletions.get(undoId);
        if (deleted == null) {
            logger.warn("No undo record found for ID: {}", undoId);
            return false;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            if ("project".equals(deleted.entityType)) {
                // Restore project
                String restoreProject = "UPDATE projects SET deleted_at = NULL, deleted_by = NULL WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(restoreProject)) {
                    stmt.setLong(1, deleted.entityId);
                    stmt.executeUpdate();
                }
                
                // Restore assignments
                if (!deleted.deletedAssignmentIds.isEmpty()) {
                    String restoreAssignments = "UPDATE assignments SET deleted_at = NULL, deleted_by = NULL WHERE id IN (" +
                        String.join(",", Collections.nCopies(deleted.deletedAssignmentIds.size(), "?")) + ")";
                    try (PreparedStatement stmt = conn.prepareStatement(restoreAssignments)) {
                        int idx = 1;
                        for (Long id : deleted.deletedAssignmentIds) {
                            stmt.setLong(idx++, id);
                        }
                        stmt.executeUpdate();
                    }
                }
                
                // Restore tasks
                if (!deleted.deletedTaskIds.isEmpty()) {
                    String restoreTasks = "UPDATE tasks SET deleted_at = NULL, deleted_by = NULL WHERE id IN (" +
                        String.join(",", Collections.nCopies(deleted.deletedTaskIds.size(), "?")) + ")";
                    try (PreparedStatement stmt = conn.prepareStatement(restoreTasks)) {
                        int idx = 1;
                        for (Long id : deleted.deletedTaskIds) {
                            stmt.setLong(idx++, id);
                        }
                        stmt.executeUpdate();
                    }
                }
                
                // Remove from trash metadata
                String removeTrash = "DELETE FROM trash_metadata WHERE entity_type = ? AND entity_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(removeTrash)) {
                    stmt.setString(1, "project");
                    stmt.setLong(2, deleted.entityId);
                    stmt.executeUpdate();
                }
                
            } else if ("assignment".equals(deleted.entityType)) {
                // Restore assignment
                String restoreAssignment = "UPDATE assignments SET deleted_at = NULL, deleted_by = NULL WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(restoreAssignment)) {
                    stmt.setLong(1, deleted.entityId);
                    stmt.executeUpdate();
                }
            }
            
            conn.commit();
            recentDeletions.remove(undoId);
            
            logger.info("Restored {} with ID: {}", deleted.entityType, deleted.entityId);
            return true;
            
        } catch (SQLException e) {
            logger.error("Error restoring entity", e);
            return false;
        }
    }
    
    /**
     * Get items in the trash
     */
    public List<TrashItem> getTrashItems() {
        List<TrashItem> items = new ArrayList<>();
        
        String query = "SELECT * FROM trash_metadata WHERE permanently_deleted_at IS NULL ORDER BY deleted_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                TrashItem item = new TrashItem(
                    rs.getString("entity_type"),
                    rs.getLong("entity_id"),
                    rs.getString("entity_name"),
                    rs.getTimestamp("deleted_at").toLocalDateTime(),
                    rs.getString("deleted_by"),
                    rs.getString("related_deletions"),
                    rs.getBoolean("can_restore")
                );
                items.add(item);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting trash items", e);
        }
        
        return items;
    }
    
    /**
     * Permanently delete an item from trash
     */
    public void permanentlyDelete(String entityType, Long entityId) {
        // This would require the user to type a confirmation string
        // Implementation would actually DELETE the records from database
        logger.info("Permanently deleting {} with ID: {}", entityType, entityId);
        // TODO: Implement permanent deletion
    }
    
    // Helper classes
    private static class DeletedEntity {
        final String entityType;
        final Long entityId;
        final String entityName;
        final LocalDateTime deletedAt;
        final String deletedBy;
        final List<Long> deletedAssignmentIds;
        final List<Long> deletedTaskIds;
        
        DeletedEntity(String entityType, Long entityId, String entityName, 
                     LocalDateTime deletedAt, String deletedBy,
                     List<Long> deletedAssignmentIds, List<Long> deletedTaskIds) {
            this.entityType = entityType;
            this.entityId = entityId;
            this.entityName = entityName;
            this.deletedAt = deletedAt;
            this.deletedBy = deletedBy;
            this.deletedAssignmentIds = new ArrayList<>(deletedAssignmentIds);
            this.deletedTaskIds = new ArrayList<>(deletedTaskIds);
        }
    }
    
    public static class TrashItem {
        private final String entityType;
        private final Long entityId;
        private final String entityName;
        private final LocalDateTime deletedAt;
        private final String deletedBy;
        private final String relatedDeletions;
        private final boolean canRestore;
        
        public TrashItem(String entityType, Long entityId, String entityName,
                        LocalDateTime deletedAt, String deletedBy, 
                        String relatedDeletions, boolean canRestore) {
            this.entityType = entityType;
            this.entityId = entityId;
            this.entityName = entityName;
            this.deletedAt = deletedAt;
            this.deletedBy = deletedBy;
            this.relatedDeletions = relatedDeletions;
            this.canRestore = canRestore;
        }
        
        // Getters
        public String getEntityType() { return entityType; }
        public Long getEntityId() { return entityId; }
        public String getEntityName() { return entityName; }
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public String getDeletedBy() { return deletedBy; }
        public String getRelatedDeletions() { return relatedDeletions; }
        public boolean isCanRestore() { return canRestore; }
    }
}