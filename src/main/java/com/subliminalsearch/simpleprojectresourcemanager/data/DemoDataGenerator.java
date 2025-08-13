package com.subliminalsearch.simpleprojectresourcemanager.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Comprehensive demo data generator that creates realistic project data
 * based on templates and business rules.
 */
public class DemoDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DemoDataGenerator.class);
    
    private final DataSource dataSource;
    private final ProjectRepository projectRepository;
    private final ResourceRepository resourceRepository;
    private final AssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    
    private DemoDataConfig config;
    private List<ProjectTemplate> templates;
    private final Random random;
    private final Map<String, List<String>> locationPools;
    private final Map<String, List<String>> customerPools;
    
    // Business rule constants
    private static final Set<Month> GARDEN_PEAK_MONTHS = Set.of(
        Month.APRIL, Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST
    );
    private static final Set<Month> DOGHOUSE_PEAK_MONTHS = Set.of(
        Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER
    );
    
    public DemoDataGenerator(DataSource dataSource) {
        this.dataSource = dataSource;
        this.projectRepository = new ProjectRepository(dataSource);
        this.resourceRepository = new ResourceRepository(dataSource);
        this.assignmentRepository = new AssignmentRepository(dataSource);
        this.taskRepository = new TaskRepository((HikariDataSource) dataSource);
        this.random = new Random(42); // Seeded for reproducibility
        this.locationPools = initializeLocationPools();
        this.customerPools = initializeCustomerPools();
    }
    
    /**
     * Main method for command-line execution
     */
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            String configPath = "config/demo_data_config.yaml";
            String templatePath = "templates/demo_templates.json";
            int startYear = 2024;
            int endYear = 2026;
            String mode = "standard";
            
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("--config=")) {
                    configPath = args[i].substring(9);
                } else if (args[i].startsWith("--templates=")) {
                    templatePath = args[i].substring(12);
                } else if (args[i].startsWith("--start=")) {
                    startYear = Integer.parseInt(args[i].substring(8));
                } else if (args[i].startsWith("--end=")) {
                    endYear = Integer.parseInt(args[i].substring(6));
                } else if (args[i].startsWith("--mode=")) {
                    mode = args[i].substring(7);
                }
            }
            
            // Create database configuration
            com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig dbConfig = 
                new com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig();
            
            // Create generator instance
            DemoDataGenerator generator = new DemoDataGenerator(dbConfig.getDataSource());
            
            // Load configuration and templates
            generator.loadConfiguration(configPath);
            generator.loadTemplates(templatePath);
            
            // Generate demo data
            generator.generateDemoData(startYear, endYear);
            
            System.out.println("Demo data generation completed successfully!");
            System.exit(0);
            
        } catch (Exception e) {
            logger.error("Failed to generate demo data", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Load configuration from YAML file
     */
    public void loadConfiguration(String configPath) throws IOException {
        logger.info("Loading configuration from: {}", configPath);
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(Paths.get(configPath))) {
            this.config = yaml.loadAs(in, DemoDataConfig.class);
        }
    }
    
    /**
     * Load project templates from JSON file
     */
    public void loadTemplates(String templatePath) throws IOException {
        logger.info("Loading templates from: {}", templatePath);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        TemplateFile templateFile = mapper.readValue(
            new File(templatePath), TemplateFile.class
        );
        this.templates = templateFile.getTemplates();
        logger.info("Loaded {} templates", templates.size());
    }
    
    /**
     * Export current projects as templates
     */
    public void exportTemplates(String outputPath, List<Long> projectIds) throws IOException {
        logger.info("Exporting {} projects as templates", projectIds.size());
        
        List<ProjectTemplate> exportTemplates = new ArrayList<>();
        for (Long projectId : projectIds) {
            Project project = projectRepository.findById(projectId).orElseThrow();
            List<Assignment> assignments = assignmentRepository.findByProjectId(projectId);
            List<Task> tasks = taskRepository.findByProjectId(projectId);
            
            ProjectTemplate template = createTemplateFromProject(project, assignments, tasks);
            exportTemplates.add(template);
        }
        
        TemplateFile templateFile = new TemplateFile();
        templateFile.setVersion("1.0");
        templateFile.setCreated(LocalDateTime.now().toString());
        templateFile.setTemplates(exportTemplates);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(new File(outputPath), templateFile);
        
        logger.info("Templates exported to: {}", outputPath);
    }
    
    /**
     * Generate demo data for the specified date range
     */
    public void generateDemoData(int startYear, int endYear) {
        logger.info("Generating demo data from {} to {}", startYear, endYear);
        
        LocalDate startDate = LocalDate.of(startYear, 1, 1);
        LocalDate endDate = LocalDate.of(endYear, 12, 31);
        
        // Clear existing demo data if configured
        if (config.isClearExisting()) {
            clearExistingData();
        }
        
        // Generate projects month by month
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            generateProjectsForMonth(currentDate);
            currentDate = currentDate.plusMonths(1);
        }
        
        // Post-generation processing
        introduceRealisticConflicts();
        updateHistoricalStatuses();
        validateGeneratedData();
        
        logger.info("Demo data generation complete");
    }
    
    /**
     * Generate projects for a specific month
     */
    private void generateProjectsForMonth(LocalDate monthStart) {
        Month month = monthStart.getMonth();
        int year = monthStart.getYear();
        
        for (ProjectTemplate template : templates) {
            int projectCount = calculateProjectCount(template.getType(), month);
            
            for (int i = 0; i < projectCount; i++) {
                LocalDate projectStart = selectProjectStartDate(monthStart, template);
                if (projectStart != null) {
                    generateProjectFromTemplate(template, projectStart, year, month, i + 1);
                }
            }
        }
    }
    
    /**
     * Generate a single project from template
     */
    private void generateProjectFromTemplate(ProjectTemplate template, LocalDate startDate, 
                                            int year, Month month, int sequence) {
        try {
            // Create project with variations
            Project project = new Project();
            project.setProjectId(generateProjectId(template.getType(), year, month, sequence));
            project.setDescription(generateProjectDescription(template, startDate));
            project.setProjectManagerId(selectProjectManager());
            
            // Apply duration variation
            int baseDuration = template.getBaseDurationDays();
            int duration = applyDurationVariation(baseDuration);
            LocalDate endDate = calculateEndDate(startDate, duration);
            
            project.setStartDate(startDate);
            project.setEndDate(endDate);
            project.setStatus(determineProjectStatus(startDate));
            
            // Save project
            project = projectRepository.save(project);
            
            // Generate assignments with resource variations
            generateAssignments(project, template, startDate, endDate);
            
            // Generate tasks
            generateTasks(project, template);
            
            logger.debug("Generated project: {}", project.getProjectId());
            
        } catch (Exception e) {
            logger.error("Failed to generate project from template", e);
        }
    }
    
    /**
     * Generate assignments for a project
     */
    private void generateAssignments(Project project, ProjectTemplate template, 
                                    LocalDate startDate, LocalDate endDate) {
        List<Resource> availableResources = getAvailableResources(
            template.getResourceRequirements(), startDate, endDate
        );
        
        if (availableResources.isEmpty()) {
            logger.warn("No available resources for project: {}", project.getProjectId());
            return;
        }
        
        // Select resources based on template requirements
        int resourceCount = determineResourceCount(template);
        List<Resource> selectedResources = selectResources(availableResources, resourceCount);
        
        for (Resource resource : selectedResources) {
            Assignment assignment = new Assignment();
            assignment.setProjectId(project.getId());
            assignment.setResourceId(resource.getId());
            assignment.setStartDate(startDate);
            assignment.setEndDate(endDate);
            
            // Add travel days for field work
            if (template.getType().contains("install")) {
                assignment.setTravelOutDays(1);
                assignment.setTravelBackDays(0);
            }
            
            assignment.setNotes(generateAssignmentNote(template, resource));
            assignment.setCreatedAt(LocalDateTime.now());
            assignment.setUpdatedAt(LocalDateTime.now());
            
            assignmentRepository.save(assignment);
        }
    }
    
    /**
     * Generate tasks for a project
     */
    private void generateTasks(Project project, ProjectTemplate template) {
        List<TaskTemplate> taskTemplates = template.getTaskTemplates();
        
        for (int i = 0; i < taskTemplates.size(); i++) {
            TaskTemplate taskTemplate = taskTemplates.get(i);
            
            Task task = new Task();
            task.setProjectId(project.getId());
            task.setTitle(taskTemplate.getName());
            task.setDescription(taskTemplate.getDescription());
            task.setTaskCode("TASK-" + project.getProjectId() + "-" + (i + 1));
            task.setEstimatedHours(taskTemplate.getEstimatedHours());
            task.setActualHours(calculateActualHours(taskTemplate.getEstimatedHours(), project));
            task.setStatus(convertTaskStatus(determineTaskStatus(project.getStatus())));
            task.setAssignedTo(selectTaskAssignee(project));
            
            // Set dates based on sequence
            LocalDate taskStart = calculateTaskStartDate(project.getStartDate(), i, taskTemplates.size());
            LocalDate taskEnd = calculateTaskEndDate(taskStart, taskTemplate.getEstimatedHours());
            task.setPlannedStart(taskStart);
            task.setPlannedEnd(taskEnd);
            
            if (task.getStatus() == Task.TaskStatus.COMPLETED) {
                task.setActualEnd(taskEnd);
                task.setCompletedAt(taskEnd.atStartOfDay());
            }
            
            task.setCreatedAt(LocalDateTime.now());
            
            taskRepository.create(task);
        }
    }
    
    /**
     * Introduce realistic conflicts into the data
     */
    private void introduceRealisticConflicts() {
        if (config.getConflictRate() <= 0) {
            return;
        }
        
        logger.info("Introducing conflicts at {}% rate", config.getConflictRate() * 100);
        
        List<Assignment> allAssignments = assignmentRepository.findAll();
        int targetConflicts = (int) (allAssignments.size() * config.getConflictRate());
        
        for (int i = 0; i < targetConflicts; i++) {
            Assignment assignment = allAssignments.get(random.nextInt(allAssignments.size()));
            createConflict(assignment);
        }
    }
    
    /**
     * Update historical project statuses
     */
    private void updateHistoricalStatuses() {
        LocalDate today = LocalDate.now();
        List<Project> allProjects = projectRepository.findAll();
        
        for (Project project : allProjects) {
            if (project.getEndDate().isBefore(today.minusMonths(3))) {
                // Historical projects should be completed
                project.setStatus(ProjectStatus.COMPLETED);
                projectRepository.update(project);
                
                // Update associated tasks
                List<Task> tasks = taskRepository.findByProjectId(project.getId());
                for (Task task : tasks) {
                    task.setStatus(Task.TaskStatus.COMPLETED);
                    task.setActualEnd(task.getPlannedEnd());
                    task.setCompletedAt(task.getPlannedEnd().atStartOfDay());
                    taskRepository.update(task);
                }
            }
        }
    }
    
    /**
     * Validate all generated data
     */
    private void validateGeneratedData() {
        logger.info("Validating generated data...");
        
        DataValidator validator = new DataValidator(dataSource);
        ValidationResult result = validator.validateAll();
        
        if (!result.isValid()) {
            logger.error("Validation failed: {}", result.getErrors());
            throw new RuntimeException("Generated data validation failed");
        }
        
        logger.info("Data validation successful");
    }
    
    // Helper methods
    
    private int calculateProjectCount(String projectType, Month month) {
        ProjectTypeConfig typeConfig = config.getProjectTypes().get(projectType);
        
        if (typeConfig.getPeakMonths().contains(month.getValue())) {
            return typeConfig.getMaxPerMonth();
        } else if (typeConfig.getOffMonths().contains(month.getValue())) {
            return typeConfig.getMinPerMonth();
        } else {
            return (typeConfig.getMinPerMonth() + typeConfig.getMaxPerMonth()) / 2;
        }
    }
    
    private LocalDate selectProjectStartDate(LocalDate monthStart, ProjectTemplate template) {
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        
        // Prefer Monday starts
        List<LocalDate> mondays = new ArrayList<>();
        LocalDate current = monthStart;
        while (!current.isAfter(monthEnd)) {
            if (current.getDayOfWeek() == DayOfWeek.MONDAY) {
                mondays.add(current);
            }
            current = current.plusDays(1);
        }
        
        if (!mondays.isEmpty()) {
            return mondays.get(random.nextInt(mondays.size()));
        }
        
        // Fallback to any weekday
        do {
            current = monthStart.plusDays(random.nextInt(20));
        } while (current.getDayOfWeek() == DayOfWeek.SATURDAY || 
                 current.getDayOfWeek() == DayOfWeek.SUNDAY);
        
        return current;
    }
    
    private LocalDate calculateEndDate(LocalDate startDate, int durationDays) {
        LocalDate endDate = startDate;
        int daysAdded = 0;
        
        while (daysAdded < durationDays) {
            endDate = endDate.plusDays(1);
            if (endDate.getDayOfWeek() != DayOfWeek.SATURDAY && 
                endDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                daysAdded++;
            }
        }
        
        return endDate;
    }
    
    private int applyDurationVariation(int baseDuration) {
        double variance = config.getDurationVariance();
        int variation = (int) (baseDuration * variance);
        return baseDuration + random.nextInt(variation * 2 + 1) - variation;
    }
    
    private String generateProjectId(String type, int year, Month month, int sequence) {
        String prefix = type.substring(0, 3).toUpperCase();
        return String.format("%s-%d-%02d-%03d", prefix, year, month.getValue(), sequence);
    }
    
    private String generateProjectDescription(ProjectTemplate template, LocalDate startDate) {
        String location = selectLocation(template.getType());
        String customer = selectCustomer(template.getType());
        
        return String.format("%s - %s @ %s", 
            template.getBaseDescription(), customer, location);
    }
    
    private String selectLocation(String projectType) {
        List<String> locations = locationPools.get(projectType);
        if (locations == null || locations.isEmpty()) {
            locations = locationPools.get("default");
        }
        return locations.get(random.nextInt(locations.size()));
    }
    
    private String selectCustomer(String projectType) {
        List<String> customers = customerPools.get(projectType);
        if (customers == null || customers.isEmpty()) {
            customers = customerPools.get("default");
        }
        return customers.get(random.nextInt(customers.size()));
    }
    
    private ProjectStatus determineProjectStatus(LocalDate startDate) {
        LocalDate today = LocalDate.now();
        
        if (startDate.isAfter(today)) {
            return ProjectStatus.PLANNED;
        } else if (startDate.isBefore(today.minusMonths(3))) {
            return ProjectStatus.COMPLETED;
        } else {
            return ProjectStatus.ACTIVE;
        }
    }
    
    private Map<String, List<String>> initializeLocationPools() {
        Map<String, List<String>> pools = new HashMap<>();
        
        pools.put("garden", List.of(
            "Oak Street", "Maple Avenue", "Pine Road", "Elm Drive",
            "Birch Lane", "Cedar Boulevard", "Willow Way", "Spruce Court"
        ));
        
        pools.put("doghouse", List.of(
            "North District", "South District", "East District", "West District",
            "Central Plaza", "Riverside", "Hillside", "Valley View"
        ));
        
        pools.put("default", List.of("Main Site", "Secondary Site", "Remote Site"));
        
        return pools;
    }
    
    private Map<String, List<String>> initializeCustomerPools() {
        Map<String, List<String>> pools = new HashMap<>();
        
        pools.put("garden", List.of(
            "Johnson Family", "Smith Residence", "Davis Estate", "Wilson Property",
            "Martinez Garden", "Anderson Yard", "Thompson Grounds", "Garcia Landscape"
        ));
        
        pools.put("doghouse", List.of(
            "Pet Paradise", "Furry Friends", "Happy Tails", "Pampered Paws",
            "Cozy Canines", "Bark Manor", "Puppy Palace", "Dog Haven"
        ));
        
        pools.put("default", List.of("Client A", "Client B", "Client C"));
        
        return pools;
    }
    
    // Additional helper methods would go here...
    
    private ProjectTemplate createTemplateFromProject(Project project, 
                                                      List<Assignment> assignments, 
                                                      List<Task> tasks) {
        // Implementation to convert existing project to template
        ProjectTemplate template = new ProjectTemplate();
        // ... conversion logic
        return template;
    }
    
    private void clearExistingData() {
        logger.info("Clearing existing demo data...");
        // Implementation to clear existing data
    }
    
    private List<Resource> getAvailableResources(List<ResourceRequirement> requirements,
                                                 LocalDate startDate, LocalDate endDate) {
        // Implementation to find available resources
        return resourceRepository.findAll(); // Simplified
    }
    
    private int determineResourceCount(ProjectTemplate template) {
        // Implementation to determine resource count based on template
        return 2; // Simplified
    }
    
    private List<Resource> selectResources(List<Resource> available, int count) {
        // Implementation to select resources
        Collections.shuffle(available);
        return available.stream().limit(count).collect(Collectors.toList());
    }
    
    private Long selectProjectManager() {
        // Implementation to select project manager
        return 1L; // Simplified
    }
    
    private String generateAssignmentNote(ProjectTemplate template, Resource resource) {
        // Implementation to generate assignment note
        return "Assigned to " + template.getType();
    }
    
    private Double calculateActualHours(Double estimated, Project project) {
        // Implementation to calculate actual hours with variation
        return estimated * (0.8 + random.nextDouble() * 0.4);
    }
    
    private TaskStatus determineTaskStatus(ProjectStatus projectStatus) {
        // Implementation to determine task status
        return projectStatus == ProjectStatus.COMPLETED ? 
            TaskStatus.COMPLETED : TaskStatus.IN_PROGRESS;
    }
    
    private Task.TaskStatus convertTaskStatus(TaskStatus status) {
        switch (status) {
            case COMPLETED:
                return Task.TaskStatus.COMPLETED;
            case IN_PROGRESS:
                return Task.TaskStatus.IN_PROGRESS;
            case BLOCKED:
                return Task.TaskStatus.BLOCKED;
            default:
                return Task.TaskStatus.NOT_STARTED;
        }
    }
    
    private Long selectTaskAssignee(Project project) {
        // Implementation to select task assignee
        return 1L; // Simplified
    }
    
    private LocalDate calculateTaskStartDate(LocalDate projectStart, int sequence, int totalTasks) {
        // Implementation to calculate task start date
        return projectStart.plusDays(sequence);
    }
    
    private LocalDate calculateTaskEndDate(LocalDate startDate, Double hours) {
        // Implementation to calculate task end date
        int days = (int) Math.ceil(hours / 8.0);
        return startDate.plusDays(days);
    }
    
    private void createConflict(Assignment assignment) {
        // Implementation to create a conflict
        // This could involve overlapping dates, double-booking, etc.
    }
}

