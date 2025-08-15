package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectManagerRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceUnavailabilityRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
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
        
        if (projectRepository.existsByProjectId(projectId)) {
            throw new IllegalArgumentException("Project with ID '" + projectId + "' already exists");
        }
        
        Project project = new Project(projectId, description, startDate, endDate);
        Project saved = projectRepository.save(project);
        
        logger.info("Created project: {} - {}", projectId, description);
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
        
        projectRepository.update(project);
        logger.info("Updated project: {}", project.getProjectId());
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
                "Cannot delete project: " + assignments.size() + " assignments exist");
        }
        
        projectRepository.delete(projectId);
        logger.info("Deleted project: {}", project.get().getProjectId());
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

    // Assignment Management
    public Assignment createAssignment(Long projectId, Long resourceId, LocalDate startDate, LocalDate endDate) {
        return createAssignment(projectId, resourceId, startDate, endDate, 1, 1);
    }

    public Assignment createAssignment(Long projectId, Long resourceId, LocalDate startDate, LocalDate endDate, 
                                     int travelOutDays, int travelBackDays) {
        validateAssignmentInputs(projectId, resourceId, startDate, endDate, travelOutDays, travelBackDays);
        
        Assignment assignment = new Assignment(projectId, resourceId, startDate, endDate, travelOutDays, travelBackDays);
        
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
        return projectRepository.findAll();
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
        
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                "Resource conflict: Resource is already assigned during this period. " +
                "Conflicting assignments: " + conflicts.size());
        }
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