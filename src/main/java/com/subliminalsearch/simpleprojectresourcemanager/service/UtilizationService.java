package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.CompanyHoliday;
import com.subliminalsearch.simpleprojectresourcemanager.model.UtilizationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Centralized service for all utilization calculations and settings management.
 * This ensures consistent calculations across the entire application.
 */
public class UtilizationService {
    private static final Logger logger = LoggerFactory.getLogger(UtilizationService.class);
    
    private final DataSource dataSource;
    private UtilizationSettings currentSettings;
    
    public UtilizationService(DataSource dataSource) {
        this.dataSource = dataSource;
        initializeDatabase();
        loadSettings();
    }
    
    /**
     * Initialize the database table for utilization settings
     */
    private void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS utilization_settings (
                id INTEGER PRIMARY KEY,
                include_weekends BOOLEAN DEFAULT 0,
                include_saturdays BOOLEAN DEFAULT 0,
                include_holidays BOOLEAN DEFAULT 0,
                count_pto_as_utilized BOOLEAN DEFAULT 0,
                count_shop_as_utilized BOOLEAN DEFAULT 1,
                count_training_as_utilized BOOLEAN DEFAULT 1,
                standard_work_week REAL DEFAULT 40.0,
                hours_per_day REAL DEFAULT 8.0,
                overtime_threshold REAL DEFAULT 100.0,
                target_utilization REAL DEFAULT 80.0,
                minimum_utilization REAL DEFAULT 65.0,
                overallocation_alert REAL DEFAULT 110.0,
                target_billable REAL DEFAULT 75.0,
                minimum_billable REAL DEFAULT 60.0,
                calculation_method TEXT DEFAULT 'WORKING_DAYS',
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
            
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Utilization settings table initialized");
        } catch (SQLException e) {
            logger.error("Error creating utilization settings table", e);
        }
    }
    
    /**
     * Load settings from database or create default if none exist
     */
    private void loadSettings() {
        String sql = "SELECT * FROM utilization_settings WHERE id = 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                currentSettings = new UtilizationSettings();
                currentSettings.setIncludeWeekends(rs.getBoolean("include_weekends"));
                currentSettings.setIncludeSaturdays(rs.getBoolean("include_saturdays"));
                currentSettings.setIncludeHolidays(rs.getBoolean("include_holidays"));
                currentSettings.setCountPtoAsUtilized(rs.getBoolean("count_pto_as_utilized"));
                currentSettings.setCountShopAsUtilized(rs.getBoolean("count_shop_as_utilized"));
                currentSettings.setCountTrainingAsUtilized(rs.getBoolean("count_training_as_utilized"));
                currentSettings.setStandardWorkWeek(rs.getDouble("standard_work_week"));
                currentSettings.setHoursPerDay(rs.getDouble("hours_per_day"));
                currentSettings.setOvertimeThreshold(rs.getDouble("overtime_threshold"));
                currentSettings.setTargetUtilization(rs.getDouble("target_utilization"));
                currentSettings.setMinimumUtilization(rs.getDouble("minimum_utilization"));
                currentSettings.setOverallocationAlert(rs.getDouble("overallocation_alert"));
                currentSettings.setTargetBillable(rs.getDouble("target_billable"));
                currentSettings.setMinimumBillable(rs.getDouble("minimum_billable"));
                
                String methodStr = rs.getString("calculation_method");
                if (methodStr != null) {
                    try {
                        currentSettings.setCalculationMethod(UtilizationSettings.CalculationMethod.valueOf(methodStr));
                    } catch (IllegalArgumentException e) {
                        currentSettings.setCalculationMethod(UtilizationSettings.CalculationMethod.WORKING_DAYS);
                    }
                }
                
                logger.info("Loaded utilization settings from database");
            } else {
                // No settings exist, create defaults
                currentSettings = new UtilizationSettings();
                saveSettings(currentSettings);
                logger.info("Created default utilization settings");
            }
        } catch (SQLException e) {
            logger.error("Error loading utilization settings", e);
            currentSettings = new UtilizationSettings(); // Fallback to defaults
        }
    }
    
    /**
     * Save settings to database
     */
    public void saveSettings(UtilizationSettings settings) {
        String sql = """
            INSERT OR REPLACE INTO utilization_settings 
            (id, include_weekends, include_saturdays, include_holidays, 
             count_pto_as_utilized, count_shop_as_utilized, count_training_as_utilized,
             standard_work_week, hours_per_day, overtime_threshold,
             target_utilization, minimum_utilization, overallocation_alert,
             target_billable, minimum_billable, calculation_method, last_updated)
            VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
            
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, settings.isIncludeWeekends());
            stmt.setBoolean(2, settings.isIncludeSaturdays());
            stmt.setBoolean(3, settings.isIncludeHolidays());
            stmt.setBoolean(4, settings.isCountPtoAsUtilized());
            stmt.setBoolean(5, settings.isCountShopAsUtilized());
            stmt.setBoolean(6, settings.isCountTrainingAsUtilized());
            stmt.setDouble(7, settings.getStandardWorkWeek());
            stmt.setDouble(8, settings.getHoursPerDay());
            stmt.setDouble(9, settings.getOvertimeThreshold());
            stmt.setDouble(10, settings.getTargetUtilization());
            stmt.setDouble(11, settings.getMinimumUtilization());
            stmt.setDouble(12, settings.getOverallocationAlert());
            stmt.setDouble(13, settings.getTargetBillable());
            stmt.setDouble(14, settings.getMinimumBillable());
            stmt.setString(15, settings.getCalculationMethod().name());
            
            stmt.executeUpdate();
            this.currentSettings = settings;
            logger.info("Saved utilization settings to database");
        } catch (SQLException e) {
            logger.error("Error saving utilization settings", e);
        }
    }
    
    /**
     * Get current utilization settings
     */
    public UtilizationSettings getSettings() {
        return currentSettings;
    }
    
    /**
     * Calculate available days based on current settings
     */
    public long calculateAvailableDays(LocalDate start, LocalDate end) {
        if (currentSettings.getCalculationMethod() == UtilizationSettings.CalculationMethod.CALENDAR_DAYS) {
            return ChronoUnit.DAYS.between(start, end) + 1;
        } else {
            return calculateWorkingDays(start, end, null);
        }
    }
    
    /**
     * Calculate available hours based on current settings
     */
    public double calculateAvailableHours(LocalDate start, LocalDate end) {
        long availableDays = calculateAvailableDays(start, end);
        return availableDays * currentSettings.getHoursPerDay();
    }
    
    /**
     * Calculate working days excluding weekends and optionally holidays
     */
    public long calculateWorkingDays(LocalDate start, LocalDate end, List<CompanyHoliday> holidays) {
        long workingDays = 0;
        LocalDate current = start;
        
        while (!current.isAfter(end)) {
            boolean isWorkingDay = true;
            
            // Check weekends
            if (current.getDayOfWeek() == DayOfWeek.SATURDAY) {
                isWorkingDay = currentSettings.isIncludeSaturdays();
            } else if (current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                isWorkingDay = currentSettings.isIncludeWeekends();
            }
            
            // Check holidays
            if (isWorkingDay && !currentSettings.isIncludeHolidays() && holidays != null) {
                LocalDate currentDate = current;
                boolean isHoliday = holidays.stream()
                    .anyMatch(h -> h.getDate().equals(currentDate) && h.isActive());
                if (isHoliday) {
                    isWorkingDay = false;
                }
            }
            
            if (isWorkingDay) {
                workingDays++;
            }
            
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    /**
     * Calculate working hours for a period
     */
    public double calculateWorkingHours(LocalDate start, LocalDate end, List<CompanyHoliday> holidays) {
        long workingDays = calculateWorkingDays(start, end, holidays);
        return workingDays * currentSettings.getHoursPerDay();
    }
    
    /**
     * Determine if a project type counts as utilized time
     */
    public boolean countsAsUtilized(String projectId) {
        if (projectId == null) return true;
        
        if ("SHOP".equalsIgnoreCase(projectId)) {
            return currentSettings.isCountShopAsUtilized();
        } else if ("TRAINING".equalsIgnoreCase(projectId)) {
            return currentSettings.isCountTrainingAsUtilized();
        } else if ("PTO".equalsIgnoreCase(projectId)) {
            return currentSettings.isCountPtoAsUtilized();
        }
        
        return true; // Regular projects always count
    }
    
    /**
     * Determine if a project type is billable
     */
    public boolean isBillable(String projectId) {
        if (projectId == null) return true;
        
        return !("SHOP".equalsIgnoreCase(projectId) || 
                 "TRAINING".equalsIgnoreCase(projectId) || 
                 "PTO".equalsIgnoreCase(projectId));
    }
    
    /**
     * Get utilization color based on percentage and thresholds
     */
    public String getUtilizationColor(double utilizationPercent) {
        if (utilizationPercent >= currentSettings.getOverallocationAlert()) {
            return "#ff4444"; // Red - overallocated
        } else if (utilizationPercent >= currentSettings.getTargetUtilization()) {
            return "#44ff44"; // Green - on target
        } else if (utilizationPercent >= currentSettings.getMinimumUtilization()) {
            return "#ffaa44"; // Orange - below target
        } else {
            return "#aaaaaa"; // Gray - underutilized
        }
    }
    
    /**
     * Get billable color based on percentage and thresholds
     */
    public String getBillableColor(double billablePercent) {
        if (billablePercent >= currentSettings.getTargetBillable()) {
            return "#44ff44"; // Green - on target
        } else if (billablePercent >= currentSettings.getMinimumBillable()) {
            return "#ffaa44"; // Orange - below target
        } else {
            return "#ff4444"; // Red - below minimum
        }
    }
}