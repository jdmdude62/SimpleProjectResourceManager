package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import java.sql.*;
import java.time.LocalDate;

public class DebugHolidays {
    
    public static void main(String[] args) {
        System.setProperty("javafx.headless", "true");
        System.setProperty("java.awt.headless", "true");
        
        DatabaseConfig dbConfig = new DatabaseConfig();
        
        try (Connection conn = dbConfig.getDataSource().getConnection()) {
            System.out.println("Connected to database");
            
            // Check if table exists
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "company_holidays", null);
            if (!tables.next()) {
                System.out.println("Table 'company_holidays' does not exist! Creating it...");
                createTable(conn);
            } else {
                System.out.println("Table 'company_holidays' exists");
            }
            
            // Count holidays
            String countSql = "SELECT COUNT(*) FROM company_holidays";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next()) {
                    System.out.println("\nTotal holidays in database: " + rs.getInt(1));
                }
            }
            
            // Count active holidays
            String activeCountSql = "SELECT COUNT(*) FROM company_holidays WHERE active = 1";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(activeCountSql)) {
                if (rs.next()) {
                    System.out.println("Active holidays in database: " + rs.getInt(1));
                }
            }
            
            // List all holidays
            String sql = "SELECT id, name, date, type, description, active FROM company_holidays ORDER BY date";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                System.out.println("\nAll Holidays in Database:");
                System.out.println("ID | Name | Date | Type | Description | Active");
                System.out.println("---|------|------|------|-------------|-------");
                
                while (rs.next()) {
                    System.out.printf("%d | %s | %s | %s | %s | %s%n",
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("date"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getBoolean("active"));
                }
            }
            
            // If no holidays, add test ones
            String checkEmpty = "SELECT COUNT(*) as cnt FROM company_holidays";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkEmpty)) {
                if (rs.next() && rs.getInt("cnt") == 0) {
                    System.out.println("\nNo holidays found. Adding test holidays for August 2025...");
                    addTestHoliday(conn, "Test Holiday 1", LocalDate.of(2025, 8, 28));
                    addTestHoliday(conn, "Test Holiday 2", LocalDate.of(2025, 8, 29));
                    System.out.println("Added 2 test holidays");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConfig.shutdown();
        }
    }
    
    private static void createTable(Connection conn) throws SQLException {
        String createTable = "CREATE TABLE IF NOT EXISTS company_holidays (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "date TEXT NOT NULL, " +
            "type TEXT, " +
            "description TEXT, " +
            "working_holiday_allowed BOOLEAN DEFAULT 0, " +
            "active BOOLEAN DEFAULT 1, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            System.out.println("Created company_holidays table");
        }
    }
    
    private static void addTestHoliday(Connection conn, String name, LocalDate date) throws SQLException {
        String sql = "INSERT INTO company_holidays (name, date, type, description, working_holiday_allowed, active) " +
                    "VALUES (?, ?, 'COMPANY', 'Test holiday', 0, 1)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, date.toString());
            stmt.executeUpdate();
        }
    }
}