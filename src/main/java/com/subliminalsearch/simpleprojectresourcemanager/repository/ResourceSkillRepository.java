package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.ResourceSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceSkillRepository {
    private static final Logger logger = LoggerFactory.getLogger(ResourceSkillRepository.class);
    private final DataSource dataSource;

    public ResourceSkillRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS resource_skills (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                resource_id INTEGER NOT NULL,
                skill_id INTEGER NOT NULL,
                proficiency_level INTEGER CHECK (proficiency_level >= 1 AND proficiency_level <= 5),
                years_of_experience INTEGER,
                notes TEXT,
                last_used_date TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE,
                FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
                UNIQUE(resource_id, skill_id)
            )
        """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.debug("Resource skills table created or verified");
        } catch (SQLException e) {
            logger.error("Failed to create resource_skills table", e);
            throw new RuntimeException("Failed to create resource_skills table", e);
        }
    }

    public ResourceSkill save(ResourceSkill rs) {
        if (rs.getId() == null) {
            return insert(rs);
        } else {
            return update(rs);
        }
    }

    private ResourceSkill insert(ResourceSkill rs) {
        String sql = """
            INSERT INTO resource_skills (resource_id, skill_id, proficiency_level,
                years_of_experience, notes, last_used_date, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(1, rs.getResourceId());
            pstmt.setLong(2, rs.getSkillId());
            
            if (rs.getProficiencyLevel() != null) {
                pstmt.setInt(3, rs.getProficiencyLevel());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            
            if (rs.getYearsOfExperience() != null) {
                pstmt.setInt(4, rs.getYearsOfExperience());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            pstmt.setString(5, rs.getNotes());
            
            if (rs.getLastUsedDate() != null) {
                pstmt.setTimestamp(6, Timestamp.valueOf(rs.getLastUsedDate()));
            } else {
                pstmt.setNull(6, Types.TIMESTAMP);
            }
            
            pstmt.setTimestamp(7, Timestamp.valueOf(rs.getCreatedAt()));
            pstmt.setTimestamp(8, Timestamp.valueOf(rs.getUpdatedAt()));

            pstmt.executeUpdate();

            try (ResultSet resultSet = pstmt.getGeneratedKeys()) {
                if (resultSet.next()) {
                    rs.setId(resultSet.getLong(1));
                }
            }

            logger.debug("Inserted resource skill for resource: {}", rs.getResourceId());
            return rs;
        } catch (SQLException e) {
            logger.error("Failed to insert resource skill", e);
            throw new RuntimeException("Failed to insert resource skill", e);
        }
    }

    private ResourceSkill update(ResourceSkill rs) {
        String sql = """
            UPDATE resource_skills 
            SET resource_id = ?, skill_id = ?, proficiency_level = ?,
                years_of_experience = ?, notes = ?, last_used_date = ?, updated_at = ?
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, rs.getResourceId());
            pstmt.setLong(2, rs.getSkillId());
            
            if (rs.getProficiencyLevel() != null) {
                pstmt.setInt(3, rs.getProficiencyLevel());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            
            if (rs.getYearsOfExperience() != null) {
                pstmt.setInt(4, rs.getYearsOfExperience());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            pstmt.setString(5, rs.getNotes());
            
            if (rs.getLastUsedDate() != null) {
                pstmt.setTimestamp(6, Timestamp.valueOf(rs.getLastUsedDate()));
            } else {
                pstmt.setNull(6, Types.TIMESTAMP);
            }
            
            pstmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(8, rs.getId());

            pstmt.executeUpdate();
            logger.debug("Updated resource skill: {}", rs.getId());
            return rs;
        } catch (SQLException e) {
            logger.error("Failed to update resource skill", e);
            throw new RuntimeException("Failed to update resource skill", e);
        }
    }

    public Optional<ResourceSkill> findById(Long id) {
        String sql = "SELECT * FROM resource_skills WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResourceSkill(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find resource skill by id", e);
            throw new RuntimeException("Failed to find resource skill by id", e);
        }
    }

    public List<ResourceSkill> findByResourceId(Long resourceId) {
        String sql = "SELECT * FROM resource_skills WHERE resource_id = ? ORDER BY proficiency_level DESC";
        List<ResourceSkill> skills = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, resourceId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    skills.add(mapResultSetToResourceSkill(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find skills by resource id", e);
            throw new RuntimeException("Failed to find skills by resource id", e);
        }

        return skills;
    }

    public List<ResourceSkill> findBySkillId(Long skillId) {
        String sql = "SELECT * FROM resource_skills WHERE skill_id = ? ORDER BY proficiency_level DESC";
        List<ResourceSkill> resources = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, skillId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapResultSetToResourceSkill(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find resources by skill id", e);
            throw new RuntimeException("Failed to find resources by skill id", e);
        }

        return resources;
    }

    public List<ResourceSkill> findByProficiencyLevel(int minLevel) {
        String sql = "SELECT * FROM resource_skills WHERE proficiency_level >= ? ORDER BY proficiency_level DESC";
        List<ResourceSkill> skills = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, minLevel);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    skills.add(mapResultSetToResourceSkill(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find skills by proficiency level", e);
            throw new RuntimeException("Failed to find skills by proficiency level", e);
        }

        return skills;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM resource_skills WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.debug("Deleted resource skill with id: {}", id);
        } catch (SQLException e) {
            logger.error("Failed to delete resource skill", e);
            throw new RuntimeException("Failed to delete resource skill", e);
        }
    }

    private ResourceSkill mapResultSetToResourceSkill(ResultSet rs) throws SQLException {
        ResourceSkill resourceSkill = new ResourceSkill();
        resourceSkill.setId(rs.getLong("id"));
        resourceSkill.setResourceId(rs.getLong("resource_id"));
        resourceSkill.setSkillId(rs.getLong("skill_id"));
        
        int profLevel = rs.getInt("proficiency_level");
        if (!rs.wasNull()) {
            resourceSkill.setProficiencyLevel(profLevel);
        }
        
        int yearsExp = rs.getInt("years_of_experience");
        if (!rs.wasNull()) {
            resourceSkill.setYearsOfExperience(yearsExp);
        }
        
        resourceSkill.setNotes(rs.getString("notes"));
        
        Timestamp lastUsed = rs.getTimestamp("last_used_date");
        if (lastUsed != null) {
            resourceSkill.setLastUsedDate(lastUsed.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            resourceSkill.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            resourceSkill.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return resourceSkill;
    }
}