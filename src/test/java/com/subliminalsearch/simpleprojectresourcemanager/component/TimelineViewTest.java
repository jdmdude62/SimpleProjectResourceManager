package com.subliminalsearch.simpleprojectresourcemanager.component;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TimelineViewTest extends ApplicationTest {
    
    private TimelineView timelineView;
    
    @BeforeAll
    static void setupJavaFX() {
        if (!Platform.isFxApplicationThread()) {
            Platform.startup(() -> {});
        }
    }
    
    @BeforeEach
    void setUp() {
        Platform.runLater(() -> {
            timelineView = new TimelineView();
        });
        
        // Wait for JavaFX thread to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    void testInitialization() {
        Platform.runLater(() -> {
            assertNotNull(timelineView);
            assertNotNull(timelineView.getProjects());
            assertNotNull(timelineView.getResources());
            assertNotNull(timelineView.getAssignments());
        });
        
        // Wait for JavaFX thread to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    void testDateRangeSettings() {
        LocalDate start = LocalDate.of(2025, 8, 1);
        LocalDate end = LocalDate.of(2025, 8, 31);
        
        Platform.runLater(() -> {
            timelineView.setDateRange(start, end);
            assertEquals(start, timelineView.startDateProperty().get());
            assertEquals(end, timelineView.endDateProperty().get());
        });
        
        // Wait for JavaFX thread to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    void testDataBinding() {
        Platform.runLater(() -> {
            // Create test data
            Project project = new Project("TEST-001", "Test Project", 
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
            project.setId(1L);
            
            ResourceType resourceType = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
            Resource resource = new Resource("Test User", "test@example.com", resourceType);
            resource.setId(1L);
            
            Assignment assignment = new Assignment(1L, 1L, 
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 10));
            assignment.setId(1L);
            
            // Add data to timeline
            timelineView.getProjects().add(project);
            timelineView.getResources().add(resource);
            timelineView.getAssignments().add(assignment);
            
            // Verify data was added
            assertEquals(1, timelineView.getProjects().size());
            assertEquals(1, timelineView.getResources().size());
            assertEquals(1, timelineView.getAssignments().size());
            
            assertEquals("TEST-001", timelineView.getProjects().get(0).getProjectId());
            assertEquals("Test User", timelineView.getResources().get(0).getName());
        });
        
        // Wait for JavaFX thread to complete
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    void testInvalidDateRange() {
        LocalDate start = LocalDate.of(2025, 8, 15);
        LocalDate end = LocalDate.of(2025, 8, 1); // End before start
        
        Platform.runLater(() -> {
            // This should not update the date range
            timelineView.setDateRange(start, end);
            
            // The date range should not have been set to invalid values
            LocalDate currentStart = timelineView.startDateProperty().get();
            LocalDate currentEnd = timelineView.endDateProperty().get();
            
            // Should either be null or a valid range (not the invalid one we tried to set)
            if (currentStart != null && currentEnd != null) {
                assertTrue(currentStart.isBefore(currentEnd) || currentStart.isEqual(currentEnd));
            }
        });
        
        // Wait for JavaFX thread to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}