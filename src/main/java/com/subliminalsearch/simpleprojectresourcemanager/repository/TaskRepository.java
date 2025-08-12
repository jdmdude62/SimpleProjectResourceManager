package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.model.TaskDependency;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskRepository {
    private static final Logger logger = LoggerFactory.getLogger(TaskRepository.class);
    private final HikariDataSource dataSource;
    
    public TaskRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    // Find the maximum planned_start date for a project
    public LocalDate findMaxDateForProject(Long projectId) {
        String sql = """
            SELECT MAX(planned_start) as max_date
            FROM tasks
            WHERE project_id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, projectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String maxDate = rs.getString("max_date");
                    if (maxDate != null && !maxDate.isEmpty()) {
                        // Extract just the date part if it has time
                        if (maxDate.length() > 10) {
                            maxDate = maxDate.substring(0, 10);
                        }
                        return LocalDate.parse(maxDate);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding max date for project", e);
        }
        
        return null; // No tasks found
    }
    
    // Find the maximum datetime across ALL project tasks and return the next second
    private String findMaxDateTimeForDate(Long projectId, LocalDate requestedDate) {
        if (requestedDate == null) return null;
        
        // Find the maximum planned_start across ALL tasks in the project
        String sql = """
            SELECT MAX(planned_start) as max_datetime
            FROM tasks
            WHERE project_id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, projectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String maxDateTime = rs.getString("max_datetime");
                    if (maxDateTime != null && !maxDateTime.isEmpty()) {
                        // Parse the max datetime
                        LocalDate maxDate;
                        LocalTime maxTime = LocalTime.of(0, 0, 0);
                        
                        if (maxDateTime.length() > 10) {
                            // Has time component "YYYY-MM-DD HH:mm:ss"
                            String[] parts = maxDateTime.split(" ");
                            maxDate = LocalDate.parse(parts[0]);
                            if (parts.length > 1) {
                                maxTime = LocalTime.parse(parts[1]);
                            }
                        } else {
                            // Only date
                            maxDate = LocalDate.parse(maxDateTime);
                        }
                        
                        // Use the later of the requested date or the max date found
                        LocalDate targetDate = requestedDate.isAfter(maxDate) ? requestedDate : maxDate;
                        
                        // If using the max date, add 1 second to the time
                        if (targetDate.equals(maxDate)) {
                            maxTime = maxTime.plusSeconds(1);
                        } else {
                            // New date, start at 00:00:00
                            maxTime = LocalTime.of(0, 0, 0);
                        }
                        
                        return targetDate + " " + maxTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding max datetime for project", e);
        }
        
        // If no existing tasks, use the requested date at 00:00:00
        return requestedDate + " 00:00:00";
    }
    
    // Create a new task
    public Task create(Task task) {
        // First, find the maximum planned_start datetime for this date
        String maxDateTimeForDate = findMaxDateTimeForDate(task.getProjectId(), task.getPlannedStart());
        
        String sql = """
            INSERT INTO tasks (
                project_id, phase_id, parent_task_id, task_code, title, description,
                task_type, priority, status, progress_percentage,
                planned_start, planned_end, estimated_hours,
                assigned_to, assigned_by, reviewer_id,
                location, equipment_required, safety_requirements, site_access_notes,
                risk_level, risk_notes, created_by, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, task.getProjectId());
            stmt.setObject(2, task.getPhaseId());
            stmt.setObject(3, task.getParentTaskId());
            stmt.setString(4, task.getTaskCode());
            stmt.setString(5, task.getTitle());
            stmt.setString(6, task.getDescription());
            stmt.setString(7, task.getTaskType() != null ? task.getTaskType().name() : "GENERAL");
            stmt.setString(8, task.getPriority() != null ? task.getPriority().name() : "MEDIUM");
            stmt.setString(9, task.getStatus() != null ? task.getStatus().name() : "NOT_STARTED");
            stmt.setInt(10, task.getProgressPercentage() != null ? task.getProgressPercentage() : 0);
            // Set planned_start to max datetime + 1 second to ensure it appears at the end
            if (task.getPlannedStart() != null) {
                stmt.setString(11, maxDateTimeForDate);
            } else {
                stmt.setObject(11, null);
            }
            stmt.setObject(12, task.getPlannedEnd());
            stmt.setObject(13, task.getEstimatedHours());
            stmt.setObject(14, task.getAssignedTo());
            stmt.setObject(15, task.getAssignedBy());
            stmt.setObject(16, task.getReviewerId());
            stmt.setString(17, task.getLocation());
            stmt.setString(18, task.getEquipmentRequired());
            stmt.setString(19, task.getSafetyRequirements());
            stmt.setString(20, task.getSiteAccessNotes());
            stmt.setString(21, task.getRiskLevel() != null ? task.getRiskLevel().name() : "LOW");
            stmt.setString(22, task.getRiskNotes());
            stmt.setObject(23, task.getCreatedBy());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    task.setId(rs.getLong(1));
                    logger.debug("Created task with ID: {}", task.getId());
                }
            }
            
            return task;
        } catch (SQLException e) {
            logger.error("Error creating task", e);
            throw new RuntimeException("Failed to create task", e);
        }
    }
    
    // Update an existing task
    public void update(Task task) {
        String sql = """
            UPDATE tasks SET
                title = ?, description = ?, task_type = ?, priority = ?, status = ?,
                progress_percentage = ?, planned_start = ?, planned_end = ?,
                actual_start = ?, actual_end = ?, estimated_hours = ?, actual_hours = ?,
                assigned_to = ?, reviewer_id = ?, location = ?, equipment_required = ?,
                safety_requirements = ?, site_access_notes = ?, risk_level = ?, risk_notes = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getTaskType() != null ? task.getTaskType().name() : null);
            stmt.setString(4, task.getPriority() != null ? task.getPriority().name() : null);
            stmt.setString(5, task.getStatus() != null ? task.getStatus().name() : null);
            stmt.setInt(6, task.getProgressPercentage() != null ? task.getProgressPercentage() : 0);
            stmt.setObject(7, task.getPlannedStart());
            stmt.setObject(8, task.getPlannedEnd());
            stmt.setObject(9, task.getActualStart());
            stmt.setObject(10, task.getActualEnd());
            stmt.setObject(11, task.getEstimatedHours());
            stmt.setObject(12, task.getActualHours());
            stmt.setObject(13, task.getAssignedTo());
            stmt.setObject(14, task.getReviewerId());
            stmt.setString(15, task.getLocation());
            stmt.setString(16, task.getEquipmentRequired());
            stmt.setString(17, task.getSafetyRequirements());
            stmt.setString(18, task.getSiteAccessNotes());
            stmt.setString(19, task.getRiskLevel() != null ? task.getRiskLevel().name() : null);
            stmt.setString(20, task.getRiskNotes());
            stmt.setLong(21, task.getId());
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Updated {} task(s)", rowsAffected);
            
            // Update completion timestamp if status changed to completed
            if (task.getStatus() == Task.TaskStatus.COMPLETED) {
                updateCompletionTimestamp(task.getId());
            }
            
        } catch (SQLException e) {
            logger.error("Error updating task", e);
            throw new RuntimeException("Failed to update task", e);
        }
    }
    
    // Find task by ID
    public Optional<Task> findById(Long id) {
        String sql = """
            SELECT t.*, r.name as assigned_to_name, p.project_id as project_name
            FROM tasks t
            LEFT JOIN resources r ON t.assigned_to = r.id
            LEFT JOIN projects p ON t.project_id = p.id
            WHERE t.id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding task by ID: {}", id, e);
        }
        return Optional.empty();
    }
    
    // Find all tasks for a project
    public List<Task> findByProjectId(Long projectId) {
        String sql = """
            SELECT t.*, r.name as assigned_to_name,
                   (SELECT COUNT(*) FROM tasks st WHERE st.parent_task_id = t.id) as subtask_count,
                   (SELECT COUNT(*) FROM tasks st WHERE st.parent_task_id = t.id AND st.status = 'COMPLETED') as completed_subtask_count
            FROM tasks t
            LEFT JOIN resources r ON t.assigned_to = r.id
            WHERE t.project_id = ?
            ORDER BY t.phase_id, t.parent_task_id NULLS FIRST, t.planned_start, COALESCE(t.created_at, datetime('2025-01-01', '+' || t.id || ' seconds')), t.id
            """;
        
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, projectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = mapResultSetToTask(rs);
                    task.setSubtaskCount(rs.getInt("subtask_count"));
                    task.setCompletedSubtaskCount(rs.getInt("completed_subtask_count"));
                    tasks.add(task);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding tasks for project: {}", projectId, e);
        }
        return tasks;
    }
    
    // Find all tasks assigned to a resource
    public List<Task> findByResourceId(Long resourceId) {
        String sql = """
            SELECT t.*, p.project_id as project_name
            FROM tasks t
            LEFT JOIN projects p ON t.project_id = p.id
            WHERE t.assigned_to = ? OR t.id IN (
                SELECT task_id FROM task_resources WHERE resource_id = ?
            )
            ORDER BY t.planned_start, t.priority DESC
            """;
        
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, resourceId);
            stmt.setLong(2, resourceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = mapResultSetToTask(rs);
                    task.setProjectName(rs.getString("project_name"));
                    tasks.add(task);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding tasks for resource: {}", resourceId, e);
        }
        return tasks;
    }
    
    // Find overdue tasks
    public List<Task> findOverdueTasks() {
        String sql = """
            SELECT t.*, r.name as assigned_to_name, p.project_id as project_name
            FROM tasks t
            LEFT JOIN resources r ON t.assigned_to = r.id
            LEFT JOIN projects p ON t.project_id = p.id
            WHERE t.planned_end < CURRENT_DATE
              AND t.status NOT IN ('COMPLETED', 'CANCELLED')
            ORDER BY t.planned_end, t.priority DESC
            """;
        
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = mapResultSetToTask(rs);
                    task.setProjectName(rs.getString("project_name"));
                    tasks.add(task);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding overdue tasks", e);
        }
        return tasks;
    }
    
    // Add task dependency
    public void addDependency(Long predecessorId, Long successorId, String dependencyType, Integer lagDays) {
        String sql = """
            INSERT INTO task_dependencies (predecessor_id, successor_id, dependency_type, lag_days)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (predecessor_id, successor_id) DO UPDATE SET
                dependency_type = excluded.dependency_type,
                lag_days = excluded.lag_days
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, predecessorId);
            stmt.setLong(2, successorId);
            stmt.setString(3, dependencyType);
            stmt.setInt(4, lagDays != null ? lagDays : 0);
            
            stmt.executeUpdate();
            logger.debug("Added dependency: {} -> {}", predecessorId, successorId);
            
        } catch (SQLException e) {
            logger.error("Error adding task dependency", e);
            throw new RuntimeException("Failed to add task dependency", e);
        }
    }
    
    // Get task dependencies
    public List<TaskDependency> findDependencies(Long taskId) {
        String sql = """
            SELECT d.*, 
                   p.title as predecessor_title,
                   s.title as successor_title
            FROM task_dependencies d
            JOIN tasks p ON d.predecessor_id = p.id
            JOIN tasks s ON d.successor_id = s.id
            WHERE d.successor_id = ? OR d.predecessor_id = ?
            """;
        
        List<TaskDependency> dependencies = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, taskId);
            stmt.setLong(2, taskId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TaskDependency dep = new TaskDependency();
                    dep.setId(rs.getLong("id"));
                    dep.setPredecessorId(rs.getLong("predecessor_id"));
                    dep.setSuccessorId(rs.getLong("successor_id"));
                    dep.setDependencyType(TaskDependency.DependencyType.fromCode(rs.getString("dependency_type")));
                    dep.setLagDays(rs.getInt("lag_days"));
                    dep.setPredecessorTitle(rs.getString("predecessor_title"));
                    dep.setSuccessorTitle(rs.getString("successor_title"));
                    dependencies.add(dep);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding dependencies for task: {}", taskId, e);
        }
        return dependencies;
    }
    
    // Delete task
    public void delete(Long id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Deleted {} task(s)", rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error deleting task", e);
            throw new RuntimeException("Failed to delete task", e);
        }
    }
    
    // Update task status
    public void updateStatus(Long taskId, Task.TaskStatus status) {
        String sql = "UPDATE tasks SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            stmt.setLong(2, taskId);
            
            stmt.executeUpdate();
            
            if (status == Task.TaskStatus.COMPLETED) {
                updateCompletionTimestamp(taskId);
            }
            
        } catch (SQLException e) {
            logger.error("Error updating task status", e);
            throw new RuntimeException("Failed to update task status", e);
        }
    }
    
    // Update task progress
    public void updateProgress(Long taskId, Integer progressPercentage) {
        String sql = "UPDATE tasks SET progress_percentage = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, progressPercentage);
            stmt.setLong(2, taskId);
            
            stmt.executeUpdate();
            
            // Auto-update status based on progress
            if (progressPercentage == 100) {
                updateStatus(taskId, Task.TaskStatus.COMPLETED);
            } else if (progressPercentage > 0) {
                updateStatus(taskId, Task.TaskStatus.IN_PROGRESS);
            }
            
        } catch (SQLException e) {
            logger.error("Error updating task progress", e);
            throw new RuntimeException("Failed to update task progress", e);
        }
    }
    
    // Helper method to update completion timestamp
    private void updateCompletionTimestamp(Long taskId) {
        String sql = "UPDATE tasks SET completed_at = CURRENT_TIMESTAMP WHERE id = ? AND completed_at IS NULL";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, taskId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Error updating completion timestamp", e);
        }
    }
    
    // Helper method to map ResultSet to Task
    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setProjectId(rs.getLong("project_id"));
        
        // Handle nullable Long values for SQLite compatibility
        long phaseId = rs.getLong("phase_id");
        task.setPhaseId(rs.wasNull() ? null : phaseId);
        
        long parentTaskId = rs.getLong("parent_task_id");
        task.setParentTaskId(rs.wasNull() ? null : parentTaskId);
        task.setTaskCode(rs.getString("task_code"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        
        String taskType = rs.getString("task_type");
        if (taskType != null) {
            task.setTaskType(Task.TaskType.valueOf(taskType));
        }
        
        String priority = rs.getString("priority");
        if (priority != null) {
            task.setPriority(Task.TaskPriority.valueOf(priority));
        }
        
        String status = rs.getString("status");
        if (status != null) {
            task.setStatus(Task.TaskStatus.valueOf(status));
        }
        
        task.setProgressPercentage(rs.getInt("progress_percentage"));
        
        // Handle date fields as strings for SQLite compatibility
        // Parse both date and datetime formats
        String plannedStart = rs.getString("planned_start");
        if (plannedStart != null && !plannedStart.isEmpty()) {
            // Handle both "YYYY-MM-DD" and "YYYY-MM-DD HH:mm:ss" formats
            if (plannedStart.length() > 10) {
                // Has time component, extract just the date part
                task.setPlannedStart(LocalDate.parse(plannedStart.substring(0, 10)));
            } else {
                task.setPlannedStart(LocalDate.parse(plannedStart));
            }
        }
        
        String plannedEnd = rs.getString("planned_end");
        if (plannedEnd != null && !plannedEnd.isEmpty()) {
            // Handle both "YYYY-MM-DD" and "YYYY-MM-DD HH:mm:ss" formats
            if (plannedEnd.length() > 10) {
                // Has time component, extract just the date part
                task.setPlannedEnd(LocalDate.parse(plannedEnd.substring(0, 10)));
            } else {
                task.setPlannedEnd(LocalDate.parse(plannedEnd));
            }
        }
        
        String actualStart = rs.getString("actual_start");
        if (actualStart != null && !actualStart.isEmpty()) {
            task.setActualStart(LocalDate.parse(actualStart));
        }
        
        String actualEnd = rs.getString("actual_end");
        if (actualEnd != null && !actualEnd.isEmpty()) {
            task.setActualEnd(LocalDate.parse(actualEnd));
        }
        
        // Handle nullable Double values for SQLite compatibility
        double estimatedHours = rs.getDouble("estimated_hours");
        task.setEstimatedHours(rs.wasNull() ? null : estimatedHours);
        
        double actualHours = rs.getDouble("actual_hours");
        task.setActualHours(rs.wasNull() ? null : actualHours);
        
        // Handle nullable Long values for SQLite compatibility
        long assignedTo = rs.getLong("assigned_to");
        task.setAssignedTo(rs.wasNull() ? null : assignedTo);
        
        long assignedBy = rs.getLong("assigned_by");
        task.setAssignedBy(rs.wasNull() ? null : assignedBy);
        
        long reviewerId = rs.getLong("reviewer_id");
        task.setReviewerId(rs.wasNull() ? null : reviewerId);
        
        task.setLocation(rs.getString("location"));
        task.setEquipmentRequired(rs.getString("equipment_required"));
        task.setSafetyRequirements(rs.getString("safety_requirements"));
        task.setSiteAccessNotes(rs.getString("site_access_notes"));
        
        String riskLevel = rs.getString("risk_level");
        if (riskLevel != null) {
            task.setRiskLevel(Task.RiskLevel.valueOf(riskLevel));
        }
        
        task.setRiskNotes(rs.getString("risk_notes"));
        
        // Try to get additional fields if they exist
        try {
            task.setAssignedToName(rs.getString("assigned_to_name"));
        } catch (SQLException ignored) {}
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            task.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            task.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            task.setCompletedAt(completedAt.toLocalDateTime());
        }
        
        return task;
    }
    
    // Task Dependency Methods
    public TaskDependency createDependency(TaskDependency dependency) {
        String sql = """
            INSERT INTO task_dependencies (
                predecessor_id, successor_id, dependency_type, lag_days
            ) VALUES (?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, dependency.getPredecessorId());
            stmt.setLong(2, dependency.getSuccessorId());
            stmt.setString(3, dependency.getDependencyType().name());
            stmt.setInt(4, dependency.getLagDays() != null ? dependency.getLagDays() : 0);
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    dependency.setId(rs.getLong(1));
                }
            }
            
            return dependency;
        } catch (SQLException e) {
            logger.error("Error creating task dependency", e);
            throw new RuntimeException("Failed to create task dependency", e);
        }
    }
    
    public List<TaskDependency> findDependenciesBySuccessor(Long successorId) {
        String sql = """
            SELECT id, predecessor_id, successor_id, dependency_type, lag_days, created_at
            FROM task_dependencies
            WHERE successor_id = ?
            """;
        
        List<TaskDependency> dependencies = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, successorId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TaskDependency dep = new TaskDependency();
                    dep.setId(rs.getLong("id"));
                    dep.setPredecessorId(rs.getLong("predecessor_id"));
                    dep.setSuccessorId(rs.getLong("successor_id"));
                    
                    String depType = rs.getString("dependency_type");
                    if (depType != null) {
                        dep.setDependencyType(TaskDependency.DependencyType.valueOf(depType));
                    }
                    
                    dep.setLagDays(rs.getInt("lag_days"));
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        dep.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    
                    dependencies.add(dep);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding dependencies for successor: {}", successorId, e);
        }
        
        return dependencies;
    }
    
    public List<TaskDependency> findDependenciesByPredecessor(Long predecessorId) {
        String sql = """
            SELECT id, predecessor_id, successor_id, dependency_type, lag_days, created_at
            FROM task_dependencies
            WHERE predecessor_id = ?
            """;
        
        List<TaskDependency> dependencies = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, predecessorId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TaskDependency dep = new TaskDependency();
                    dep.setId(rs.getLong("id"));
                    dep.setPredecessorId(rs.getLong("predecessor_id"));
                    dep.setSuccessorId(rs.getLong("successor_id"));
                    
                    String depType = rs.getString("dependency_type");
                    if (depType != null) {
                        dep.setDependencyType(TaskDependency.DependencyType.valueOf(depType));
                    }
                    
                    dep.setLagDays(rs.getInt("lag_days"));
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        dep.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    
                    dependencies.add(dep);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding dependencies for predecessor: {}", predecessorId, e);
        }
        
        return dependencies;
    }
    
    public void deleteDependency(Long dependencyId) {
        String sql = "DELETE FROM task_dependencies WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, dependencyId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Error deleting task dependency: {}", dependencyId, e);
            throw new RuntimeException("Failed to delete task dependency", e);
        }
    }
    
    // Generate unique task code
    public String generateTaskCode(Long projectId) {
        String sql = "SELECT COUNT(*) + 1 FROM tasks WHERE project_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, projectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int taskNumber = rs.getInt(1);
                    return String.format("T-%d-%03d", projectId, taskNumber);
                }
            }
        } catch (SQLException e) {
            logger.error("Error generating task code", e);
        }
        
        return "T-" + projectId + "-001";
    }
}