package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.ResourceCertification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceCertificationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ResourceCertificationRepository.class);
    private final DataSource dataSource;

    public ResourceCertificationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS resource_certifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                resource_id INTEGER NOT NULL,
                certification_id INTEGER NOT NULL,
                date_obtained DATE,
                expiry_date DATE,
                certification_number TEXT,
                proficiency_score INTEGER CHECK (proficiency_score >= 1 AND proficiency_score <= 5),
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE,
                FOREIGN KEY (certification_id) REFERENCES certifications(id) ON DELETE CASCADE,
                UNIQUE(resource_id, certification_id)
            )
        """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.debug("Resource certifications table created or verified");
        } catch (SQLException e) {
            logger.error("Failed to create resource_certifications table", e);
            throw new RuntimeException("Failed to create resource_certifications table", e);
        }
    }

    public ResourceCertification save(ResourceCertification rc) {
        if (rc.getId() == null) {
            return insert(rc);
        } else {
            return update(rc);
        }
    }

    private ResourceCertification insert(ResourceCertification rc) {
        String sql = """
            INSERT INTO resource_certifications (resource_id, certification_id, date_obtained,
                expiry_date, certification_number, proficiency_score, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(1, rc.getResourceId());
            pstmt.setLong(2, rc.getCertificationId());
            pstmt.setDate(3, rc.getDateObtained() != null ? Date.valueOf(rc.getDateObtained()) : null);
            pstmt.setDate(4, rc.getExpiryDate() != null ? Date.valueOf(rc.getExpiryDate()) : null);
            pstmt.setString(5, rc.getCertificationNumber());
            
            if (rc.getProficiencyScore() != null) {
                pstmt.setInt(6, rc.getProficiencyScore());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }
            
            pstmt.setString(7, rc.getNotes());
            pstmt.setTimestamp(8, Timestamp.valueOf(rc.getCreatedAt()));
            pstmt.setTimestamp(9, Timestamp.valueOf(rc.getUpdatedAt()));

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    rc.setId(rs.getLong(1));
                }
            }

            logger.debug("Inserted resource certification for resource: {}", rc.getResourceId());
            return rc;
        } catch (SQLException e) {
            logger.error("Failed to insert resource certification", e);
            throw new RuntimeException("Failed to insert resource certification", e);
        }
    }

    private ResourceCertification update(ResourceCertification rc) {
        String sql = """
            UPDATE resource_certifications 
            SET resource_id = ?, certification_id = ?, date_obtained = ?,
                expiry_date = ?, certification_number = ?, proficiency_score = ?, 
                notes = ?, updated_at = ?
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, rc.getResourceId());
            pstmt.setLong(2, rc.getCertificationId());
            pstmt.setDate(3, rc.getDateObtained() != null ? Date.valueOf(rc.getDateObtained()) : null);
            pstmt.setDate(4, rc.getExpiryDate() != null ? Date.valueOf(rc.getExpiryDate()) : null);
            pstmt.setString(5, rc.getCertificationNumber());
            
            if (rc.getProficiencyScore() != null) {
                pstmt.setInt(6, rc.getProficiencyScore());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }
            
            pstmt.setString(7, rc.getNotes());
            pstmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(9, rc.getId());

            pstmt.executeUpdate();
            logger.debug("Updated resource certification: {}", rc.getId());
            return rc;
        } catch (SQLException e) {
            logger.error("Failed to update resource certification", e);
            throw new RuntimeException("Failed to update resource certification", e);
        }
    }

    public Optional<ResourceCertification> findById(Long id) {
        String sql = "SELECT * FROM resource_certifications WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResourceCertification(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find resource certification by id", e);
            throw new RuntimeException("Failed to find resource certification by id", e);
        }
    }

    public List<ResourceCertification> findByResourceId(Long resourceId) {
        String sql = "SELECT * FROM resource_certifications WHERE resource_id = ? ORDER BY date_obtained DESC";
        List<ResourceCertification> certifications = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, resourceId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    certifications.add(mapResultSetToResourceCertification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find certifications by resource id", e);
            throw new RuntimeException("Failed to find certifications by resource id", e);
        }

        return certifications;
    }

    public List<ResourceCertification> findByCertificationId(Long certificationId) {
        String sql = "SELECT * FROM resource_certifications WHERE certification_id = ?";
        List<ResourceCertification> resources = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, certificationId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapResultSetToResourceCertification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find resources by certification id", e);
            throw new RuntimeException("Failed to find resources by certification id", e);
        }

        return resources;
    }

    public List<ResourceCertification> findExpiring(int daysAhead) {
        String sql = """
            SELECT * FROM resource_certifications 
            WHERE expiry_date IS NOT NULL 
            AND expiry_date BETWEEN date('now') AND date('now', '+' || ? || ' days')
            ORDER BY expiry_date
        """;
        List<ResourceCertification> expiring = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, daysAhead);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    expiring.add(mapResultSetToResourceCertification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find expiring certifications", e);
            throw new RuntimeException("Failed to find expiring certifications", e);
        }

        return expiring;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM resource_certifications WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.debug("Deleted resource certification with id: {}", id);
        } catch (SQLException e) {
            logger.error("Failed to delete resource certification", e);
            throw new RuntimeException("Failed to delete resource certification", e);
        }
    }

    private ResourceCertification mapResultSetToResourceCertification(ResultSet rs) throws SQLException {
        ResourceCertification rc = new ResourceCertification();
        rc.setId(rs.getLong("id"));
        rc.setResourceId(rs.getLong("resource_id"));
        rc.setCertificationId(rs.getLong("certification_id"));
        
        Date dateObtained = rs.getDate("date_obtained");
        if (dateObtained != null) {
            rc.setDateObtained(dateObtained.toLocalDate());
        }
        
        Date expiryDate = rs.getDate("expiry_date");
        if (expiryDate != null) {
            rc.setExpiryDate(expiryDate.toLocalDate());
        }
        
        rc.setCertificationNumber(rs.getString("certification_number"));
        
        int profScore = rs.getInt("proficiency_score");
        if (!rs.wasNull()) {
            rc.setProficiencyScore(profScore);
        }
        
        rc.setNotes(rs.getString("notes"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            rc.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            rc.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return rc;
    }
}