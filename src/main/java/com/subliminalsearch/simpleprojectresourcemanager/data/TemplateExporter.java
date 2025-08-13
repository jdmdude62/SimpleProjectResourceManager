package com.subliminalsearch.simpleprojectresourcemanager.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility to export existing projects as templates for demo data generation
 */
public class TemplateExporter {
    private static final Logger logger = LoggerFactory.getLogger(TemplateExporter.class);
    
    private final ProjectRepository projectRepository;
    private final ResourceRepository resourceRepository;
    private final AssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    
    public TemplateExporter() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        this.projectRepository = new ProjectRepository(dbConfig.getDataSource());
        this.resourceRepository = new ResourceRepository(dbConfig.getDataSource());
        this.assignmentRepository = new AssignmentRepository(dbConfig.getDataSource());
        this.taskRepository = new TaskRepository(dbConfig.getDataSource());
    }
    
    public static void main(String[] args) {
        try {
            TemplateExporter exporter = new TemplateExporter();
            
            // Parse command line arguments
            List<Long> projectIds = new ArrayList<>();
            String outputPath = "templates/demo_templates.json";
            
            for (String arg : args) {
                if (arg.startsWith("--ids=")) {
                    String idsStr = arg.substring(6);
                    for (String id : idsStr.split(",")) {
                        projectIds.add(Long.parseLong(id.trim()));
                    }
                } else if (arg.startsWith("--output=")) {
                    outputPath = arg.substring(9);
                }
            }
            
            if (projectIds.isEmpty()) {
                System.err.println("No project IDs specified. Use --ids=1,2,3");
                System.exit(1);
            }
            
            // Export templates
            exporter.exportTemplates(projectIds, outputPath);
            
        } catch (Exception e) {
            logger.error("Failed to export templates", e);
            System.exit(1);
        }
    }
    
    public void exportTemplates(List<Long> projectIds, String outputPath) throws IOException {
        logger.info("Exporting {} projects as templates", projectIds.size());
        
        List<ExportedTemplate> templates = new ArrayList<>();
        
        for (Long projectId : projectIds) {
            try {
                Optional<Project> projectOpt = projectRepository.findById(projectId);
                if (projectOpt.isEmpty()) {
                    logger.warn("Project not found: {}", projectId);
                    continue;
                }
                
                Project project = projectOpt.get();
                ExportedTemplate template = createTemplate(project);
                templates.add(template);
                
                logger.info("Exported project: {} as template type: {}", 
                    project.getProjectId(), template.getType());
                
            } catch (Exception e) {
                logger.error("Failed to export project: {}", projectId, e);
            }
        }
        
        // Create template file
        TemplateFileExport file = new TemplateFileExport();
        file.setVersion("1.0");
        file.setCreated(LocalDateTime.now());
        file.setSourceDatabase(System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        file.setTemplates(templates);
        
        // Write to JSON file
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        mapper.writeValue(outputFile, file);
        
        logger.info("Templates exported to: {}", outputPath);
        System.out.println("Successfully exported " + templates.size() + " templates to " + outputPath);
    }
    
    private ExportedTemplate createTemplate(Project project) {
        ExportedTemplate template = new ExportedTemplate();
        
        // Determine project type from project ID
        String projectId = project.getProjectId();
        String type = determineProjectType(projectId);
        template.setType(type);
        
        // Set base project info
        template.setBaseProjectId(projectId);
        template.setBaseDescription(project.getDescription());
        template.setBaseDuration(calculateDuration(project.getStartDate(), project.getEndDate()));
        
        // Get assignments
        List<Assignment> assignments = assignmentRepository.findByProjectId(project.getId());
        template.setResourceCount(assignments.size());
        
        // Extract resource patterns
        List<ResourcePattern> resourcePatterns = new ArrayList<>();
        for (Assignment assignment : assignments) {
            Optional<Resource> resourceOpt = resourceRepository.findById(assignment.getResourceId());
            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();
                ResourcePattern pattern = new ResourcePattern();
                pattern.setResourceName(resource.getName());
                pattern.setRole(determineRole(resource.getName()));
                pattern.setTravelDays(assignment.getTravelOutDays() + assignment.getTravelBackDays());
                resourcePatterns.add(pattern);
            }
        }
        template.setResourcePatterns(resourcePatterns);
        
        // Get tasks
        List<Task> tasks = taskRepository.findByProjectId(project.getId());
        List<TaskPattern> taskPatterns = new ArrayList<>();
        
        for (Task task : tasks) {
            TaskPattern pattern = new TaskPattern();
            pattern.setName(task.getTitle());
            pattern.setDescription(task.getDescription());
            pattern.setSequence(taskPatterns.size() + 1); // Use sequential numbering
            pattern.setEstimatedHours(task.getEstimatedHours());
            taskPatterns.add(pattern);
        }
        template.setTaskPatterns(taskPatterns);
        
        // Set variation rules based on type
        VariationRules rules = new VariationRules();
        switch (type) {
            case "garden":
                rules.setMinDuration(2);
                rules.setMaxDuration(5);
                rules.setMinResources(1);
                rules.setMaxResources(3);
                rules.setSeasonalPreference("spring_summer");
                break;
            case "doghouse":
                rules.setMinDuration(3);
                rules.setMaxDuration(4);
                rules.setMinResources(2);
                rules.setMaxResources(5);
                rules.setSeasonalPreference("fall");
                rules.setRequiresTeamRotation(true);
                break;
            case "cathouse":
                rules.setMinDuration(2);
                rules.setMaxDuration(3);
                rules.setMinResources(2);
                rules.setMaxResources(4);
                rules.setSeasonalPreference("year_round");
                break;
            default:
                rules.setMinDuration(1);
                rules.setMaxDuration(5);
                rules.setMinResources(1);
                rules.setMaxResources(3);
                rules.setSeasonalPreference("year_round");
        }
        template.setVariationRules(rules);
        
        return template;
    }
    
    private String determineProjectType(String projectId) {
        if (projectId.startsWith("GAR-")) return "garden";
        if (projectId.startsWith("DH-")) return "doghouse";
        if (projectId.startsWith("CAT-")) return "cathouse";
        return "general";
    }
    
    private int calculateDuration(LocalDate start, LocalDate end) {
        int duration = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek().getValue() <= 5) { // Weekdays only
                duration++;
            }
            current = current.plusDays(1);
        }
        return duration;
    }
    
    private String determineRole(String resourceName) {
        String nameLower = resourceName.toLowerCase();
        if (nameLower.contains("lead") || nameLower.contains("senior")) return "lead";
        if (nameLower.contains("junior") || nameLower.contains("assistant")) return "assistant";
        if (nameLower.contains("specialist")) return "specialist";
        if (nameLower.contains("tech")) return "technician";
        return "general";
    }
}

