package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.TaskDependency;
import com.subliminalsearch.simpleprojectresourcemanager.model.TaskDependency.DependencyType;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskDependencyRepository {
    
    private final DataSource dataSource;
    
    public TaskDependencyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfNotExists();
    }
    
    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS task_dependencies (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                predecessor_id INTEGER NOT NULL,
                successor_id INTEGER NOT NULL,
                dependency_type VARCHAR(20) NOT NULL DEFAULT 'FINISH_TO_START',
                lag_days INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (predecessor_id) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY (successor_id) REFERENCES tasks(id) ON DELETE CASCADE,
                UNIQUE(predecessor_id, successor_id)
            )
            """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create task_dependencies table", e);
        }
    }
    
    public TaskDependency save(TaskDependency dependency) {
        String sql = """
            INSERT INTO task_dependencies (predecessor_id, successor_id, dependency_type, lag_days)
            VALUES (?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, dependency.getPredecessorId());
            stmt.setLong(2, dependency.getSuccessorId());
            stmt.setString(3, dependency.getDependencyType().name());
            stmt.setInt(4, dependency.getLagDays() != null ? dependency.getLagDays() : 0);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating task dependency failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    dependency.setId(generatedKeys.getLong(1));
                    dependency.setCreatedAt(LocalDateTime.now());
                }
            }
            
            return dependency;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save task dependency", e);
        }
    }
    
    public void update(TaskDependency dependency) {
        String sql = """
            UPDATE task_dependencies
            SET dependency_type = ?, lag_days = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, dependency.getDependencyType().name());
            stmt.setInt(2, dependency.getLagDays() != null ? dependency.getLagDays() : 0);
            stmt.setLong(3, dependency.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update task dependency", e);
        }
    }
    
    public void delete(Long id) {
        String sql = "DELETE FROM task_dependencies WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete task dependency", e);
        }
    }
    
    public void deleteByTaskId(Long taskId) {
        String sql = "DELETE FROM task_dependencies WHERE predecessor_id = ? OR successor_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, taskId);
            stmt.setLong(2, taskId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete task dependencies", e);
        }
    }
    
    public List<TaskDependency> findByProjectId(Long projectId) {
        String sql = """
            SELECT td.*, 
                   t1.title as predecessor_title,
                   t2.title as successor_title
            FROM task_dependencies td
            JOIN tasks t1 ON td.predecessor_id = t1.id
            JOIN tasks t2 ON td.successor_id = t2.id
            WHERE t1.project_id = ? OR t2.project_id = ?
            """;
        
        List<TaskDependency> dependencies = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, projectId);
            stmt.setLong(2, projectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dependencies.add(mapResultSetToDependency(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find task dependencies", e);
        }
        
        return dependencies;
    }
    
    public List<TaskDependency> findPredecessors(Long taskId) {
        String sql = """
            SELECT td.*,
                   t.title as predecessor_title
            FROM task_dependencies td
            JOIN tasks t ON td.predecessor_id = t.id
            WHERE td.successor_id = ?
            """;
        
        List<TaskDependency> dependencies = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, taskId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TaskDependency dep = mapResultSetToDependency(rs);
                    dep.setPredecessorTitle(rs.getString("predecessor_title"));
                    dependencies.add(dep);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find predecessors", e);
        }
        
        return dependencies;
    }
    
    public List<TaskDependency> findSuccessors(Long taskId) {
        String sql = """
            SELECT td.*,
                   t.title as successor_title
            FROM task_dependencies td
            JOIN tasks t ON td.successor_id = t.id
            WHERE td.predecessor_id = ?
            """;
        
        List<TaskDependency> dependencies = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, taskId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TaskDependency dep = mapResultSetToDependency(rs);
                    dep.setSuccessorTitle(rs.getString("successor_title"));
                    dependencies.add(dep);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find successors", e);
        }
        
        return dependencies;
    }
    
    public boolean hasCyclicDependency(Long predecessorId, Long successorId) {
        // Check if adding this dependency would create a cycle
        // Start from successor and see if we can reach predecessor
        return canReachTask(successorId, predecessorId, new ArrayList<>());
    }
    
    private boolean canReachTask(Long fromTaskId, Long toTaskId, List<Long> visited) {
        if (fromTaskId.equals(toTaskId)) {
            return true;
        }
        
        if (visited.contains(fromTaskId)) {
            return false;
        }
        
        visited.add(fromTaskId);
        
        List<TaskDependency> successors = findSuccessors(fromTaskId);
        for (TaskDependency dep : successors) {
            if (canReachTask(dep.getSuccessorId(), toTaskId, visited)) {
                return true;
            }
        }
        
        return false;
    }
    
    private TaskDependency mapResultSetToDependency(ResultSet rs) throws SQLException {
        TaskDependency dependency = new TaskDependency();
        dependency.setId(rs.getLong("id"));
        dependency.setPredecessorId(rs.getLong("predecessor_id"));
        dependency.setSuccessorId(rs.getLong("successor_id"));
        
        String typeStr = rs.getString("dependency_type");
        dependency.setDependencyType(DependencyType.valueOf(typeStr));
        
        dependency.setLagDays(rs.getInt("lag_days"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            dependency.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        // Try to get titles if they exist in the result set
        try {
            dependency.setPredecessorTitle(rs.getString("predecessor_title"));
        } catch (SQLException ignored) {}
        
        try {
            dependency.setSuccessorTitle(rs.getString("successor_title"));
        } catch (SQLException ignored) {}
        
        return dependency;
    }
}