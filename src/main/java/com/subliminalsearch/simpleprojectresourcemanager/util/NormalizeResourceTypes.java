package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to normalize resource type values in the database
 * to match the new dropdown options
 */
public class NormalizeResourceTypes {
    private static final Logger logger = LoggerFactory.getLogger(NormalizeResourceTypes.class);
    
    // Mapping of common variations to standardized values
    private static final Map<String, String> TYPE_MAPPINGS = new HashMap<>();
    
    static {
        // INTERNAL mappings
        TYPE_MAPPINGS.put("employee", "Full-Time Employee");
        TYPE_MAPPINGS.put("full time", "Full-Time Employee");
        TYPE_MAPPINGS.put("fulltime", "Full-Time Employee");
        TYPE_MAPPINGS.put("full-time", "Full-Time Employee");
        TYPE_MAPPINGS.put("ft employee", "Full-Time Employee");
        TYPE_MAPPINGS.put("part time", "Part-Time Employee");
        TYPE_MAPPINGS.put("parttime", "Part-Time Employee");
        TYPE_MAPPINGS.put("part-time", "Part-Time Employee");
        TYPE_MAPPINGS.put("pt employee", "Part-Time Employee");
        TYPE_MAPPINGS.put("tech", "Technician");
        TYPE_MAPPINGS.put("field tech", "Technician");
        TYPE_MAPPINGS.put("field technician", "Technician");
        TYPE_MAPPINGS.put("temp", "Temporary Employee");
        TYPE_MAPPINGS.put("temporary", "Temporary Employee");
        TYPE_MAPPINGS.put("mgr", "Manager");
        TYPE_MAPPINGS.put("manager", "Manager");
        TYPE_MAPPINGS.put("eng", "Engineer");
        TYPE_MAPPINGS.put("engineer", "Engineer");
        TYPE_MAPPINGS.put("support", "Support Staff");
        TYPE_MAPPINGS.put("intern", "Intern");
        
        // CONTRACTOR mappings
        TYPE_MAPPINGS.put("contractor", "Independent Contractor");
        TYPE_MAPPINGS.put("contract", "Independent Contractor");
        TYPE_MAPPINGS.put("consultant", "Consultant");
        TYPE_MAPPINGS.put("consulting", "Consultant");
        TYPE_MAPPINGS.put("freelance", "Freelancer");
        TYPE_MAPPINGS.put("freelancer", "Freelancer");
        TYPE_MAPPINGS.put("subcontractor", "Subcontractor");
        TYPE_MAPPINGS.put("sub contractor", "Subcontractor");
        TYPE_MAPPINGS.put("sub", "Subcontractor");
        TYPE_MAPPINGS.put("hourly", "Hourly Contractor");
        TYPE_MAPPINGS.put("specialist", "Specialist");
        TYPE_MAPPINGS.put("technical", "Technical Contractor");
        
        // VENDOR mappings
        TYPE_MAPPINGS.put("vendor", "Service Provider");
        TYPE_MAPPINGS.put("supplier", "Material Supplier");
        TYPE_MAPPINGS.put("equipment", "Equipment Vendor");
        TYPE_MAPPINGS.put("equipment supplier", "Equipment Vendor");
        TYPE_MAPPINGS.put("service", "Service Provider");
        TYPE_MAPPINGS.put("services", "Service Provider");
        TYPE_MAPPINGS.put("maintenance", "Maintenance Provider");
        TYPE_MAPPINGS.put("software", "Software Vendor");
        TYPE_MAPPINGS.put("consulting firm", "Consulting Firm");
        TYPE_MAPPINGS.put("consultancy", "Consulting Firm");
        TYPE_MAPPINGS.put("staffing", "Staffing Agency");
        TYPE_MAPPINGS.put("staffing agency", "Staffing Agency");
        TYPE_MAPPINGS.put("3rd party", "Third-Party Service");
        TYPE_MAPPINGS.put("third party", "Third-Party Service");
        TYPE_MAPPINGS.put("third-party", "Third-Party Service");
    }
    