// Export data classes

class TemplateFileExport {
    private String version;
    private LocalDateTime created;
    private String sourceDatabase;
    private List<ExportedTemplate> templates;
    
    // Getters and setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }
    public String getSourceDatabase() { return sourceDatabase; }
    public void setSourceDatabase(String sourceDatabase) { this.sourceDatabase = sourceDatabase; }
    public List<ExportedTemplate> getTemplates() { return templates; }
    public void setTemplates(List<ExportedTemplate> templates) { this.templates = templates; }
}

class ExportedTemplate {
    private String type;
    private String baseProjectId;
    private String baseDescription;
    private int baseDuration;
    private int resourceCount;
    private List<ResourcePattern> resourcePatterns;
    private List<TaskPattern> taskPatterns;
    private VariationRules variationRules;
    
    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBaseProjectId() { return baseProjectId; }
    public void setBaseProjectId(String baseProjectId) { this.baseProjectId = baseProjectId; }
    public String getBaseDescription() { return baseDescription; }
    public void setBaseDescription(String baseDescription) { this.baseDescription = baseDescription; }
    public int getBaseDuration() { return baseDuration; }
    public void setBaseDuration(int baseDuration) { this.baseDuration = baseDuration; }
    public int getResourceCount() { return resourceCount; }
    public void setResourceCount(int resourceCount) { this.resourceCount = resourceCount; }
    public List<ResourcePattern> getResourcePatterns() { return resourcePatterns; }
    public void setResourcePatterns(List<ResourcePattern> resourcePatterns) { this.resourcePatterns = resourcePatterns; }
    public List<TaskPattern> getTaskPatterns() { return taskPatterns; }
    public void setTaskPatterns(List<TaskPattern> taskPatterns) { this.taskPatterns = taskPatterns; }
    public VariationRules getVariationRules() { return variationRules; }
    public void setVariationRules(VariationRules variationRules) { this.variationRules = variationRules; }
}

class ResourcePattern {
    private String resourceName;
    private String role;
    private int travelDays;
    
    // Getters and setters
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getTravelDays() { return travelDays; }
    public void setTravelDays(int travelDays) { this.travelDays = travelDays; }
}

class TaskPattern {
    private String name;
    private String description;
    private int sequence;
    private Double estimatedHours;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }
}

class VariationRules {
    private int minDuration;
    private int maxDuration;
    private int minResources;
    private int maxResources;
    private String seasonalPreference;
    private boolean requiresTeamRotation;
    
    // Getters and setters
    public int getMinDuration() { return minDuration; }
    public void setMinDuration(int minDuration) { this.minDuration = minDuration; }
    public int getMaxDuration() { return maxDuration; }
    public void setMaxDuration(int maxDuration) { this.maxDuration = maxDuration; }
    public int getMinResources() { return minResources; }
    public void setMinResources(int minResources) { this.minResources = minResources; }
    public int getMaxResources() { return maxResources; }
    public void setMaxResources(int maxResources) { this.maxResources = maxResources; }
    public String getSeasonalPreference() { return seasonalPreference; }
    public void setSeasonalPreference(String seasonalPreference) { this.seasonalPreference = seasonalPreference; }
    public boolean isRequiresTeamRotation() { return requiresTeamRotation; }
    public void setRequiresTeamRotation(boolean requiresTeamRotation) { this.requiresTeamRotation = requiresTeamRotation; }
}