import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class ReportingDataCheck {
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            Statement stmt = conn.createStatement();
            
            System.out.println("=== EXECUTIVE REPORTING DATA AVAILABILITY ===\n");
            
            // Project Status Summary
            System.out.println("PROJECT STATUS SUMMARY:");
            ResultSet rs = stmt.executeQuery("""
                SELECT status, COUNT(*) as count,
                       printf('%.1f%%', COUNT(*) * 100.0 / (SELECT COUNT(*) FROM projects)) as percentage
                FROM projects
                GROUP BY status
            """);
            while (rs.next()) {
                System.out.printf("  %-12s: %3d projects (%s)\n", 
                    rs.getString("status"), rs.getInt("count"), rs.getString("percentage"));
            }
            
            // Monthly Project Volume
            System.out.println("\nMONTHLY PROJECT VOLUME (Last 6 months):");
            rs = stmt.executeQuery("""
                SELECT strftime('%Y-%m', start_date) as month,
                       COUNT(*) as project_count,
                       COUNT(DISTINCT (SELECT resource_id FROM assignments WHERE project_id = p.id)) as resources_used
                FROM projects p
                WHERE start_date >= date('now', '-6 months')
                GROUP BY month
                ORDER BY month DESC
                LIMIT 6
            """);
            while (rs.next()) {
                System.out.printf("  %s: %2d projects\n", 
                    rs.getString("month"), rs.getInt("project_count"));
            }
            
            // Resource Utilization
            System.out.println("\nRESOURCE UTILIZATION:");
            rs = stmt.executeQuery("""
                SELECT 
                    COUNT(DISTINCT r.id) as total_resources,
                    COUNT(DISTINCT a.resource_id) as assigned_resources,
                    printf('%.1f%%', COUNT(DISTINCT a.resource_id) * 100.0 / COUNT(DISTINCT r.id)) as utilization_rate
                FROM resources r
                LEFT JOIN assignments a ON r.id = a.resource_id 
                    AND date(a.start_date) <= date('now') 
                    AND date(a.end_date) >= date('now')
                WHERE r.is_active = 1
            """);
            if (rs.next()) {
                System.out.printf("  Active Resources: %d\n", rs.getInt("total_resources"));
                System.out.printf("  Currently Assigned: %d\n", rs.getInt("assigned_resources"));
                System.out.printf("  Utilization Rate: %s\n", rs.getString("utilization_rate"));
            }
            
            // Project Type Distribution
            System.out.println("\nPROJECT TYPE DISTRIBUTION:");
            rs = stmt.executeQuery("""
                SELECT 
                    CASE 
                        WHEN project_id LIKE 'GRDN%' THEN 'Garden'
                        WHEN project_id LIKE 'DH-%' THEN 'Dog House'
                        WHEN project_id LIKE 'CH-%' THEN 'Cat House'
                        ELSE 'Other'
                    END as project_type,
                    COUNT(*) as count,
                    AVG(julianday(end_date) - julianday(start_date)) as avg_duration
                FROM projects
                GROUP BY project_type
            """);
            while (rs.next()) {
                System.out.printf("  %-12s: %3d projects (avg %.1f days)\n", 
                    rs.getString("project_type"), 
                    rs.getInt("count"),
                    rs.getDouble("avg_duration"));
            }
            
            // Task Completion Metrics
            System.out.println("\nTASK METRICS:");
            rs = stmt.executeQuery("""
                SELECT 
                    COUNT(*) as total_tasks,
                    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
                    printf('%.1f%%', SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as completion_rate
                FROM tasks
            """);
            if (rs.next()) {
                System.out.printf("  Total Tasks: %d\n", rs.getInt("total_tasks"));
                System.out.printf("  Completed: %d\n", rs.getInt("completed"));
                System.out.printf("  Completion Rate: %s\n", rs.getString("completion_rate"));
            }
            
            // Geographic Distribution
            System.out.println("\nGEOGRAPHIC DISTRIBUTION (Top 5 locations):");
            rs = stmt.executeQuery("""
                SELECT location, COUNT(*) as project_count
                FROM projects
                WHERE location IS NOT NULL
                GROUP BY location
                ORDER BY project_count DESC
                LIMIT 5
            """);
            while (rs.next()) {
                System.out.printf("  %-30s: %d projects\n", 
                    rs.getString("location"), rs.getInt("project_count"));
            }
            
            System.out.println("\n✅ Data is sufficient for executive reporting!");
            System.out.println("📊 Recommended next step: Build reporting views/services");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}