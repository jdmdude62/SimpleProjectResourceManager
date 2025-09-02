package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssignmentRepository implements BaseRepository<Assignment, Long> {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentRepository.class);
    private final DataSource dataSource;

    public AssignmentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Assignment save(Assignment assignment) {
        String sql = """
            INSERT INTO assignments (project_id, resource_id, start_date, end_date, 
                                   travel_out_days, travel_back_days, is_override, 
                                   override_reason, notes, location, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, assignment.getProjectId());
            stmt.setLong(2, assignment.getResourceId());
            stmt.setString(3, assignment.getStartDate().toString() + " 00:00:00.000");
            stmt.setString(4, assignment.getEndDate().toString() + " 00:00:00.000");
            stmt.setInt(5, assignment.getTravelOutDays());
            stmt.setInt(6, assignment.getTravelBackDays());
            stmt.setBoolean(7, assignment.isOverride());
            stmt.setString(8, assignment.getOverrideReason());
            stmt.setString(9, assignment.getNotes());
            stmt.setString(10, assignment.getLocation());
            stmt.setTimestamp(11, Timestamp.valueOf(assignment.getCreatedAt()));
            stmt.setTimestamp(12, Timestamp.valueOf(assignment.getUpdatedAt()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating assignment failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    assignment.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating assignment failed, no ID obtained.");
                }
            }
            
            logger.info("Created assignment: project={}, resource={}", 
                assignment.getProjectId(), assignment.getResourceId());
            return assignment;
            
        } catch (SQLException e) {
            logger.error("Failed to save assignment: project={}, resource={}", 
                assignment.getProjectId(), assignment.getResourceId(), e);
            throw new RuntimeException("Failed to save assignment", e);
        }
    }

    @Override
    public void update(Assignment assignment) {
        String sql = """
            UPDATE assignments 
            SET project_id = ?, resource_id = ?, start_date = ?, end_date = ?, 
                travel_out_days = ?, travel_back_days = ?, is_override = ?, 
                override_reason = ?, notes = ?, location = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            assignment.setUpdatedAt(LocalDateTime.now());
            
            stmt.setLong(1, assignment.getProjectId());
            stmt.setLong(2, assignment.getResourceId());
            stmt.setString(3, assignment.getStartDate().toString() + " 00:00:00.000");
            stmt.setString(4, assignment.getEndDate().toString() + " 00:00:00.000");
            stmt.setInt(5, assignment.getTravelOutDays());
            stmt.setInt(6, assignment.getTravelBackDays());
            stmt.setBoolean(7, assignment.isOverride());
            stmt.setString(8, assignment.getOverrideReason());
            stmt.setString(9, assignment.getNotes());
            stmt.setString(10, assignment.getLocation());
            stmt.setTimestamp(11, Timestamp.valueOf(assignment.getUpdatedAt()));
            stmt.setLong(12, assignment.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating assignment failed, assignment not found: " + assignment.getId());
            }
            
            logger.info("Updated assignment: {}", assignment.getId());
            
        } catch (SQLException e) {
            logger.error("Failed to update assignment: {}", assignment.getId(), e);
            throw new RuntimeException("Failed to update assignment", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM assignments WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting assignment failed, assignment not found: " + id);
            }
            
            logger.info("Deleted assignment with ID: {}", id);
            
        } catch (SQLException e) {
            logger.error("Failed to delete assignment: {}", id, e);
            throw new RuntimeException("Failed to delete assignment", e);
        }
    }

    @Override
    public Optional<Assignment> findById(Long id) {
        String sql = "SELECT * FROM assignments WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAssignment(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            logger.error("Failed to find assignment by ID: {}", id, e);
            throw new RuntimeException("Failed to find assignment", e);
        }
    }

    @Override
    public List<Assignment> findAll() {
        String sql = "SELECT * FROM assignments ORDER BY start_date ASC";
        return executeQuery(sql);
    }

    public List<Assignment> findByProjectId(Long projectId) {
        String sql = "SELECT * FROM assignments WHERE project_id = ? ORDER BY start_date ASC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, projectId);
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find assignments by project ID: {}", projectId, e);
            throw new RuntimeException("Failed to find assignments", e);
        }
    }

    public List<Assignment> findByResourceId(Long resourceId) {
        String sql = "SELECT * FROM assignments WHERE resource_id = ? ORDER BY start_date ASC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, resourceId);
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find assignments by resource ID: {}", resourceId, e);
            throw new RuntimeException("Failed to find assignments", e);
        }
    }

    public List<Assignment> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM assignments 
            WHERE date(start_date) <= date(?) AND date(end_date) >= date(?)
            ORDER BY start_date ASC
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, endDate.toString());
            stmt.setString(2, startDate.toString());
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find assignments by date range: {} to {}", startDate, endDate, e);
            throw new RuntimeException("Failed to find assignments", e);
        }
    }

    public List<Assignment> findOverlappingAssignments(Long resourceId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM assignments 
            WHERE resource_id = ? 
            AND ((date(start_date) <= date(?) AND date(end_date) >= date(?)) 
                 OR (date(start_date) <= date(?) AND date(end_date) >= date(?))
                 OR (date(start_date) >= date(?) AND date(start_date) <= date(?)))
            ORDER BY start_date ASC
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, resourceId);
            stmt.setString(2, endDate.toString());
            stmt.setString(3, startDate.toString());
            stmt.setString(4, startDate.toString());
            stmt.setString(5, startDate.toString());
            stmt.setString(6, startDate.toString());
            stmt.setString(7, endDate.toString());
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find overlapping assignments for resource: {}", resourceId, e);
            throw new RuntimeException("Failed to find overlapping assignments", e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM assignments WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            logger.error("Failed to check if assignment exists: {}", id, e);
            throw new RuntimeException("Failed to check assignment existence", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM assignments";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
            
        } catch (SQLException e) {
            logger.error("Failed to count assignments", e);
            throw new RuntimeException("Failed to count assignments", e);
        }
    }

    private List<Assignment> executeQuery(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            List<Assignment> assignments = new ArrayList<>();
            while (rs.next()) {
                assignments.add(mapResultSetToAssignment(rs));
            }
            return assignments;
            
        } catch (SQLException e) {
            logger.error("Failed to execute query: {}", sql, e);
            throw new RuntimeException("Failed to execute query", e);
        }
    }

    private List<Assignment> executeQuery(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Assignment> assignments = new ArrayList<>();
            while (rs.next()) {
                assignments.add(mapResultSetToAssignment(rs));
            }
            return assignments;
        }
    }

    private Assignment mapResultSetToAssignment(ResultSet rs) throws SQLException {
        Assignment assignment = new Assignment();
        assignment.setId(rs.getLong("id"));
        assignment.setProjectId(rs.getLong("project_id"));
        assignment.setResourceId(rs.getLong("resource_id"));
        assignment.setStartDate(rs.getDate("start_date").toLocalDate());
        assignment.setEndDate(rs.getDate("end_date").toLocalDate());
        assignment.setTravelOutDays(rs.getInt("travel_out_days"));
        assignment.setTravelBackDays(rs.getInt("travel_back_days"));
        assignment.setOverride(rs.getBoolean("is_override"));
        assignment.setOverrideReason(rs.getString("override_reason"));
        assignment.setNotes(rs.getString("notes"));
        
        // Try to read location field - may not exist in older databases
        try {
            assignment.setLocation(rs.getString("location"));
        } catch (SQLException e) {
            // Column doesn't exist yet, ignore
        }
        
        assignment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        assignment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return assignment;
    }
}