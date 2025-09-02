package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.Certification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CertificationRepository {
    private static final Logger logger = LoggerFactory.getLogger(CertificationRepository.class);
    private final DataSource dataSource;

    public CertificationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS certifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                issuing_organization TEXT,
                validity_period_months INTEGER,
                is_active BOOLEAN DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.debug("Certifications table created or verified");
        } catch (SQLException e) {
            logger.error("Failed to create certifications table", e);
            throw new RuntimeException("Failed to create certifications table", e);
        }
    }

    public Certification save(Certification certification) {
        if (certification.getId() == null) {
            return insert(certification);
        } else {
            return update(certification);
        }
    }

    private Certification insert(Certification certification) {
        String sql = """
            INSERT INTO certifications (name, description, issuing_organization, 
                validity_period_months, is_active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, certification.getName());
            pstmt.setString(2, certification.getDescription());
            pstmt.setString(3, certification.getIssuingOrganization());
            
            // Handle NULL validity period correctly
            if (certification.getValidityPeriodMonths() != null) {
                pstmt.setInt(4, certification.getValidityPeriodMonths());
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }
            
            pstmt.setBoolean(5, certification.isActive());
            pstmt.setTimestamp(6, Timestamp.valueOf(certification.getCreatedAt()));
            pstmt.setTimestamp(7, Timestamp.valueOf(certification.getUpdatedAt()));

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    certification.setId(rs.getLong(1));
                }
            }

            logger.debug("Inserted certification: {}", certification.getName());
            return certification;
        } catch (SQLException e) {
            logger.error("Failed to insert certification", e);
            throw new RuntimeException("Failed to insert certification", e);
        }
    }

    private Certification update(Certification certification) {
        String sql = """
            UPDATE certifications 
            SET name = ?, description = ?, issuing_organization = ?,
                validity_period_months = ?, is_active = ?, updated_at = ?
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, certification.getName());
            pstmt.setString(2, certification.getDescription());
            pstmt.setString(3, certification.getIssuingOrganization());
            
            // Handle NULL validity period correctly
            if (certification.getValidityPeriodMonths() != null) {
                pstmt.setInt(4, certification.getValidityPeriodMonths());
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }
            
            pstmt.setBoolean(5, certification.isActive());
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(7, certification.getId());

            pstmt.executeUpdate();
            logger.debug("Updated certification: {}", certification.getName());
            return certification;
        } catch (SQLException e) {
            logger.error("Failed to update certification", e);
            throw new RuntimeException("Failed to update certification", e);
        }
    }

    public Optional<Certification> findById(Long id) {
        String sql = "SELECT * FROM certifications WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCertification(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find certification by id", e);
            throw new RuntimeException("Failed to find certification by id", e);
        }
    }

    public Optional<Certification> findByName(String name) {
        String sql = "SELECT * FROM certifications WHERE name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCertification(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find certification by name", e);
            throw new RuntimeException("Failed to find certification by name", e);
        }
    }

    public List<Certification> findAll() {
        String sql = "SELECT * FROM certifications ORDER BY name";
        List<Certification> certifications = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                certifications.add(mapResultSetToCertification(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all certifications", e);
            throw new RuntimeException("Failed to find all certifications", e);
        }

        return certifications;
    }

    public List<Certification> findActive() {
        String sql = "SELECT * FROM certifications WHERE is_active = 1 ORDER BY name";
        List<Certification> certifications = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                certifications.add(mapResultSetToCertification(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find active certifications", e);
            throw new RuntimeException("Failed to find active certifications", e);
        }

        return certifications;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM certifications WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.debug("Deleted certification with id: {}", id);
        } catch (SQLException e) {
            logger.error("Failed to delete certification", e);
            throw new RuntimeException("Failed to delete certification", e);
        }
    }

    private Certification mapResultSetToCertification(ResultSet rs) throws SQLException {
        Certification certification = new Certification();
        certification.setId(rs.getLong("id"));
        certification.setName(rs.getString("name"));
        certification.setDescription(rs.getString("description"));
        certification.setIssuingOrganization(rs.getString("issuing_organization"));
        
        // Handle validity_period_months which might be NULL
        int validityMonths = rs.getInt("validity_period_months");
        if (!rs.wasNull()) {
            certification.setValidityPeriodMonths(validityMonths);
        } else {
            certification.setValidityPeriodMonths(null);
        }
        
        certification.setActive(rs.getBoolean("is_active"));
        
        // Handle timestamps which might be null
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            certification.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            certification.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return certification;
    }
}