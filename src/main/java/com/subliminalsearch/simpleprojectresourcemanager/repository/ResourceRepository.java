package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.ResourceCategory;
import com.subliminalsearch.simpleprojectresourcemanager.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceRepository implements BaseRepository<Resource, Long> {
    private static final Logger logger = LoggerFactory.getLogger(ResourceRepository.class);
    private final DataSource dataSource;

    public ResourceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Resource save(Resource resource) {
        String sql = """
            INSERT INTO resources (name, email, phone, resource_type_id, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, resource.getName());
            stmt.setString(2, resource.getEmail());
            stmt.setString(3, resource.getPhone());
            stmt.setObject(4, resource.getResourceType() != null ? resource.getResourceType().getId() : null);
            stmt.setBoolean(5, resource.isActive());
            stmt.setTimestamp(6, Timestamp.valueOf(resource.getCreatedAt()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating resource failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    resource.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating resource failed, no ID obtained.");
                }
            }
            
            logger.info("Created resource: {}", resource.getName());
            return resource;
            
        } catch (SQLException e) {
            logger.error("Failed to save resource: {}", resource.getName(), e);
            throw new RuntimeException("Failed to save resource", e);
        }
    }

    @Override
    public void update(Resource resource) {
        String sql = """
            UPDATE resources 
            SET name = ?, email = ?, phone = ?, resource_type_id = ?, is_active = ?, ldap_username = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, resource.getName());
            stmt.setString(2, resource.getEmail());
            stmt.setString(3, resource.getPhone());
            stmt.setObject(4, resource.getResourceType() != null ? resource.getResourceType().getId() : null);
            stmt.setBoolean(5, resource.isActive());
            stmt.setString(6, resource.getLdapUsername());
            stmt.setLong(7, resource.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating resource failed, resource not found: " + resource.getId());
            }
            
            logger.info("Updated resource: {}", resource.getName());
            
        } catch (SQLException e) {
            logger.error("Failed to update resource: {}", resource.getId(), e);
            throw new RuntimeException("Failed to update resource", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM resources WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting resource failed, resource not found: " + id);
            }
            
            logger.info("Deleted resource with ID: {}", id);
            
        } catch (SQLException e) {
            logger.error("Failed to delete resource: {}", id, e);
            throw new RuntimeException("Failed to delete resource", e);
        }
    }

    @Override
    public Optional<Resource> findById(Long id) {
        String sql = """
            SELECT r.*, rt.name as type_name, rt.category as type_category
            FROM resources r
            LEFT JOIN resource_types rt ON r.resource_type_id = rt.id
            WHERE r.id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResource(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            logger.error("Failed to find resource by ID: {}", id, e);
            throw new RuntimeException("Failed to find resource", e);
        }
    }

    @Override
    public List<Resource> findAll() {
        String sql = """
            SELECT r.*, rt.name as type_name, rt.category as type_category
            FROM resources r
            LEFT JOIN resource_types rt ON r.resource_type_id = rt.id
            ORDER BY r.name ASC
            """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            List<Resource> resources = new ArrayList<>();
            while (rs.next()) {
                resources.add(mapResultSetToResource(rs));
            }
            return resources;
            
        } catch (SQLException e) {
            logger.error("Failed to find all resources", e);
            throw new RuntimeException("Failed to find resources", e);
        }
    }

    public List<Resource> findByCategory(ResourceCategory category) {
        String sql = """
            SELECT r.*, rt.name as type_name, rt.category as type_category
            FROM resources r
            LEFT JOIN resource_types rt ON r.resource_type_id = rt.id
            WHERE rt.category = ?
            ORDER BY r.name ASC
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<Resource> resources = new ArrayList<>();
                while (rs.next()) {
                    resources.add(mapResultSetToResource(rs));
                }
                return resources;
            }
            
        } catch (SQLException e) {
            logger.error("Failed to find resources by category: {}", category, e);
            throw new RuntimeException("Failed to find resources", e);
        }
    }

    public List<Resource> findActiveResources() {
        String sql = """
            SELECT r.*, rt.name as type_name, rt.category as type_category
            FROM resources r
            LEFT JOIN resource_types rt ON r.resource_type_id = rt.id
            WHERE r.is_active = true
            ORDER BY r.name ASC
            """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            List<Resource> resources = new ArrayList<>();
            while (rs.next()) {
                resources.add(mapResultSetToResource(rs));
            }
            return resources;
            
        } catch (SQLException e) {
            logger.error("Failed to find active resources", e);
            throw new RuntimeException("Failed to find active resources", e);
        }
    }

    public Optional<Resource> findByEmail(String email) {
        String sql = """
            SELECT r.*, rt.name as type_name, rt.category as type_category
            FROM resources r
            LEFT JOIN resource_types rt ON r.resource_type_id = rt.id
            WHERE r.email = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResource(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            logger.error("Failed to find resource by email: {}", email, e);
            throw new RuntimeException("Failed to find resource", e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM resources WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            logger.error("Failed to check if resource exists: {}", id, e);
            throw new RuntimeException("Failed to check resource existence", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM resources";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
            
        } catch (SQLException e) {
            logger.error("Failed to count resources", e);
            throw new RuntimeException("Failed to count resources", e);
        }
    }

    private Resource mapResultSetToResource(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getLong("id"));
        resource.setName(rs.getString("name"));
        resource.setEmail(rs.getString("email"));
        resource.setPhone(rs.getString("phone"));
        resource.setActive(rs.getBoolean("is_active"));
        resource.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        // Map LDAP username if present
        try {
            resource.setLdapUsername(rs.getString("ldap_username"));
        } catch (SQLException e) {
            // Column might not exist in older databases
            logger.debug("ldap_username column not found, skipping");
        }
        
        // Map resource type if present
        String typeName = rs.getString("type_name");
        if (typeName != null) {
            ResourceType resourceType = new ResourceType();
            resourceType.setId(rs.getLong("resource_type_id"));
            resourceType.setName(typeName);
            resourceType.setCategory(ResourceCategory.valueOf(rs.getString("type_category")));
            resource.setResourceType(resourceType);
        }
        
        return resource;
    }
}