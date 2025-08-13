package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectRepository implements BaseRepository<Project, Long> {
    private static final Logger logger = LoggerFactory.getLogger(ProjectRepository.class);
    private final DataSource dataSource;

    public ProjectRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Project save(Project project) {
        String sql = """
            INSERT INTO projects (project_id, description, project_manager_id, start_date, end_date, status,
                                contact_name, contact_email, contact_phone, contact_company, contact_role,
                                send_reports, report_frequency, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, project.getProjectId());
            stmt.setString(2, project.getDescription());
            if (project.getProjectManagerId() != null) {
                stmt.setLong(3, project.getProjectManagerId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, project.getStartDate().toString());
            stmt.setString(5, project.getEndDate().toString());
            stmt.setString(6, project.getStatus().name());
            
            // Client contact fields
            stmt.setString(7, project.getContactName());
            stmt.setString(8, project.getContactEmail());
            stmt.setString(9, project.getContactPhone());
            stmt.setString(10, project.getContactCompany());
            stmt.setString(11, project.getContactRole());
            stmt.setInt(12, project.isSendReports() ? 1 : 0);
            stmt.setString(13, project.getReportFrequency());
            
            stmt.setTimestamp(14, Timestamp.valueOf(project.getCreatedAt()));
            stmt.setTimestamp(15, Timestamp.valueOf(project.getUpdatedAt()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating project failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    project.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating project failed, no ID obtained.");
                }
            }
            
            logger.info("Created project: {}", project.getProjectId());
            return project;
            
        } catch (SQLException e) {
            logger.error("Failed to save project: {}", project.getProjectId(), e);
            throw new RuntimeException("Failed to save project", e);
        }
    }

    @Override
    public void update(Project project) {
        String sql = """
            UPDATE projects 
            SET description = ?, project_manager_id = ?, start_date = ?, end_date = ?, status = ?,
                contact_name = ?, contact_email = ?, contact_phone = ?, contact_company = ?, contact_role = ?,
                send_reports = ?, report_frequency = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            project.setUpdatedAt(LocalDateTime.now());
            
            stmt.setString(1, project.getDescription());
            if (project.getProjectManagerId() != null) {
                stmt.setLong(2, project.getProjectManagerId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, project.getStartDate().toString());
            stmt.setString(4, project.getEndDate().toString());
            stmt.setString(5, project.getStatus().name());
            
            // Client contact fields
            stmt.setString(6, project.getContactName());
            stmt.setString(7, project.getContactEmail());
            stmt.setString(8, project.getContactPhone());
            stmt.setString(9, project.getContactCompany());
            stmt.setString(10, project.getContactRole());
            stmt.setInt(11, project.isSendReports() ? 1 : 0);
            stmt.setString(12, project.getReportFrequency());
            
            stmt.setTimestamp(13, Timestamp.valueOf(project.getUpdatedAt()));
            stmt.setLong(14, project.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating project failed, project not found: " + project.getId());
            }
            
            logger.info("Updated project: {}", project.getProjectId());
            
        } catch (SQLException e) {
            logger.error("Failed to update project: {}", project.getId(), e);
            throw new RuntimeException("Failed to update project", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM projects WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting project failed, project not found: " + id);
            }
            
            logger.info("Deleted project with ID: {}", id);
            
        } catch (SQLException e) {
            logger.error("Failed to delete project: {}", id, e);
            throw new RuntimeException("Failed to delete project", e);
        }
    }

    @Override
    public Optional<Project> findById(Long id) {
        String sql = "SELECT * FROM projects WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProject(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            logger.error("Failed to find project by ID: {}", id, e);
            throw new RuntimeException("Failed to find project", e);
        }
    }

    public Optional<Project> findByProjectId(String projectId) {
        String sql = "SELECT * FROM projects WHERE project_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProject(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            logger.error("Failed to find project by project ID: {}", projectId, e);
            throw new RuntimeException("Failed to find project", e);
        }
    }

    @Override
    public List<Project> findAll() {
        String sql = "SELECT * FROM projects ORDER BY start_date DESC";
        return executeQuery(sql);
    }

    public List<Project> findByStatus(ProjectStatus status) {
        String sql = "SELECT * FROM projects WHERE status = ? ORDER BY start_date DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find projects by status: {}", status, e);
            throw new RuntimeException("Failed to find projects", e);
        }
    }

    public List<Project> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM projects 
            WHERE date(start_date) <= date(?) AND date(end_date) >= date(?)
            ORDER BY start_date ASC
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, endDate.toString());
            stmt.setString(2, startDate.toString());
            return executeQuery(stmt);
            
        } catch (SQLException e) {
            logger.error("Failed to find projects by date range: {} to {}", startDate, endDate, e);
            throw new RuntimeException("Failed to find projects", e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT 1 FROM projects WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            logger.error("Failed to check if project exists: {}", id, e);
            throw new RuntimeException("Failed to check project existence", e);
        }
    }

    public boolean existsByProjectId(String projectId) {
        String sql = "SELECT 1 FROM projects WHERE project_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, projectId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            logger.error("Failed to check if project exists: {}", projectId, e);
            throw new RuntimeException("Failed to check project existence", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM projects";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
            
        } catch (SQLException e) {
            logger.error("Failed to count projects", e);
            throw new RuntimeException("Failed to count projects", e);
        }
    }
    
    public List<String> getAllProjectIds() {
        String sql = "SELECT project_id FROM projects ORDER BY project_id";
        List<String> projectIds = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                projectIds.add(rs.getString("project_id"));
            }
            
            return projectIds;
            
        } catch (SQLException e) {
            logger.error("Failed to get all project IDs", e);
            throw new RuntimeException("Failed to get project IDs", e);
        }
    }

    private List<Project> executeQuery(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            List<Project> projects = new ArrayList<>();
            while (rs.next()) {
                projects.add(mapResultSetToProject(rs));
            }
            return projects;
            
        } catch (SQLException e) {
            logger.error("Failed to execute query: {}", sql, e);
            throw new RuntimeException("Failed to execute query", e);
        }
    }

    private List<Project> executeQuery(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Project> projects = new ArrayList<>();
            while (rs.next()) {
                projects.add(mapResultSetToProject(rs));
            }
            return projects;
        }
    }

    private Project mapResultSetToProject(ResultSet rs) throws SQLException {
        Project project = new Project();
        project.setId(rs.getLong("id"));
        project.setProjectId(rs.getString("project_id"));
        project.setDescription(rs.getString("description"));
        
        // Handle nullable project_manager_id
        long pmId = rs.getLong("project_manager_id");
        if (!rs.wasNull()) {
            project.setProjectManagerId(pmId);
        }
        
        project.setStartDate(LocalDate.parse(rs.getString("start_date")));
        project.setEndDate(LocalDate.parse(rs.getString("end_date")));
        project.setStatus(ProjectStatus.valueOf(rs.getString("status")));
        
        // Client contact fields - handle nulls
        String contactName = rs.getString("contact_name");
        if (contactName != null) project.setContactName(contactName);
        
        String contactEmail = rs.getString("contact_email");
        if (contactEmail != null) project.setContactEmail(contactEmail);
        
        String contactPhone = rs.getString("contact_phone");
        if (contactPhone != null) project.setContactPhone(contactPhone);
        
        String contactCompany = rs.getString("contact_company");
        if (contactCompany != null) project.setContactCompany(contactCompany);
        
        String contactRole = rs.getString("contact_role");
        if (contactRole != null) project.setContactRole(contactRole);
        
        // Handle send_reports as integer (0/1)
        try {
            project.setSendReports(rs.getInt("send_reports") == 1);
        } catch (SQLException e) {
            // Column might not exist in older schemas
            project.setSendReports(true);
        }
        
        String reportFrequency = rs.getString("report_frequency");
        if (reportFrequency != null) project.setReportFrequency(reportFrequency);
        
        // Load financial fields
        try {
            Double budgetAmount = rs.getDouble("budget_amount");
            if (!rs.wasNull()) project.setBudgetAmount(budgetAmount);
            
            Double actualCost = rs.getDouble("actual_cost");
            if (!rs.wasNull()) project.setActualCost(actualCost);
            
            Double revenueAmount = rs.getDouble("revenue_amount");
            if (!rs.wasNull()) project.setRevenueAmount(revenueAmount);
            
            String currencyCode = rs.getString("currency_code");
            if (currencyCode != null) project.setCurrencyCode(currencyCode);
            
            Double laborCost = rs.getDouble("labor_cost");
            if (!rs.wasNull()) project.setLaborCost(laborCost);
            
            Double materialCost = rs.getDouble("material_cost");
            if (!rs.wasNull()) project.setMaterialCost(materialCost);
            
            Double travelCost = rs.getDouble("travel_cost");
            if (!rs.wasNull()) project.setTravelCost(travelCost);
            
            Double otherCost = rs.getDouble("other_cost");
            if (!rs.wasNull()) project.setOtherCost(otherCost);
            
            String costNotes = rs.getString("cost_notes");
            if (costNotes != null) project.setCostNotes(costNotes);
        } catch (SQLException e) {
            // Financial columns might not exist in older schemas
            logger.debug("Financial columns not found in result set", e);
        }
        
        project.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        project.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return project;
    }
}