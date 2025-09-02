package com.subliminalsearch.simpleprojectresourcemanager.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class OpenItemMetadataService {
    private static final Logger LOGGER = Logger.getLogger(OpenItemMetadataService.class.getName());
    private final DataSource dataSource;
    
    // Default categories for industrial automation
    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
        // Core Automation Systems
        "PLC Programming",
        "SCADA Configuration",
        "HMI Development",
        "DCS Setup",
        "Field Instrumentation",
        
        // Testing & Commissioning
        "Factory Acceptance Test (FAT)",
        "Site Acceptance Test (SAT)",
        "Loop Testing",
        "System Integration Testing",
        "Performance Testing",
        
        // Documentation & Compliance
        "P&ID Review",
        "Control Narrative",
        "Alarm Management",
        "Safety Documentation",
        "As-Built Documentation",
        
        // Field Service & Installation
        "Equipment Installation",
        "Cable Termination",
        "Panel Wiring",
        "Network Configuration",
        "Field Device Calibration",
        
        // Software & Configuration
        "Software Deployment",
        "Database Configuration",
        "Communication Setup",
        "Redundancy Configuration",
        "Backup Configuration",
        
        // Training & Support
        "Operator Training",
        "Maintenance Training",
        "System Handover",
        "Support Documentation",
        
        // Issues & Corrections
        "Punch List Item",
        "Deficiency Report",
        "Non-Conformance",
        "Change Request",
        "Bug Fix",
        
        // Project Management
        "Milestone Review",
        "Client Approval",
        "Vendor Coordination",
        "Resource Allocation",
        "Schedule Update"
    );
    
    // Default tags for industrial automation
    private static final List<String> DEFAULT_TAGS = Arrays.asList(
        // Priority/Urgency
        "urgent",
        "critical",
        "safety-critical",
        "blocking",
        "high-priority",
        
        // System Components
        "plc",
        "scada",
        "hmi",
        "dcs",
        "instrumentation",
        "networking",
        "database",
        
        // Phase/Stage
        "commissioning",
        "testing",
        "installation",
        "configuration",
        "integration",
        "startup",
        "shutdown",
        
        // Type of Work
        "hardware",
        "software",
        "electrical",
        "mechanical",
        "documentation",
        "training",
        
        // Status/Action
        "needs-review",
        "pending-approval",
        "vendor-action",
        "client-approval",
        "on-site",
        "remote",
        
        // Compliance/Quality
        "safety",
        "quality",
        "compliance",
        "regulatory",
        "audit",
        
        // Common Issues
        "bug",
        "defect",
        "enhancement",
        "change-order",
        "warranty",
        
        // Resources
        "requires-shutdown",
        "requires-specialist",
        "requires-parts",
        "weather-dependent"
    );
    
    public OpenItemMetadataService(DataSource dataSource) {
        this.dataSource = dataSource;
        createTablesIfNotExist();
        initializeDefaultValues();
    }
    
    private void createTablesIfNotExist() {
        String createCategoriesTable = """
            CREATE TABLE IF NOT EXISTS open_item_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_name TEXT NOT NULL UNIQUE,
                is_default BOOLEAN DEFAULT 0,
                usage_count INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_used TIMESTAMP
            )
        """;
        
        String createTagsTable = """
            CREATE TABLE IF NOT EXISTS open_item_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tag_name TEXT NOT NULL UNIQUE,
                is_default BOOLEAN DEFAULT 0,
                usage_count INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_used TIMESTAMP
            )
        """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createCategoriesTable);
            stmt.execute(createTagsTable);
            LOGGER.info("Open item metadata tables created/verified");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating metadata tables", e);
        }
    }
    
    private void initializeDefaultValues() {
        // Initialize default categories
        String insertCategory = "INSERT OR IGNORE INTO open_item_categories (category_name, is_default) VALUES (?, 1)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertCategory)) {
            for (String category : DEFAULT_CATEGORIES) {
                pstmt.setString(1, category);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error initializing default categories", e);
        }
        
        // Initialize default tags
        String insertTag = "INSERT OR IGNORE INTO open_item_tags (tag_name, is_default) VALUES (?, 1)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertTag)) {
            for (String tag : DEFAULT_TAGS) {
                pstmt.setString(1, tag);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error initializing default tags", e);
        }
    }
    
    public ObservableList<String> getAllCategories() {
        ObservableList<String> categories = FXCollections.observableArrayList();
        String query = "SELECT category_name FROM open_item_categories ORDER BY usage_count DESC, category_name ASC";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                categories.add(rs.getString("category_name"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading categories", e);
            // Return defaults if database fails
            categories.addAll(DEFAULT_CATEGORIES);
        }
        
        return categories;
    }
    
    public ObservableList<String> getAllTags() {
        ObservableList<String> tags = FXCollections.observableArrayList();
        String query = "SELECT tag_name FROM open_item_tags ORDER BY usage_count DESC, tag_name ASC";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                tags.add(rs.getString("tag_name"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading tags", e);
            // Return defaults if database fails
            tags.addAll(DEFAULT_TAGS);
        }
        
        return tags;
    }
    
    public void addCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return;
        }
        
        String sql = """
            INSERT INTO open_item_categories (category_name, is_default) 
            VALUES (?, 0) 
            ON CONFLICT(category_name) DO UPDATE SET 
                usage_count = usage_count + 1,
                last_used = CURRENT_TIMESTAMP
        """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.trim());
            pstmt.executeUpdate();
            LOGGER.info("Category added/updated: " + category);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error adding category: " + category, e);
        }
    }
    
    public void addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        
        String sql = """
            INSERT INTO open_item_tags (tag_name, is_default) 
            VALUES (?, 0) 
            ON CONFLICT(tag_name) DO UPDATE SET 
                usage_count = usage_count + 1,
                last_used = CURRENT_TIMESTAMP
        """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tag.trim());
            pstmt.executeUpdate();
            LOGGER.info("Tag added/updated: " + tag);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error adding tag: " + tag, e);
        }
    }
    
    public void removeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return;
        }
        
        String sql = "DELETE FROM open_item_categories WHERE category_name = ? AND is_default = 0";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.trim());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("Category removed: " + category);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error removing category: " + category, e);
        }
    }
    
    public void removeTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        
        String sql = "DELETE FROM open_item_tags WHERE tag_name = ? AND is_default = 0";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tag.trim());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("Tag removed: " + tag);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error removing tag: " + tag, e);
        }
    }
    
    public boolean isDefaultCategory(String category) {
        String sql = "SELECT is_default FROM open_item_categories WHERE category_name = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_default");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error checking if category is default: " + category, e);
        }
        
        return false;
    }
    
    public boolean isDefaultTag(String tag) {
        String sql = "SELECT is_default FROM open_item_tags WHERE tag_name = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tag);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_default");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error checking if tag is default: " + tag, e);
        }
        
        return false;
    }
}