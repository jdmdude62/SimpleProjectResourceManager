package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectManagerRepository {
    private static final Logger logger = LoggerFactory.getLogger(ProjectManagerRepository.class);
    private final DataSource dataSource;

    public ProjectManagerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ProjectManager create(ProjectManager projectManager) {
        String sql = "INSERT INTO project_managers (name, email, phone, department, active) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, projectManager.getName());
            stmt.setString(2, projectManager.getEmail());
            stmt.setString(3, projectManager.getPhone());
            stmt.setString(4, projectManager.getDepartment());
            stmt.setBoolean(5, projectManager.isActive());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating project manager failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    projectManager.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating project manager failed, no ID obtained.");
                }
            }
            
            logger.debug("Created project manager: {}", projectManager);
            return projectManager;
            
        } catch (SQLException e) {
            logger.error("Error creating project manager", e);
            throw new RuntimeException("Failed to create project manager", e);
        }
    }

    public Optional<ProjectManager> findById(Long id) {
        String sql = "SELECT * FROM project_managers WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProjectManager(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding project manager by id: {}", id, e);
        }
        
        return Optional.empty();
    }

    public Optional<ProjectManager> findByName(String name) {
        String sql = "SELECT * FROM project_managers WHERE name = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProjectManager(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding project manager by name: {}", name, e);
        }
        
        return Optional.empty();
    }

    public List<ProjectManager> findAll() {
        List<ProjectManager> managers = new ArrayList<>();
        String sql = "SELECT * FROM project_managers ORDER BY name";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                managers.add(mapResultSetToProjectManager(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error finding all project managers", e);
        }
        
        return managers;
    }

    public List<ProjectManager> findAllActive() {
        List<ProjectManager> managers = new ArrayList<>();
        String sql = "SELECT * FROM project_managers WHERE active = 1 ORDER BY name";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                managers.add(mapResultSetToProjectManager(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error finding active project managers", e);
        }
        
        return managers;
    }

    public ProjectManager update(ProjectManager projectManager) {
        String sql = "UPDATE project_managers SET name = ?, email = ?, phone = ?, department = ?, active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectManager.getName());
            stmt.setString(2, projectManager.getEmail());
            stmt.setString(3, projectManager.getPhone());
            stmt.setString(4, projectManager.getDepartment());
            stmt.setBoolean(5, projectManager.isActive());
            stmt.setLong(6, projectManager.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Updating project manager failed, no rows affected.");
            }
            
            logger.debug("Updated project manager: {}", projectManager);
            return projectManager;
            
        } catch (SQLException e) {
            logger.error("Error updating project manager", e);
            throw new RuntimeException("Failed to update project manager", e);
        }
    }

    public void delete(Long id) {
        // Don't allow deletion of "Unassigned" PM
        Optional<ProjectManager> pm = findById(id);
        if (pm.isPresent() && "Unassigned".equals(pm.get().getName())) {
            throw new RuntimeException("Cannot delete the 'Unassigned' project manager");
        }
        
        String sql = "DELETE FROM project_managers WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                logger.warn("No project manager found with id: {}", id);
            } else {
                logger.debug("Deleted project manager with id: {}", id);
            }
            
        } catch (SQLException e) {
            logger.error("Error deleting project manager", e);
            throw new RuntimeException("Failed to delete project manager", e);
        }
    }

    private ProjectManager mapResultSetToProjectManager(ResultSet rs) throws SQLException {
        ProjectManager pm = new ProjectManager();
        pm.setId(rs.getLong("id"));
        pm.setName(rs.getString("name"));
        pm.setEmail(rs.getString("email"));
        pm.setPhone(rs.getString("phone"));
        pm.setDepartment(rs.getString("department"));
        pm.setActive(rs.getBoolean("active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            pm.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            pm.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return pm;
    }
}