    public static void normalizeTypes(HikariDataSource dataSource) {
        logger.info("Starting resource type normalization...");
        
        int updated = 0;
        int skipped = 0;
        
        try (Connection conn = dataSource.getConnection()) {
            // First, let's see what types we have - resources use resource_types table
            String selectSql = """
                SELECT rt.id, rt.name as type_name, rt.category 
                FROM resource_types rt
                WHERE rt.name IS NOT NULL
                """;
            
            Map<Long, String> updates = new HashMap<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String currentType = rs.getString("type_name");
                    String category = rs.getString("category");
                    
                    if (currentType == null || currentType.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Check if it needs normalization
                    String normalized = normalizeType(currentType, category);
                    
                    if (!currentType.equals(normalized)) {
                        updates.put(id, normalized);
                        logger.info("Will update resource {}: '{}' -> '{}'", id, currentType, normalized);
                    } else {
                        skipped++;
                    }
                }
            }
            
            // Now apply the updates to resource_types table
            if (!updates.isEmpty()) {
                String updateSql = "UPDATE resource_types SET name = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    for (Map.Entry<Long, String> entry : updates.entrySet()) {
                        stmt.setString(1, entry.getValue());
                        stmt.setLong(2, entry.getKey());
                        stmt.executeUpdate();
                        updated++;
                    }
                }
                
                conn.commit();
                logger.info("Successfully normalized {} resource types", updated);
            } else {
                logger.info("No resource types needed normalization");
            }
            
            logger.info("Normalization complete - Updated: {}, Already normalized: {}", updated, skipped);
            
        } catch (SQLException e) {
            logger.error("Failed to normalize resource types", e);
            // Don't throw exception, just log the error - this is non-critical
        }
    }
    
    private static String normalizeType(String currentType, String category) {
        if (currentType == null) return null;
        
        String trimmed = currentType.trim();
        String lower = trimmed.toLowerCase();
        
        // First check direct mappings
        if (TYPE_MAPPINGS.containsKey(lower)) {
            return TYPE_MAPPINGS.get(lower);
        }
        
        // Check for common patterns based on category
        if (category != null) {
            switch (category.toUpperCase()) {
                case "INTERNAL":
                    // Check for employee-related keywords
                    if (lower.contains("employee") || lower.contains("staff")) {
                        if (lower.contains("full") || lower.contains("ft")) {
                            return "Full-Time Employee";
                        } else if (lower.contains("part") || lower.contains("pt")) {
                            return "Part-Time Employee";
                        } else if (lower.contains("temp")) {
                            return "Temporary Employee";
                        }
                        return "Full-Time Employee"; // Default for generic "employee"
                    }
                    if (lower.contains("tech")) return "Technician";
                    if (lower.contains("manager") || lower.contains("mgr")) return "Manager";
                    if (lower.contains("engineer") || lower.contains("eng")) return "Engineer";
                    if (lower.contains("intern")) return "Intern";
                    if (lower.contains("support")) return "Support Staff";
                    break;
                    
                case "CONTRACTOR":
                    if (lower.contains("consult")) return "Consultant";
                    if (lower.contains("freelance")) return "Freelancer";
                    if (lower.contains("sub")) return "Subcontractor";
                    if (lower.contains("hour")) return "Hourly Contractor";
                    if (lower.contains("special")) return "Specialist";
                    if (lower.contains("technical") || lower.contains("tech")) return "Technical Contractor";
                    if (lower.contains("contract")) return "Independent Contractor";
                    break;
                    
                case "VENDOR":
                    if (lower.contains("equipment")) return "Equipment Vendor";
                    if (lower.contains("material") || lower.contains("supply")) return "Material Supplier";
                    if (lower.contains("software")) return "Software Vendor";
                    if (lower.contains("maintenance")) return "Maintenance Provider";
                    if (lower.contains("consulting") || lower.contains("consult")) return "Consulting Firm";
                    if (lower.contains("staff")) return "Staffing Agency";
                    if (lower.contains("third") || lower.contains("3rd")) return "Third-Party Service";
                    if (lower.contains("service")) return "Service Provider";
                    if (lower.contains("vendor")) return "Service Provider";
                    break;
            }
        }
        
        // If no mapping found, return the original (it might be a custom value)
        return trimmed;
    }
    
    public static void main(String[] args) {
        logger.info("Resource Type Normalization Utility");
        logger.info("====================================");
        
        HikariDataSource dataSource = null;
        
        try {
            DatabaseConfig dbConfig = new DatabaseConfig();
            dataSource = dbConfig.getDataSource();
            
            normalizeTypes(dataSource);
            
            logger.info("Normalization completed successfully!");
            
        } catch (Exception e) {
            logger.error("Normalization failed", e);
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }
}