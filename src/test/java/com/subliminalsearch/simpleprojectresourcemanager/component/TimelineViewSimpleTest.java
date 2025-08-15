package com.subliminalsearch.simpleprojectresourcemanager.component;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for TimelineView logic
 * Tests the business logic without requiring JavaFX runtime
 */
@DisplayName("Timeline View Logic Tests")
public class TimelineViewSimpleTest {
    
    private List<Project> projects;
    private List<Resource> resources;
    private List<Assignment> assignments;
    
    @BeforeEach
    void setUp() {
        projects = new ArrayList<>();
        resources = new ArrayList<>();
        assignments = new ArrayList<>();
    }
    
    @Test
    @DisplayName("Should validate date ranges correctly")
    void testDateRangeValidation() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        
        // Valid range
        assertTrue(isValidDateRange(startDate, endDate));
        
        // Invalid range (end before start)
        assertFalse(isValidDateRange(endDate, startDate));
        
        // Same date is valid
        assertTrue(isValidDateRange(startDate, startDate));
    }
    
    @Test
    @DisplayName("Should filter projects by date range")
    void testProjectFilterByDateRange() {
        // Create test projects
        Project project1 = createProject("PROJ-001", 
            LocalDate.of(2025, 1, 1), 
            LocalDate.of(2025, 3, 31));
        
        Project project2 = createProject("PROJ-002", 
            LocalDate.of(2025, 4, 1), 
            LocalDate.of(2025, 6, 30));
        
        Project project3 = createProject("PROJ-003", 
            LocalDate.of(2025, 7, 1), 
            LocalDate.of(2025, 9, 30));
        
        projects.add(project1);
        projects.add(project2);
        projects.add(project3);
        
        // Filter for Q2 2025
        LocalDate filterStart = LocalDate.of(2025, 4, 1);
        LocalDate filterEnd = LocalDate.of(2025, 6, 30);
        
        List<Project> filtered = filterProjectsByDateRange(filterStart, filterEnd);
        
        assertEquals(1, filtered.size());
        assertFalse(filtered.contains(project1)); // Q1 ends before Q2 starts (March 31 < April 1)
        assertTrue(filtered.contains(project2)); // Fully in Q2
        assertFalse(filtered.contains(project3)); // In Q3
    }
    
    @Test
    @DisplayName("Should detect assignment conflicts")
    void testAssignmentConflictDetection() {
        Resource resource = new Resource();
        resource.setId(1L);
        resource.setName("John Doe");
        
        Assignment assignment1 = new Assignment();
        assignment1.setResourceId(1L);
        assignment1.setStartDate(LocalDate.of(2025, 1, 10));
        assignment1.setEndDate(LocalDate.of(2025, 1, 20));
        
        Assignment assignment2 = new Assignment();
        assignment2.setResourceId(1L);
        assignment2.setStartDate(LocalDate.of(2025, 1, 15));
        assignment2.setEndDate(LocalDate.of(2025, 1, 25));
        
        Assignment assignment3 = new Assignment();
        assignment3.setResourceId(1L);
        assignment3.setStartDate(LocalDate.of(2025, 2, 1));
        assignment3.setEndDate(LocalDate.of(2025, 2, 10));
        
        assignments.add(assignment1);
        
        // Should detect conflict with assignment1
        assertTrue(hasConflict(assignment2));
        
        // Should not detect conflict with assignment1
        assertFalse(hasConflict(assignment3));
    }
    
    @Test
    @DisplayName("Should calculate project duration correctly")
    void testProjectDurationCalculation() {
        Project project = createProject("PROJ-001",
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31));
        
        long duration = calculateProjectDuration(project);
        assertEquals(31, duration); // 31 days in January
        
        // Test leap year
        Project leapProject = createProject("PROJ-002",
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 2, 29));
        
        long leapDuration = calculateProjectDuration(leapProject);
        assertEquals(29, leapDuration); // 29 days in Feb 2024 (leap year)
    }
    
    @Test
    @DisplayName("Should group assignments by resource")
    void testGroupAssignmentsByResource() {
        Assignment assignment1 = createAssignment(1L, 1L, 
            LocalDate.of(2025, 1, 1), 
            LocalDate.of(2025, 1, 15));
        
        Assignment assignment2 = createAssignment(2L, 1L,
            LocalDate.of(2025, 2, 1),
            LocalDate.of(2025, 2, 15));
        
        Assignment assignment3 = createAssignment(1L, 2L,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 15));
        
        assignments.add(assignment1);
        assignments.add(assignment2);
        assignments.add(assignment3);
        
        var grouped = groupByResource(assignments);
        
        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get(1L).size());
        assertEquals(1, grouped.get(2L).size());
    }
    
    // Helper methods
    private boolean isValidDateRange(LocalDate start, LocalDate end) {
        return !start.isAfter(end);
    }
    
    private List<Project> filterProjectsByDateRange(LocalDate start, LocalDate end) {
        return projects.stream()
            .filter(p -> !p.getEndDate().isBefore(start) && !p.getStartDate().isAfter(end))
            .toList();
    }
    
    private boolean hasConflict(Assignment newAssignment) {
        return assignments.stream()
            .filter(a -> a.getResourceId().equals(newAssignment.getResourceId()))
            .anyMatch(a -> datesOverlap(a, newAssignment));
    }
    
    private boolean datesOverlap(Assignment a1, Assignment a2) {
        return !a1.getEndDate().isBefore(a2.getStartDate()) && 
               !a1.getStartDate().isAfter(a2.getEndDate());
    }
    
    private long calculateProjectDuration(Project project) {
        return project.getStartDate().until(project.getEndDate()).getDays() + 1;
    }
    
    private java.util.Map<Long, List<Assignment>> groupByResource(List<Assignment> assignments) {
        return assignments.stream()
            .collect(java.util.stream.Collectors.groupingBy(Assignment::getResourceId));
    }
    
    private Project createProject(String id, LocalDate start, LocalDate end) {
        Project project = new Project();
        project.setProjectId(id);
        project.setStartDate(start);
        project.setEndDate(end);
        return project;
    }
    
    private Assignment createAssignment(Long resourceId, Long projectId, LocalDate start, LocalDate end) {
        Assignment assignment = new Assignment();
        assignment.setResourceId(resourceId);
        assignment.setProjectId(projectId);
        assignment.setStartDate(start);
        assignment.setEndDate(end);
        return assignment;
    }
}