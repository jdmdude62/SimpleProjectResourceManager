import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;

public class GenerateDemoData {
    private static final Random random = new Random();
    private static int projectCounter = 1;
    private static final String[] CITIES = {
        "Nashville", "Memphis", "Knoxville", "Chattanooga", "Clarksville",
        "Council Bluffs", "Des Moines", "Cedar Rapids", "Davenport", "Sioux City",
        "Lincoln", "Omaha", "Grand Island", "Kearney", "North Platte"
    };
    
    private static final String[] STATES = {
        "TN", "IA", "NE", "MO", "KY", "AR", "MS", "AL", "GA", "SC"
    };
    
    public static void main(String[] args) throws Exception {
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        System.out.println("=== GENERATING DEMO DATA FROM TEMPLATES ===\n");
        
        // Load templates from JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        File templateFile = new File("templates/demo_templates.json");
        JsonNode root = mapper.readTree(templateFile);
        JsonNode templates = root.get("templates");
        
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            
            int projectCount = 0;
            int taskCount = 0;
            
            // Generate projects for 2024-2026
            LocalDate currentDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 12, 31);
            
            while (currentDate.isBefore(endDate)) {
                // Determine number of projects for this month based on season
                int month = currentDate.getMonthValue();
                int numProjects = getProjectsForMonth(month);
                
                for (int i = 0; i < numProjects; i++) {
                    // Pick a random template
                    JsonNode template = templates.get(random.nextInt(templates.size()));
                    String projectType = template.get("type").asText();
                    
                    // Generate project variation
                    LocalDate startDate = currentDate.plusDays(random.nextInt(28));
                    int baseDuration = template.get("baseDuration").asInt();
                    int duration = baseDuration + random.nextInt(3) - 1; // +/- 1 day variation
                    LocalDate projectEndDate = startDate.plusDays(duration);
                    
                    // Create project
                    String projectId = generateProjectId(projectType, startDate);
                    String city = CITIES[random.nextInt(CITIES.length)];
                    String state = STATES[random.nextInt(STATES.length)];
                    String description = template.get("baseDescription").asText()
                        .replaceAll("Nashville TN|Council Bluffs IA|Liincoln NE", city + " " + state);
                    
                    PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO projects (project_id, description, start_date, end_date, status, " +
                        "created_at) VALUES (?, ?, ?, ?, ?, datetime('now'))",
                        Statement.RETURN_GENERATED_KEYS
                    );
                    
                    pstmt.setString(1, projectId);
                    pstmt.setString(2, description);
                    pstmt.setString(3, startDate.toString());
                    pstmt.setString(4, projectEndDate.toString());
                    pstmt.setString(5, random.nextDouble() < 0.7 ? "COMPLETED" : "ACTIVE");
                    
                    pstmt.executeUpdate();
                    ResultSet keys = pstmt.getGeneratedKeys();
                    int projectDbId = keys.getInt(1);
                    projectCount++;
                    
                    // Create tasks for this project
                    JsonNode taskPatterns = template.get("taskPatterns");
                    if (taskPatterns != null) {
                        LocalDate taskStart = startDate;
                        for (int j = 0; j < taskPatterns.size(); j++) {
                            JsonNode taskPattern = taskPatterns.get(j);
                            
                            PreparedStatement taskStmt = conn.prepareStatement(
                                "INSERT INTO tasks (project_id, task_code, title, description, priority, " +
                                "status, planned_start, planned_end, estimated_hours, created_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))"
                            );
                            
                            String taskCode = projectId + "-T" + String.format("%03d", j + 1);
                            int taskDuration = Math.max(1, duration / Math.max(1, taskPatterns.size()));
                            LocalDate taskEnd = taskStart.plusDays(taskDuration - 1);
                            
                            taskStmt.setInt(1, projectDbId);
                            taskStmt.setString(2, taskCode);
                            taskStmt.setString(3, taskPattern.get("name").asText());
                            taskStmt.setString(4, taskPattern.has("description") ? 
                                taskPattern.get("description").asText() : null);
                            taskStmt.setString(5, "MEDIUM");
                            taskStmt.setString(6, projectEndDate.isBefore(LocalDate.now()) ? "COMPLETED" : "NOT_STARTED");
                            taskStmt.setString(7, taskStart.toString());
                            taskStmt.setString(8, taskEnd.toString());
                            taskStmt.setDouble(9, 8.0 * taskDuration); // 8 hours per day
                            
                            taskStmt.executeUpdate();
                            taskCount++;
                            
                            // Move to next task start
                            taskStart = taskEnd.plusDays(1);
                            if (taskStart.isAfter(projectEndDate)) {
                                taskStart = projectEndDate;
                            }
                        }
                    }
                }
                
                // Move to next month
                currentDate = currentDate.plusMonths(1).withDayOfMonth(1);
            }
            
            conn.commit();
            System.out.println("✓ Generated " + projectCount + " projects with " + taskCount + " tasks");
            System.out.println("✓ Demo data generation complete!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static int getProjectsForMonth(int month) {
        // Garden projects peak in spring/summer (April-August)
        // Doghouse projects peak in fall (September-November)
        // Cathouse projects year-round
        
        if (month >= 4 && month <= 8) {
            // Spring/Summer - more garden projects
            return 5 + random.nextInt(4); // 5-8 projects
        } else if (month >= 9 && month <= 11) {
            // Fall - more doghouse projects
            return 4 + random.nextInt(3); // 4-6 projects
        } else if (month == 12 || month <= 2) {
            // Winter - fewer projects
            return 2 + random.nextInt(2); // 2-3 projects
        } else {
            // Early spring/late fall - moderate
            return 3 + random.nextInt(3); // 3-5 projects
        }
    }
    
    private static String generateProjectId(String type, LocalDate date) {
        String prefix;
        switch (type) {
            case "garden":
                prefix = "GRDN";
                break;
            case "doghouse":
                prefix = random.nextBoolean() ? "DH-PBLD" : "DH-INST";
                break;
            default:
                prefix = "CH-" + (random.nextBoolean() ? "PBLD" : "INST");
                break;
        }
        
        return prefix + "-" + date.getYear() + "-" + String.format("%03d", projectCounter++);
    }
}