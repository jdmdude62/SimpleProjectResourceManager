package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.TechnicianUnavailability;
import com.subliminalsearch.simpleprojectresourcemanager.model.UnavailabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceUnavailabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(ResourceUnavailabilityRepository.class);
    private final DataSource dataSource;
    
    public ResourceUnavailabilityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfNotExists();
    }
    
    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS resource_unavailability (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                resource_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                start_date DATE NOT NULL,
                end_date DATE NOT NULL,
                reason TEXT,
                description TEXT,
                approved BOOLEAN DEFAULT 0,
                approved_by TEXT,
                approved_at TIMESTAMP,
                is_recurring BOOLEAN DEFAULT 0,
                recurrence_pattern TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (resource_id) REFERENCES resources (id)
            )
            """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Resource unavailability table ready");
        } catch (SQLException e) {
            logger.error("Failed to create resource_unavailability table", e);
            throw new RuntimeException("Failed to create resource_unavailability table", e);
        }
    }
    
    public TechnicianUnavailability save(TechnicianUnavailability unavailability) {
        String sql = """
            INSERT INTO resource_unavailability 
            (resource_id, type, start_date, end_date, reason, description, 
             approved, approved_by, approved_at, is_recurring, recurrence_pattern, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, unavailability.getResourceId());
            stmt.setString(2, unavailability.getType().name());
            stmt.setString(3, unavailability.getStartDate().toString());
            stmt.setString(4, unavailability.getEndDate().toString());
            stmt.setString(5, unavailability.getReason());
            stmt.setString(6, unavailability.getDescription());
            stmt.setBoolean(7, unavailability.isApproved());
            stmt.setString(8, unavailability.getApprovedBy());
            stmt.setTimestamp(9, unavailability.getApprovedAt() != null ? 
                Timestamp.valueOf(unavailability.getApprovedAt()) : null);
            stmt.setBoolean(10, unavailability.isRecurring());
            stmt.setString(11, unavailability.getRecurrencePattern());
            stmt.setTimestamp(12, Timestamp.valueOf(unavailability.getCreatedAt()));
            stmt.setTimestamp(13, Timestamp.valueOf(unavailability.getUpdatedAt()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating unavailability failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    unavailability.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating unavailability failed, no ID obtained.");
                }
            }
            
            logger.info("Created unavailability for resource {}: {} from {} to {}", 
                unavailability.getResourceId(), unavailability.getType(), 
                unavailability.getStartDate(), unavailability.getEndDate());
            
            return unavailability;
            
        } catch (SQLException e) {
            logger.error("Failed to save unavailability", e);
            throw new RuntimeException("Failed to save unavailability", e);
        }
    }
    
    public void update(TechnicianUnavailability unavailability) {
        String sql = """
            UPDATE resource_unavailability 
            SET resource_id = ?, type = ?, start_date = ?, end_date = ?, 
                reason = ?, description = ?, approved = ?, approved_by = ?, 
                approved_at = ?, is_recurring = ?, recurrence_pattern = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            unavailability.setUpdatedAt(LocalDateTime.now());
            
            stmt.setLong(1, unavailability.getResourceId());
            stmt.setString(2, unavailability.getType().name());
            stmt.setString(3, unavailability.getStartDate().toString());
            stmt.setString(4, unavailability.getEndDate().toString());
            stmt.setString(5, unavailability.getReason());
            stmt.setString(6, unavailability.getDescription());
            stmt.setBoolean(7, unavailability.isApproved());
            stmt.setString(8, unavailability.getApprovedBy());
            stmt.setTimestamp(9, unavailability.getApprovedAt() != null ? 
                Timestamp.valueOf(unavailability.getApprovedAt()) : null);
            stmt.setBoolean(10, unavailability.isRecurring());
            stmt.setString(11, unavailability.getRecurrencePattern());
            stmt.setTimestamp(12, Timestamp.valueOf(unavailability.getUpdatedAt()));
            stmt.setLong(13, unavailability.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating unavailability failed, unavailability not found: " + unavailability.getId());
            }
            
            logger.info("Updated unavailability: {}", unavailability.getId());
            
        } catch (SQLException e) {
            logger.error("Failed to update unavailability: {}", unavailability.getId(), e);
            throw new RuntimeException("Failed to update unavailability", e);
        }
    }
    
    public void delete(Long id) {
        String sql = "DELETE FROM resource_unavailability WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting unavailability failed, unavailability not found: " + id);
            }
            
            logger.info("Deleted unavailability with ID: {}", id);
            
        } catch (SQLException e) {
            logger.error("Failed to delete unavailability: {}", id, e);
            throw new RuntimeException("Failed to delete unavailability", e);
        }
    }
    
    public Optional<TechnicianUnavailability> findById(Long id) {
        String sql = "SELECT * FROM resource_unavailability WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
            
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Failed to find unavailability by id: {}", id, e);
            throw new RuntimeException("Failed to find unavailability", e);
        }
    }
    
    public List<TechnicianUnavailability> findByResourceId(Long resourceId) {
        String sql = "SELECT * FROM resource_unavailability WHERE resource_id = ? ORDER BY start_date";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, resourceId);
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find unavailabilities for resource: {}", resourceId, e);
            throw new RuntimeException("Failed to find unavailabilities", e);
        }
    }
    
    public List<TechnicianUnavailability> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM resource_unavailability 
            WHERE date(start_date) <= date(?) AND date(end_date) >= date(?)
            ORDER BY start_date
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, endDate.toString());
            stmt.setString(2, startDate.toString());
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find unavailabilities by date range: {} to {}", startDate, endDate, e);
            throw new RuntimeException("Failed to find unavailabilities", e);
        }
    }
    
    public List<TechnicianUnavailability> findOverlapping(Long resourceId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM resource_unavailability 
            WHERE resource_id = ? 
            AND date(start_date) <= date(?) 
            AND date(end_date) >= date(?)
            ORDER BY start_date
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, resourceId);
            stmt.setString(2, endDate.toString());
            stmt.setString(3, startDate.toString());
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find overlapping unavailabilities for resource: {}", resourceId, e);
            throw new RuntimeException("Failed to find overlapping unavailabilities", e);
        }
    }
    
    public List<TechnicianUnavailability> findApproved() {
        String sql = "SELECT * FROM resource_unavailability WHERE approved = 1 ORDER BY start_date";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find approved unavailabilities", e);
            throw new RuntimeException("Failed to find approved unavailabilities", e);
        }
    }
    
    public List<TechnicianUnavailability> findPendingApproval() {
        String sql = "SELECT * FROM resource_unavailability WHERE approved = 0 ORDER BY created_at DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find pending unavailabilities", e);
            throw new RuntimeException("Failed to find pending unavailabilities", e);
        }
    }
    
    public List<TechnicianUnavailability> findAll() {
        String sql = "SELECT * FROM resource_unavailability ORDER BY start_date";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find all unavailabilities", e);
            throw new RuntimeException("Failed to find all unavailabilities", e);
        }
    }
    
    public void approveUnavailability(Long id, String approvedBy) {
        String sql = """
            UPDATE resource_unavailability 
            SET approved = 1, approved_by = ?, approved_at = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            LocalDateTime now = LocalDateTime.now();
            stmt.setString(1, approvedBy);
            stmt.setTimestamp(2, Timestamp.valueOf(now));
            stmt.setTimestamp(3, Timestamp.valueOf(now));
            stmt.setLong(4, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Approving unavailability failed, unavailability not found: " + id);
            }
            
            logger.info("Approved unavailability {} by {}", id, approvedBy);
            
        } catch (SQLException e) {
            logger.error("Failed to approve unavailability: {}", id, e);
            throw new RuntimeException("Failed to approve unavailability", e);
        }
    }
    
    private List<TechnicianUnavailability> executeQuery(PreparedStatement stmt) throws SQLException {
        List<TechnicianUnavailability> results = new ArrayList<>();
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.add(mapResultSet(rs));
            }
        }
        
        return results;
    }
    
    private TechnicianUnavailability mapResultSet(ResultSet rs) throws SQLException {
        TechnicianUnavailability unavailability = new TechnicianUnavailability();
        
        unavailability.setId(rs.getLong("id"));
        unavailability.setResourceId(rs.getLong("resource_id"));
        unavailability.setType(UnavailabilityType.valueOf(rs.getString("type")));
        unavailability.setStartDate(LocalDate.parse(rs.getString("start_date")));
        unavailability.setEndDate(LocalDate.parse(rs.getString("end_date")));
        unavailability.setReason(rs.getString("reason"));
        unavailability.setDescription(rs.getString("description"));
        unavailability.setApproved(rs.getBoolean("approved"));
        unavailability.setApprovedBy(rs.getString("approved_by"));
        
        Timestamp approvedAt = rs.getTimestamp("approved_at");
        if (approvedAt != null) {
            unavailability.setApprovedAt(approvedAt.toLocalDateTime());
        }
        
        unavailability.setRecurring(rs.getBoolean("is_recurring"));
        unavailability.setRecurrencePattern(rs.getString("recurrence_pattern"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            unavailability.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            unavailability.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return unavailability;
    }
}