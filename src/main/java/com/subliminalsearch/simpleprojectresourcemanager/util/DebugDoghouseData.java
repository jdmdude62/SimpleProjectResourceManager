package com.subliminalsearch.simpleprojectresourcemanager.util;

import java.sql.*;

public class DebugDoghouseData {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        Statement stmt = conn.createStatement();
        
        System.out.println("=== DOGHOUSE DATA DEBUG ===\n");
        
        // Check if doghouse technicians exist
        System.out.println("1. Checking for Doghouse Technicians (IDs 51-60):");
        ResultSet rs = stmt.executeQuery("SELECT id, name FROM resources WHERE id BETWEEN 51 AND 60 ORDER BY id");
        int techCount = 0;
        while (rs.next()) {
            techCount++;
            System.out.println("   Tech #" + rs.getInt("id") + ": " + rs.getString("name"));
        }
        System.out.println("   Total technicians found: " + techCount);
        
        // Check if doghouse projects exist
        System.out.println("\n2. Checking for Doghouse Projects (DH-*):");
        rs = stmt.executeQuery("SELECT id, project_id, description, start_date, end_date FROM projects WHERE project_id LIKE 'DH-%' ORDER BY id");
        int dhProjectCount = 0;
        while (rs.next()) {
            dhProjectCount++;
            System.out.println("   Project #" + rs.getInt("id") + ": " + rs.getString("project_id") + 
                " (" + rs.getString("start_date").substring(0, 10) + " to " + 
                rs.getString("end_date").substring(0, 10) + ")");
        }
        System.out.println("   Total doghouse projects found: " + dhProjectCount);
        
        // Check ALL January 2025 projects
        System.out.println("\n3. ALL Projects in January 2025:");
        rs = stmt.executeQuery(
            "SELECT id, project_id, description, start_date, end_date FROM projects " +
            "WHERE date(start_date) <= date('2025-01-31') " +
            "AND date(end_date) >= date('2025-01-01') " +
            "ORDER BY start_date"
        );
        int janProjectCount = 0;
        while (rs.next()) {
            janProjectCount++;
            String desc = rs.getString("description");
            if (desc.length() > 30) desc = desc.substring(0, 30) + "...";
            System.out.println("   " + rs.getString("project_id") + ": " + desc +
                " (" + rs.getString("start_date").substring(0, 10) + " to " + 
                rs.getString("end_date").substring(0, 10) + ")");
        }
        System.out.println("   Total January projects: " + janProjectCount);
        
        // Check assignments for doghouse projects
        System.out.println("\n4. Assignments for Doghouse Projects:");
        rs = stmt.executeQuery(
            "SELECT a.*, p.project_id, r.name as resource_name " +
            "FROM assignments a " +
            "JOIN projects p ON a.project_id = p.id " +
            "JOIN resources r ON a.resource_id = r.id " +
            "WHERE p.project_id LIKE 'DH-%' " +
            "ORDER BY a.start_date LIMIT 10"
        );
        int assignCount = 0;
        while (rs.next()) {
            assignCount++;
            System.out.println("   " + rs.getString("project_id") + " <- " + 
                rs.getString("resource_name") + " (" + 
                rs.getString("start_date") + " to " + rs.getString("end_date") + ")");
        }
        System.out.println("   Total doghouse assignments: " + assignCount);
        
        // Check what ProjectRepository would return for January
        System.out.println("\n5. Testing ProjectRepository query for January 2025:");
        String sql = "SELECT * FROM projects " +
                     "WHERE date(start_date) <= date(?) AND date(end_date) >= date(?) " +
                     "ORDER BY start_date ASC";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, "2025-01-31");
        pstmt.setString(2, "2025-01-01");
        rs = pstmt.executeQuery();
        int queryCount = 0;
        while (rs.next()) {
            queryCount++;
            if (queryCount <= 5) {  // Show first 5
                System.out.println("   Found: " + rs.getString("project_id"));
            }
        }
        System.out.println("   Query returned " + queryCount + " projects");
        
        conn.close();
        System.out.println("\n=== END DEBUG ===");
    }
}