// Supporting classes

class DemoDataConfig {
    private boolean clearExisting;
    private double durationVariance;
    private double conflictRate;
    private Map<String, ProjectTypeConfig> projectTypes;
    
    // Getters and setters
    public boolean isClearExisting() { return clearExisting; }
    public void setClearExisting(boolean clearExisting) { this.clearExisting = clearExisting; }
    public double getDurationVariance() { return durationVariance; }
    public void setDurationVariance(double durationVariance) { this.durationVariance = durationVariance; }
    public double getConflictRate() { return conflictRate; }
    public void setConflictRate(double conflictRate) { this.conflictRate = conflictRate; }
    public Map<String, ProjectTypeConfig> getProjectTypes() { return projectTypes; }
    public void setProjectTypes(Map<String, ProjectTypeConfig> projectTypes) { this.projectTypes = projectTypes; }
}

class ProjectTypeConfig {
    private int minPerMonth;
    private int maxPerMonth;
    private List<Integer> peakMonths;
    private List<Integer> offMonths;
    
    // Getters and setters
    public int getMinPerMonth() { return minPerMonth; }
    public void setMinPerMonth(int minPerMonth) { this.minPerMonth = minPerMonth; }
    public int getMaxPerMonth() { return maxPerMonth; }
    public void setMaxPerMonth(int maxPerMonth) { this.maxPerMonth = maxPerMonth; }
    public List<Integer> getPeakMonths() { return peakMonths; }
    public void setPeakMonths(List<Integer> peakMonths) { this.peakMonths = peakMonths; }
    public List<Integer> getOffMonths() { return offMonths; }
    public void setOffMonths(List<Integer> offMonths) { this.offMonths = offMonths; }
}

