import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class CreateAssignments {
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            
            System.out.println("=== CREATING RESOURCE ASSIGNMENTS ===\n");
            
            // Get all active resources
            List<Integer> resourceIds = new ArrayList<>();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name FROM resources WHERE is_active = 1");
            while (rs.next()) {
                resourceIds.add(rs.getInt("id"));
            }
            System.out.println("Found " + resourceIds.size() + " active resources");
            
            if (resourceIds.isEmpty()) {
                System.err.println("No field technicians found!");
                return;
            }
            
            // Get all projects with their dates
            rs = stmt.executeQuery("""
                SELECT id, project_id, start_date, end_date 
                FROM projects 
                WHERE id > 4
                ORDER BY start_date
            """);
            
            int assignmentCount = 0;
            int projectCount = 0;
            
            PreparedStatement assignStmt = conn.prepareStatement("""
                INSERT INTO assignments 
                (resource_id, project_id, start_date, end_date, created_at)
                VALUES (?, ?, ?, ?, datetime('now'))
            """);
            
            // Track resource availability to avoid conflicts
            Map<Integer, LocalDate> resourceAvailability = new HashMap<>();
            for (int resourceId : resourceIds) {
                resourceAvailability.put(resourceId, LocalDate.of(2024, 1, 1));
            }
            
            while (rs.next()) {
                int projectId = rs.getInt("id");
                String projectCode = rs.getString("project_id");
                LocalDate startDate = LocalDate.parse(rs.getString("start_date"));
                LocalDate endDate = LocalDate.parse(rs.getString("end_date"));
                
                // Determine how many resources needed based on project type
                int resourcesNeeded = 1;
                if (projectCode.contains("GRDN")) {
                    resourcesNeeded = 2 + random.nextInt(2); // 2-3 for garden
                } else if (projectCode.contains("DH-")) {
                    resourcesNeeded = 2 + random.nextInt(3); // 2-4 for doghouse
                } else {
                    resourcesNeeded = 1 + random.nextInt(2); // 1-2 for cathouse
                }
                
                // Find available resources
                List<Integer> availableResources = new ArrayList<>();
                for (Map.Entry<Integer, LocalDate> entry : resourceAvailability.entrySet()) {
                    if (entry.getValue().isBefore(startDate) || entry.getValue().isEqual(startDate)) {
                        availableResources.add(entry.getKey());
                    }
                }
                
                // If not enough available, use any resources (allow some overlap)
                if (availableResources.size() < resourcesNeeded) {
                    availableResources = new ArrayList<>(resourceIds);
                }
                
                // Shuffle and take the needed number
                Collections.shuffle(availableResources);
                List<Integer> assignedResources = availableResources.subList(0, 
                    Math.min(resourcesNeeded, availableResources.size()));
                
                // Create assignments
                for (int resourceId : assignedResources) {
                    assignStmt.setInt(1, resourceId);
                    assignStmt.setInt(2, projectId);
                    assignStmt.setString(3, startDate.toString());
                    assignStmt.setString(4, endDate.toString());
                    assignStmt.executeUpdate();
                    assignmentCount++;
                    
                    // Update resource availability (add 1 day buffer)
                    resourceAvailability.put(resourceId, endDate.plusDays(1));
                }
                
                projectCount++;
                
                if (projectCount % 20 == 0) {
                    System.out.println("Processed " + projectCount + " projects...");
                }
            }
            
            conn.commit();
            System.out.println("\n✓ Created " + assignmentCount + " resource assignments for " + projectCount + " projects");
            
            // Show summary
            rs = stmt.executeQuery("""
                SELECT 
                    strftime('%Y-%m', start_date) as month,
                    COUNT(DISTINCT project_id) as projects,
                    COUNT(DISTINCT resource_id) as resources,
                    COUNT(*) as assignments
                FROM assignments
                GROUP BY month
                ORDER BY month DESC
                LIMIT 6
            """);
            
            System.out.println("\nRecent months summary:");
            System.out.println("Month     | Projects | Resources | Assignments");
            System.out.println("----------|----------|-----------|------------");
            while (rs.next()) {
                System.out.printf("%-9s | %8d | %9d | %11d\n",
                    rs.getString("month"),
                    rs.getInt("projects"),
                    rs.getInt("resources"),
                    rs.getInt("assignments"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}