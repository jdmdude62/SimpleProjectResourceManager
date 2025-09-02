package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectManagerRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceUnavailabilityRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class SchedulingService {
    private static final Logger logger = LoggerFactory.getLogger(SchedulingService.class);
    
    private final ProjectRepository projectRepository;
    private final ResourceRepository resourceRepository;
    private final AssignmentRepository assignmentRepository;
    private final ProjectManagerRepository projectManagerRepository;
    private final TaskRepository taskRepository;
    private ResourceUnavailabilityRepository unavailabilityRepository;
    private final HikariDataSource dataSource;

    public SchedulingService(ProjectRepository projectRepository, 
                           ResourceRepository resourceRepository,
                           AssignmentRepository assignmentRepository,
                           ProjectManagerRepository projectManagerRepository,
                           HikariDataSource dataSource) {
        this.projectRepository = projectRepository;
        this.resourceRepository = resourceRepository;
        this.assignmentRepository = assignmentRepository;
        this.projectManagerRepository = projectManagerRepository;
        this.taskRepository = new TaskRepository(dataSource);
        this.dataSource = dataSource;
        // Initialize unavailability repository lazily to avoid issues in tests
        this.unavailabilityRepository = null;
    }
    
    private ResourceUnavailabilityRepository getUnavailabilityRepository() {
        if (unavailabilityRepository == null && dataSource != null) {
            unavailabilityRepository = new ResourceUnavailabilityRepository(dataSource);
        }
        return unavailabilityRepository;
    }
    
    // Getter methods for repositories (needed for dialogs)
    public ProjectRepository getProjectRepository() {
        return projectRepository;
    }
    
    public ResourceRepository getResourceRepository() {
        return resourceRepository;
    }
    
    public AssignmentRepository getAssignmentRepository() {
        return assignmentRepository;
    }
    
    public ProjectManagerRepository getProjectManagerRepository() {
        return projectManagerRepository;
    }
    
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    // Project Management
    public Project createProject(String projectId, String description, LocalDate startDate, LocalDate endDate) {
        validateProjectDates(startDate, endDate);
        
        // No longer checking for duplicate project IDs - they are allowed now
        // Multiple projects can have the same project ID (for different phases/locations)
        // The database ID (auto-increment) is the unique identifier
        
        Project project = new Project(projectId, description, startDate, endDate);
        Project saved = projectRepository.save(project);
        
        logger.info("Created project: {} - {} (ID: {})", projectId, description, saved.getId());
        return saved;
    }

    public void updateProject(Project project) {
        if (!projectRepository.existsById(project.getId())) {
            throw new IllegalArgumentException("Project not found: " + project.getId());
        }
        
        validateProjectDates(project.getStartDate(), project.getEndDate());
        
        // Check if date changes affect existing assignments
        List<Assignment> assignments = assignmentRepository.findByProjectId(project.getId());
        for (Assignment assignment : assignments) {
            if (assignment.getStartDate().isBefore(project.getStartDate()) ||
                assignment.getEndDate().isAfter(project.getEndDate())) {
                throw new IllegalArgumentException(
                    "Cannot update project dates: assignments exist outside the new date range");
            }
        }
        
        logger.info("About to update project {} with travel={}", project.getProjectId(), project.isTravel());
        projectRepository.update(project);
        logger.info("Updated project: {} with travel={}", project.getProjectId(), project.isTravel());
    }

    public void deleteProject(Long projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }
        
        // Check for existing assignments
        List<Assignment> assignments = assignmentRepository.findByProjectId(projectId);
        if (!assignments.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot delete project: " + assignments.size() + " assignments exist. Please delete assignments first.");
        }
        
        // Check for tasks
        List<com.subliminalsearch.simpleprojectresourcemanager.model.Task> tasks = taskRepository.findByProjectId(projectId);
        if (!tasks.isEmpty()) {
            // Delete tasks first (cascading)
            for (com.subliminalsearch.simpleprojectresourcemanager.model.Task task : tasks) {
                taskRepository.delete(task.getId());
            }
            logger.info("Deleted {} tasks for project {}", tasks.size(), projectId);
        }
        
        projectRepository.delete(projectId);
        logger.info("Deleted project: {}", project.get().getProjectId());
    }
    
    public void deleteResource(Long resourceId) {
        Optional<Resource> resource = resourceRepository.findById(resourceId);
        if (resource.isEmpty()) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
        
        // Check for existing assignments
        List<Assignment> assignments = assignmentRepository.findByResourceId(resourceId);
        if (!assignments.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot delete resource: " + assignments.size() + " assignments exist. Please delete assignments first.");
        }
        
        // Delete resource skills and certifications (cascading)
        // These should cascade automatically with foreign key constraints
        
        resourceRepository.delete(resourceId);
        logger.info("Deleted resource: {}", resource.get().getName());
    }
    
    public void deleteProjectWithAssignments(Long projectId) {
        logger.info("Attempting to delete project with ID: {}", projectId);
        
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is null");
        }
        
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            logger.error("Project not found in repository with ID: {}", projectId);
            throw new IllegalArgumentException("Failed to find project with ID: " + projectId);
        }
        
        logger.info("Found project to delete: {}", project.get().getProjectId());
        
        // First delete all assignments for this project
        List<Assignment> assignments = assignmentRepository.findByProjectId(projectId);
        int assignmentCount = assignments.size();
        for (Assignment assignment : assignments) {
            assignmentRepository.delete(assignment.getId());
        }
        if (assignmentCount > 0) {
            logger.info("Deleted {} assignments for project {}", assignmentCount, project.get().getProjectId());
        }
        
        // Delete tasks
        List<com.subliminalsearch.simpleprojectresourcemanager.model.Task> tasks = taskRepository.findByProjectId(projectId);
        for (com.subliminalsearch.simpleprojectresourcemanager.model.Task task : tasks) {
            taskRepository.delete(task.getId());
        }
        if (!tasks.isEmpty()) {
            logger.info("Deleted {} tasks for project {}", tasks.size(), projectId);
        }
        
        // Now delete the project
        projectRepository.delete(projectId);
        logger.info("Deleted project: {} (with {} assignments)", project.get().getProjectId(), assignmentCount);
    }

    // Project Manager Management
    public ProjectManager createProjectManager(String name, String email, String phone, String department) {
        ProjectManager manager = new ProjectManager(name, email, phone, department);
        return projectManagerRepository.create(manager);
    }
    
    public List<ProjectManager> getAllProjectManagers() {
        return projectManagerRepository.findAll();
    }
    
    public List<ProjectManager> getActiveProjectManagers() {
        return projectManagerRepository.findAllActive();
    }
    
    public Optional<ProjectManager> getProjectManagerById(Long id) {
        return projectManagerRepository.findById(id);
    }
    
    public ProjectManager updateProjectManager(ProjectManager manager) {
        return projectManagerRepository.update(manager);
    }
    
    public void deleteProjectManager(Long id) {
        // Update all projects with this manager to "Unassigned"
        List<Project> projects = projectRepository.findAll();
        ProjectManager unassigned = projectManagerRepository.findByName("Unassigned").orElse(null);
        if (unassigned != null) {
            for (Project project : projects) {
                if (project.getProjectManagerId() != null && project.getProjectManagerId().equals(id)) {
                    project.setProjectManagerId(unassigned.getId());
                    projectRepository.update(project);
                }
            }
        }
        projectManagerRepository.delete(id);
    }
    
    // Resource Management
    public Resource createResource(String name, String email, ResourceType resourceType) {
        if (email != null && resourceRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Resource with email '" + email + "' already exists");
        }
        
        Resource resource = new Resource(name, email, resourceType);
        Resource saved = resourceRepository.save(resource);
        
        logger.info("Created resource: {} - {}", name, resourceType);
        return saved;
    }
    
    public Resource updateResource(Resource resource) {
        if (resource.getId() == null) {
            throw new IllegalArgumentException("Cannot update resource without ID");
        }
        
        Optional<Resource> existing = resourceRepository.findById(resource.getId());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Resource not found: " + resource.getId());
        }
        
        Resource updated = resourceRepository.save(resource);
        logger.info("Updated resource: {} - {}", updated.getName(), updated.getResourceType());
        return updated;
    }

    // Assignment Management
    public Assignment createAssignment(Long projectId, Long resourceId, LocalDate startDate, LocalDate endDate) {
        return createAssignment(projectId, resourceId, startDate, endDate, 0, 0);
    }

    public Assignment createAssignment(Long projectId, Long resourceId, LocalDate startDate, LocalDate endDate, 
                                     int travelOutDays, int travelBackDays) {
        validateAssignmentInputs(projectId, resourceId, startDate, endDate, travelOutDays, travelBackDays);
        
        Assignment assignment = new Assignment(projectId, resourceId, startDate, endDate, travelOutDays, travelBackDays);
        
        // Check if we should auto-remove SHOP assignments
        Project assignmentProject = projectRepository.findById(projectId).orElse(null);
        if (assignmentProject != null && !assignmentProject.getProjectId().equalsIgnoreCase("SHOP")) {
            // This is a real project assignment, remove any overlapping SHOP assignments
            removeOverlappingShopAssignments(resourceId, assignment.getEffectiveStartDate(), assignment.getEffectiveEndDate());
        }
        
        // Validate business rules
        validateAssignmentBusinessRules(assignment);
        
        Assignment saved = assignmentRepository.save(assignment);
        
        logger.info("Created assignment: project={}, resource={}, dates={} to {}", 
            projectId, resourceId, startDate, endDate);
        return saved;
    }

    public Assignment createAssignmentWithOverride(Long projectId, Long resourceId, LocalDate startDate, LocalDate endDate,
                                                 int travelOutDays, int travelBackDays, String overrideReason) {
        validateAssignmentInputs(projectId, resourceId, startDate, endDate, travelOutDays, travelBackDays);
        
        Assignment assignment = new Assignment(projectId, resourceId, startDate, endDate, travelOutDays, travelBackDays);
        assignment.setOverride(true);
        assignment.setOverrideReason(overrideReason);
        
        // Still validate project dates but skip conflict checks
        validateAssignmentProjectDates(assignment);
        
        Assignment saved = assignmentRepository.save(assignment);
        
        logger.warn("Created assignment with override: project={}, resource={}, reason={}", 
            projectId, resourceId, overrideReason);
        return saved;
    }

    public void updateAssignment(Assignment assignment) {
        if (!assignmentRepository.existsById(assignment.getId())) {
            throw new IllegalArgumentException("Assignment not found: " + assignment.getId());
        }
        
        // Check if we should auto-remove SHOP assignments when updating
        Project assignmentProject = projectRepository.findById(assignment.getProjectId()).orElse(null);
        if (assignmentProject != null && !assignmentProject.getProjectId().equalsIgnoreCase("SHOP")) {
            // This is a real project assignment, remove any overlapping SHOP assignments
            // But exclude the current assignment from removal check
            removeOverlappingShopAssignmentsExcluding(assignment.getResourceId(), 
                assignment.getEffectiveStartDate(), assignment.getEffectiveEndDate(), assignment.getId());
        }
        
        // If not an override, validate business rules
        if (!assignment.isOverride()) {
            validateAssignmentBusinessRules(assignment);
        } else {
            // Even with override, validate project dates
            validateAssignmentProjectDates(assignment);
        }
        
        assignmentRepository.update(assignment);
        logger.info("Updated assignment: {}", assignment.getId());
    }

    public void deleteAssignment(Long assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new IllegalArgumentException("Assignment not found: " + assignmentId);
        }
        
        assignmentRepository.delete(assignmentId);
        logger.info("Deleted assignment: {}", assignmentId);
    }

    // Query Methods
    public List<Project> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        
        // Debug: Log travel values when projects are loaded
        if (!projects.isEmpty()) {
            Project first = projects.get(0);
            logger.info("getAllProjects: First project {} has travel={}", 
                first.getProjectId(), first.isTravel());
        }
        
        return projects;
    }

    public List<Project> getActiveProjects() {
        return projectRepository.findByStatus(ProjectStatus.ACTIVE);
    }

    public List<Project> getProjectsByDateRange(LocalDate startDate, LocalDate endDate) {
        return projectRepository.findByDateRange(startDate, endDate);
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public List<Resource> getActiveResources() {
        return resourceRepository.findActiveResources();
    }

    public List<Resource> getResourcesByCategory(ResourceCategory category) {
        return resourceRepository.findByCategory(category);
    }

    public List<Assignment> getAssignmentsByProject(Long projectId) {
        return assignmentRepository.findByProjectId(projectId);
    }

    public List<Assignment> getAssignmentsByResource(Long resourceId) {
        return assignmentRepository.findByResourceId(resourceId);
    }

    public List<Assignment> getAssignmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return assignmentRepository.findByDateRange(startDate, endDate);
    }
    
    public List<Assignment> getAssignmentsByProjectId(Long projectId) {
        return assignmentRepository.findByProjectId(projectId);
    }
    
    public List<Assignment> getAssignmentsByResourceId(Long resourceId) {
        return assignmentRepository.findByResourceId(resourceId);
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public Optional<Project> getProjectByProjectId(String projectId) {
        return projectRepository.findByProjectId(projectId);
    }

    public Optional<Resource> getResourceById(Long id) {
        return resourceRepository.findById(id);
    }

    public Optional<Assignment> getAssignmentById(Long id) {
        return assignmentRepository.findById(id);
    }

    // Conflict Detection
    public boolean hasResourceConflicts(Long resourceId, LocalDate startDate, LocalDate endDate) {
        List<Assignment> overlapping = assignmentRepository.findOverlappingAssignments(resourceId, startDate, endDate);
        return !overlapping.isEmpty();
    }

    public List<Assignment> getConflictingAssignments(Long resourceId, LocalDate startDate, LocalDate endDate) {
        return assignmentRepository.findOverlappingAssignments(resourceId, startDate, endDate);
    }

    public boolean isResourceAvailable(Long resourceId, LocalDate startDate, LocalDate endDate) {
        // Check for overlapping assignments (excluding overrides)
        List<Assignment> conflicts = getConflictingAssignments(resourceId, startDate, endDate);
        boolean hasAssignmentConflict = conflicts.stream().anyMatch(a -> !a.isOverride());
        
        // Check for resource unavailability (vacation, sick leave, etc.)
        List<TechnicianUnavailability> unavailabilities = getUnavailabilityRepository() != null ? 
            getUnavailabilityRepository().findOverlapping(resourceId, startDate, endDate) : 
            new ArrayList<>();
        boolean hasUnavailability = !unavailabilities.isEmpty();
        
        if (hasUnavailability) {
            logger.debug("Resource {} is unavailable from {} to {} due to: {}", 
                resourceId, startDate, endDate, 
                unavailabilities.stream()
                    .map(u -> u.getType().getDisplayName())
                    .collect(Collectors.joining(", ")));
        }
        
        return !hasAssignmentConflict && !hasUnavailability;
    }

    // Resource Unavailability Management
    public TechnicianUnavailability createUnavailability(Long resourceId, UnavailabilityType type, 
                                                        LocalDate startDate, LocalDate endDate, String reason) {
        // Validate resource exists
        if (!resourceRepository.existsById(resourceId)) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
        
        // Validate dates
        validateProjectDates(startDate, endDate);
        
        // Check for existing assignments during this period
        List<Assignment> conflictingAssignments = getConflictingAssignments(resourceId, startDate, endDate);
        if (!conflictingAssignments.isEmpty()) {
            logger.warn("Resource {} has {} existing assignments during unavailability period", 
                resourceId, conflictingAssignments.size());
        }
        
        TechnicianUnavailability unavailability = new TechnicianUnavailability(resourceId, type, startDate, endDate);
        unavailability.setReason(reason);
        
        return getUnavailabilityRepository() != null ? getUnavailabilityRepository().save(unavailability) : unavailability;
    }
    
    public List<TechnicianUnavailability> getResourceUnavailabilities(Long resourceId) {
        return getUnavailabilityRepository() != null ? getUnavailabilityRepository().findByResourceId(resourceId) : new ArrayList<>();
    }
    
    public List<TechnicianUnavailability> getUnavailabilitiesInDateRange(LocalDate startDate, LocalDate endDate) {
        return getUnavailabilityRepository() != null ? getUnavailabilityRepository().findByDateRange(startDate, endDate) : new ArrayList<>();
    }
    
    public void approveUnavailability(Long unavailabilityId, String approvedBy) {
        if (getUnavailabilityRepository() != null) {
            getUnavailabilityRepository().approveUnavailability(unavailabilityId, approvedBy);
        }
    }
    
    public void deleteUnavailability(Long unavailabilityId) {
        if (getUnavailabilityRepository() != null) {
            getUnavailabilityRepository().delete(unavailabilityId);
        }
    }
    
    public List<TechnicianUnavailability> getPendingUnavailabilities() {
        return getUnavailabilityRepository() != null ? getUnavailabilityRepository().findPendingApproval() : new ArrayList<>();
    }
    
    // Method to remove SHOP assignments that conflict with a real project assignment
    private void removeOverlappingShopAssignments(Long resourceId, LocalDate startDate, LocalDate endDate) {
        removeOverlappingShopAssignmentsExcluding(resourceId, startDate, endDate, null);
    }
    
    // Method to remove SHOP assignments, excluding a specific assignment ID (used when updating)
    private void removeOverlappingShopAssignmentsExcluding(Long resourceId, LocalDate startDate, LocalDate endDate, Long excludeAssignmentId) {
        // Find the SHOP project
        List<Project> allProjects = projectRepository.findAll();
        Project shopProject = allProjects.stream()
            .filter(p -> p.getProjectId().equalsIgnoreCase("SHOP"))
            .findFirst()
            .orElse(null);
        
        if (shopProject == null) {
            // No SHOP project exists, nothing to remove
            return;
        }
        
        // Get all assignments for this resource in the date range
        List<Assignment> resourceAssignments = assignmentRepository.findByResourceId(resourceId);
        
        int removedCount = 0;
        for (Assignment assignment : resourceAssignments) {
            // Skip if this is the assignment we're updating
            if (excludeAssignmentId != null && assignment.getId().equals(excludeAssignmentId)) {
                continue;
            }
            
            // Check if this is a SHOP assignment
            if (assignment.getProjectId().equals(shopProject.getId())) {
                // Check if it overlaps with the new assignment period
                LocalDate assignmentStart = assignment.getStartDate();
                LocalDate assignmentEnd = assignment.getEndDate();
                
                boolean overlaps = !(assignmentEnd.isBefore(startDate) || assignmentStart.isAfter(endDate));
                
                if (overlaps) {
                    // Remove this SHOP assignment
                    assignmentRepository.delete(assignment.getId());
                    removedCount++;
                    logger.info("Removed overlapping SHOP assignment ID {} for resource {} (dates: {} to {})", 
                        assignment.getId(), resourceId, assignmentStart, assignmentEnd);
                }
            }
        }
        
        if (removedCount > 0) {
            logger.info("Automatically removed {} SHOP assignments that conflicted with new project assignment", removedCount);
        }
    }
    
    // SHOP Auto-Assignment Methods
    public int deleteShopAssignments(Project shopProject, List<Resource> selectedResources) {
        if (shopProject == null) {
            return 0;
        }
        
        int deletedCount = 0;
        
        // Get all SHOP assignments for selected resources
        List<Assignment> shopAssignments = assignmentRepository.findByProjectId(shopProject.getId());
        
        // Filter by selected resources if provided
        if (selectedResources != null && !selectedResources.isEmpty()) {
            Set<Long> resourceIds = selectedResources.stream()
                .map(Resource::getId)
                .collect(Collectors.toSet());
            
            shopAssignments = shopAssignments.stream()
                .filter(a -> resourceIds.contains(a.getResourceId()))
                .collect(Collectors.toList());
        }
        
        // Delete the assignments
        for (Assignment assignment : shopAssignments) {
            assignmentRepository.delete(assignment.getId());
            deletedCount++;
            logger.debug("Deleted SHOP assignment ID {} for resource {}", 
                assignment.getId(), assignment.getResourceId());
        }
        
        logger.info("Deleted {} SHOP assignments", deletedCount);
        return deletedCount;
    }
    
    public int autoAssignShopTime(Project shopProject, LocalDate startDate, LocalDate endDate, 
                                   List<Resource> selectedResources, boolean skipHolidays, boolean excludeWeekends) {
        logger.info("Starting SHOP auto-assignment: project={}, startDate={}, endDate={}, resources={}, skipHolidays={}, excludeWeekends={}", 
            shopProject != null ? shopProject.getProjectId() : "null", 
            startDate, endDate, 
            selectedResources != null ? selectedResources.size() : 0,
            skipHolidays, excludeWeekends);
            
        if (shopProject == null || !shopProject.getProjectId().equalsIgnoreCase("SHOP")) {
            logger.error("Invalid SHOP project: {}", shopProject != null ? shopProject.getProjectId() : "null");
            throw new IllegalArgumentException("Must select a SHOP project");
        }
        
        int assignmentsCreated = 0;
        List<LocalDate> weekdays = getWeekdays(startDate, endDate, skipHolidays, excludeWeekends);
        logger.info("Processing {} weekdays for SHOP assignments", weekdays.size());
        
        for (Resource resource : selectedResources) {
            logger.debug("Processing resource: {} (ID: {}, Active: {})", resource.getName(), resource.getId(), resource.isActive());
            if (!resource.isActive()) {
                logger.debug("Skipping inactive resource: {}", resource.getName());
                continue; // Skip inactive resources
            }
            
            // Get existing assignments and unavailability for this resource
            List<Assignment> existingAssignments = assignmentRepository.findByResourceId(resource.getId());
            List<TechnicianUnavailability> unavailabilities = getUnavailabilityRepository() != null ?
                getUnavailabilityRepository().findByResourceId(resource.getId()).stream()
                    .filter(u -> u.isApproved())
                    .collect(Collectors.toList()) : new ArrayList<>();
            
            // Group consecutive available weekdays into blocks
            List<LocalDate> availableDays = new ArrayList<>();
            LocalDate blockStart = null;
            LocalDate blockEnd = null;
            int resourceAssignments = 0;
            
            for (int i = 0; i <= weekdays.size(); i++) {
                LocalDate currentDate = i < weekdays.size() ? weekdays.get(i) : null;
                boolean isAvailable = false;
                boolean shouldEndBlock = false;
                
                if (currentDate != null) {
                    // Check if resource has any assignment on this date
                    boolean hasAssignment = existingAssignments.stream()
                        .anyMatch(a -> !currentDate.isBefore(a.getStartDate()) && !currentDate.isAfter(a.getEndDate()));
                    
                    // Check if resource is unavailable on this date
                    boolean isUnavailable = unavailabilities.stream()
                        .anyMatch(u -> !currentDate.isBefore(u.getStartDate()) && !currentDate.isAfter(u.getEndDate()));
                    
                    isAvailable = !hasAssignment && !isUnavailable;
                    
                    if (hasAssignment) {
                        logger.trace("Resource {} already has assignment on {}", resource.getName(), currentDate);
                    }
                    if (isUnavailable) {
                        logger.trace("Resource {} is unavailable on {}", resource.getName(), currentDate);
                    }
                    
                    // Check if there's a gap (weekend or holiday) between previous date and current date
                    if (blockEnd != null && ChronoUnit.DAYS.between(blockEnd, currentDate) > 1) {
                        shouldEndBlock = true; // Gap detected, end current block
                    }
                }
                
                if (shouldEndBlock || (!isAvailable && blockStart != null) || currentDate == null) {
                    // End of available block - create assignment if we have a block
                    if (blockStart != null && blockEnd != null) {
                        // Create SHOP assignment for this block
                        Assignment shopAssignment = new Assignment(
                            shopProject.getId(),
                            resource.getId(),
                            blockStart,
                            blockEnd,
                            0, 0   // No travel days for SHOP
                        );
                        shopAssignment.setNotes("Auto-assigned to SHOP");
                        shopAssignment.setLocation("Shop Floor");
                        Assignment saved = assignmentRepository.save(shopAssignment);
                        assignmentsCreated++;
                        resourceAssignments++;
                        
                        long dayCount = ChronoUnit.DAYS.between(blockStart, blockEnd) + 1;
                        logger.info("Created SHOP assignment for {} from {} to {} ({} days, Assignment ID: {})", 
                            resource.getName(), blockStart, blockEnd, dayCount, 
                            saved != null ? saved.getId() : "null");
                        
                        blockStart = null;
                        blockEnd = null;
                    }
                }
                
                if (isAvailable && currentDate != null) {
                    // Start new block or continue current block
                    if (blockStart == null) {
                        blockStart = currentDate;
                        blockEnd = currentDate;
                    } else {
                        blockEnd = currentDate;
                    }
                }
            }
            
            logger.info("Created {} SHOP assignment blocks for resource {}", resourceAssignments, resource.getName());
        }
        
        logger.info("SHOP auto-assignment complete: created {} total assignments", assignmentsCreated);
        return assignmentsCreated;
    }
    
    private List<LocalDate> getWeekdays(LocalDate startDate, LocalDate endDate, boolean skipHolidays, boolean excludeWeekends) {
        List<LocalDate> weekdays = new ArrayList<>();
        LocalDate current = startDate;
        
        logger.info("Getting assignable days from {} to {}, skipHolidays={}, excludeWeekends={}", 
            startDate, endDate, skipHolidays, excludeWeekends);
        
        int skippedWeekends = 0;
        int skippedHolidays = 0;
        
        while (!current.isAfter(endDate)) {
            // Skip weekends if requested
            if (excludeWeekends && (current.getDayOfWeek().getValue() > 5)) { // Saturday = 6, Sunday = 7
                logger.debug("Skipping weekend day: {} ({})", current, current.getDayOfWeek());
                skippedWeekends++;
                current = current.plusDays(1);
                continue;
            }
            
            // Skip holidays if requested
            if (skipHolidays && isCompanyHoliday(current)) {
                logger.debug("Skipping holiday: {}", current);
                skippedHolidays++;
                current = current.plusDays(1);
                continue;
            }
            
            weekdays.add(current);
            current = current.plusDays(1);
        }
        
        logger.info("Returning {} assignable days (skipped {} weekends, {} holidays)", 
            weekdays.size(), skippedWeekends, skippedHolidays);
        return weekdays;
    }
    
    private boolean isCompanyHoliday(LocalDate date) {
        // Check if the date is a company holiday from the Holiday Calendar
        try {
            String sql = "SELECT COUNT(*) FROM company_holidays WHERE date = ? AND active = 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, date.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to check company holidays for date {}: {}", date, e.getMessage());
            // Fall back to no holiday if database check fails
        }
        return false;
    }
    
    // Utility Methods
    public int getProjectCount() {
        return (int) projectRepository.count();
    }

    public int getResourceCount() {
        return (int) resourceRepository.count();
    }

    public int getAssignmentCount() {
        return (int) assignmentRepository.count();
    }

    // Private Validation Methods
    private void validateProjectDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }

    private void validateAssignmentInputs(Long projectId, Long resourceId, LocalDate startDate, LocalDate endDate,
                                        int travelOutDays, int travelBackDays) {
        if (projectId == null || resourceId == null) {
            throw new IllegalArgumentException("Project ID and Resource ID are required");
        }
        
        validateProjectDates(startDate, endDate);
        
        if (travelOutDays < 0 || travelBackDays < 0) {
            throw new IllegalArgumentException("Travel days cannot be negative");
        }
        
        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }
        
        if (!resourceRepository.existsById(resourceId)) {
            throw new IllegalArgumentException("Resource not found: " + resourceId);
        }
    }

    private void validateAssignmentBusinessRules(Assignment assignment) {
        // Validate project date boundaries
        validateAssignmentProjectDates(assignment);
        
        // Get the project for this assignment to check its project ID
        Project assignmentProject = projectRepository.findById(assignment.getProjectId()).orElse(null);
        if (assignmentProject == null) {
            throw new IllegalArgumentException("Project not found: " + assignment.getProjectId());
        }
        
        // Check for resource conflicts
        List<Assignment> conflicts = getConflictingAssignments(
            assignment.getResourceId(), 
            assignment.getEffectiveStartDate(), 
            assignment.getEffectiveEndDate()
        );
        
        // Filter out the current assignment if updating
        conflicts = conflicts.stream()
            .filter(a -> !a.getId().equals(assignment.getId()))
            .filter(a -> !a.isOverride()) // Ignore other overrides
            .toList();
        
        // Check for actual conflicts (excluding SHOP assignments which are auto-removed)
        boolean hasRealConflicts = false;
        for (Assignment conflict : conflicts) {
            Project conflictProject = projectRepository.findById(conflict.getProjectId()).orElse(null);
            if (conflictProject != null) {
                // Check if it's the same PROJECT ID
                if (conflictProject.getProjectId().equals(assignmentProject.getProjectId())) {
                    // Same PROJECT ID - this is not allowed
                    Resource resource = resourceRepository.findById(assignment.getResourceId()).orElse(null);
                    String resourceName = resource != null ? resource.getName() : "Resource #" + assignment.getResourceId();
                    throw new IllegalArgumentException(
                        String.format("Resource conflict: %s is already assigned to project '%s' during this period. " +
                                      "A resource cannot be assigned to the same project ID multiple times with overlapping dates.",
                                      resourceName, assignmentProject.getProjectId()));
                }
                // Check if the conflict is with a non-SHOP project
                if (!conflictProject.getProjectId().equalsIgnoreCase("SHOP")) {
                    hasRealConflicts = true;
                }
            }
        }
        
        // If there are conflicts with non-SHOP projects, report them
        if (hasRealConflicts && !assignmentProject.getProjectId().equalsIgnoreCase("SHOP")) {
            Resource resource = resourceRepository.findById(assignment.getResourceId()).orElse(null);
            String resourceName = resource != null ? resource.getName() : "Resource #" + assignment.getResourceId();
            
            // Build list of conflicting projects
            StringBuilder conflictDetails = new StringBuilder();
            for (Assignment conflict : conflicts) {
                Project conflictProject = projectRepository.findById(conflict.getProjectId()).orElse(null);
                if (conflictProject != null && !conflictProject.getProjectId().equalsIgnoreCase("SHOP")) {
                    if (conflictDetails.length() > 0) conflictDetails.append(", ");
                    conflictDetails.append(String.format("'%s' (%s to %s)", 
                        conflictProject.getProjectId(), conflict.getStartDate(), conflict.getEndDate()));
                }
            }
            
            throw new IllegalArgumentException(
                String.format("Resource conflict: %s is already assigned to the following projects during this period: %s",
                              resourceName, conflictDetails.toString()));
        }
        
        // If we get here, conflicts exist but they're for different project IDs, which is now allowed
        // This enables scenarios like working on "ProjectA Phase 1" and "ProjectA Phase 2" simultaneously
    }

    private void validateAssignmentProjectDates(Assignment assignment) {
        // Assignments can extend beyond project dates for legitimate business reasons:
        // - Setup/preparation work before project starts
        // - Teardown/cleanup work after project ends  
        // - Documentation and knowledge transfer
        // - Follow-up activities
        // Therefore, we don't enforce project date boundaries on assignments
    }
    
    // Comprehensive Conflict Detection Methods
    public Set<Long> detectAllConflicts(LocalDate startDate, LocalDate endDate) {
        List<Assignment> assignments = getAssignmentsByDateRange(startDate, endDate);
        Set<Long> conflictedAssignments = new HashSet<>();
        
        // Group assignments by resource
        var assignmentsByResource = assignments.stream()
            .collect(Collectors.groupingBy(Assignment::getResourceId));
        
        // Check each resource for overlapping assignments
        for (Long resourceId : assignmentsByResource.keySet()) {
            List<Assignment> resourceAssignments = assignmentsByResource.get(resourceId);
            Set<Long> conflicts = findOverlappingAssignments(resourceAssignments);
            conflictedAssignments.addAll(conflicts);
        }
        
        logger.debug("Found {} conflicted assignments in date range {} to {}", 
                    conflictedAssignments.size(), startDate, endDate);
        return conflictedAssignments;
    }
    
    public Set<Long> findOverlappingAssignments(List<Assignment> assignments) {
        Set<Long> conflicts = new HashSet<>();
        
        for (int i = 0; i < assignments.size(); i++) {
            Assignment assignment1 = assignments.get(i);
            
            for (int j = i + 1; j < assignments.size(); j++) {
                Assignment assignment2 = assignments.get(j);
                
                if (assignmentsOverlap(assignment1, assignment2)) {
                    conflicts.add(assignment1.getId());
                    conflicts.add(assignment2.getId());
                    
                    logger.debug("Conflict detected between assignments {} and {} for resource {}", 
                               assignment1.getId(), assignment2.getId(), assignment1.getResourceId());
                }
            }
        }
        
        return conflicts;
    }
    
    public boolean assignmentsOverlap(Assignment a1, Assignment a2) {
        // Check if two assignments have overlapping date ranges
        return !a1.getEndDate().isBefore(a2.getStartDate()) && 
               !a2.getEndDate().isBefore(a1.getStartDate());
    }
    
    public boolean hasConflicts(Long assignmentId) {
        Optional<Assignment> assignment = assignmentRepository.findById(assignmentId);
        if (assignment.isEmpty()) {
            return false;
        }
        
        List<Assignment> conflicts = getConflictingAssignments(
            assignment.get().getResourceId(),
            assignment.get().getStartDate(),
            assignment.get().getEndDate()
        );
        
        // Remove the assignment itself from conflicts
        return conflicts.stream().anyMatch(a -> !a.getId().equals(assignmentId));
    }
}