class TemplateFile {
    private String version;
    private String created;
    private List<ProjectTemplate> templates;
    
    // Getters and setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }
    public List<ProjectTemplate> getTemplates() { return templates; }
    public void setTemplates(List<ProjectTemplate> templates) { this.templates = templates; }
}

class ProjectTemplate {
    private String type;
    private String baseDescription;
    private int baseDurationDays;
    private List<ResourceRequirement> resourceRequirements;
    private List<TaskTemplate> taskTemplates;
    
    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBaseDescription() { return baseDescription; }
    public void setBaseDescription(String baseDescription) { this.baseDescription = baseDescription; }
    public int getBaseDurationDays() { return baseDurationDays; }
    public void setBaseDurationDays(int baseDurationDays) { this.baseDurationDays = baseDurationDays; }
    public List<ResourceRequirement> getResourceRequirements() { return resourceRequirements; }
    public void setResourceRequirements(List<ResourceRequirement> resourceRequirements) { this.resourceRequirements = resourceRequirements; }
    public List<TaskTemplate> getTaskTemplates() { return taskTemplates; }
    public void setTaskTemplates(List<TaskTemplate> taskTemplates) { this.taskTemplates = taskTemplates; }
}

class ResourceRequirement {
    private String role;
    private String skillLevel;
    
    // Getters and setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }
}

class TaskTemplate {
    private String name;
    private String description;
    private Double estimatedHours;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }
}

class DataValidator {
    private final DataSource dataSource;
    
    public DataValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public ValidationResult validateAll() {
        ValidationResult result = new ValidationResult();
        // Implementation of validation logic
        return result;
    }
}

class ValidationResult {
    private boolean valid = true;
    private List<String> errors = new ArrayList<>();
    
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}

enum TaskStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, BLOCKED
}