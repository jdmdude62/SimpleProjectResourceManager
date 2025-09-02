package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkillRepository {
    private static final Logger logger = LoggerFactory.getLogger(SkillRepository.class);
    private final DataSource dataSource;

    public SkillRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS skills (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                category TEXT,
                is_active BOOLEAN DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.debug("Skills table created or verified");
        } catch (SQLException e) {
            logger.error("Failed to create skills table", e);
            throw new RuntimeException("Failed to create skills table", e);
        }
    }

    public Skill save(Skill skill) {
        if (skill.getId() == null) {
            return insert(skill);
        } else {
            return update(skill);
        }
    }

    private Skill insert(Skill skill) {
        String sql = """
            INSERT INTO skills (name, description, category, is_active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, skill.getName());
            pstmt.setString(2, skill.getDescription());
            pstmt.setString(3, skill.getCategory());
            pstmt.setBoolean(4, skill.isActive());
            pstmt.setTimestamp(5, Timestamp.valueOf(skill.getCreatedAt()));
            pstmt.setTimestamp(6, Timestamp.valueOf(skill.getUpdatedAt()));

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    skill.setId(rs.getLong(1));
                }
            }

            logger.debug("Inserted skill: {}", skill.getName());
            return skill;
        } catch (SQLException e) {
            logger.error("Failed to insert skill", e);
            throw new RuntimeException("Failed to insert skill", e);
        }
    }

    private Skill update(Skill skill) {
        String sql = """
            UPDATE skills 
            SET name = ?, description = ?, category = ?, is_active = ?, updated_at = ?
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, skill.getName());
            pstmt.setString(2, skill.getDescription());
            pstmt.setString(3, skill.getCategory());
            pstmt.setBoolean(4, skill.isActive());
            pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(6, skill.getId());

            pstmt.executeUpdate();
            logger.debug("Updated skill: {}", skill.getName());
            return skill;
        } catch (SQLException e) {
            logger.error("Failed to update skill", e);
            throw new RuntimeException("Failed to update skill", e);
        }
    }

    public Optional<Skill> findById(Long id) {
        String sql = "SELECT * FROM skills WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSkill(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find skill by id", e);
            throw new RuntimeException("Failed to find skill by id", e);
        }
    }

    public Optional<Skill> findByName(String name) {
        String sql = "SELECT * FROM skills WHERE name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSkill(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find skill by name", e);
            throw new RuntimeException("Failed to find skill by name", e);
        }
    }

    public List<Skill> findAll() {
        String sql = "SELECT * FROM skills ORDER BY category, name";
        List<Skill> skills = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                skills.add(mapResultSetToSkill(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all skills", e);
            throw new RuntimeException("Failed to find all skills", e);
        }

        return skills;
    }

    public List<Skill> findActive() {
        String sql = "SELECT * FROM skills WHERE is_active = 1 ORDER BY category, name";
        List<Skill> skills = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                skills.add(mapResultSetToSkill(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find active skills", e);
            throw new RuntimeException("Failed to find active skills", e);
        }

        return skills;
    }

    public List<Skill> findByCategory(String category) {
        String sql = "SELECT * FROM skills WHERE category = ? AND is_active = 1 ORDER BY name";
        List<Skill> skills = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, category);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    skills.add(mapResultSetToSkill(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find skills by category", e);
            throw new RuntimeException("Failed to find skills by category", e);
        }

        return skills;
    }

    public List<String> findAllCategories() {
        String sql = "SELECT DISTINCT category FROM skills WHERE category IS NOT NULL ORDER BY category";
        List<String> categories = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all categories", e);
            throw new RuntimeException("Failed to find all categories", e);
        }

        return categories;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM skills WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.debug("Deleted skill with id: {}", id);
        } catch (SQLException e) {
            logger.error("Failed to delete skill", e);
            throw new RuntimeException("Failed to delete skill", e);
        }
    }

    private Skill mapResultSetToSkill(ResultSet rs) throws SQLException {
        Skill skill = new Skill();
        skill.setId(rs.getLong("id"));
        skill.setName(rs.getString("name"));
        skill.setDescription(rs.getString("description"));
        skill.setCategory(rs.getString("category"));
        skill.setActive(rs.getBoolean("is_active"));
        skill.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        skill.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        return skill;
    }
}