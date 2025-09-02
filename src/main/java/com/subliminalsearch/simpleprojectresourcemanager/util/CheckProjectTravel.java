package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.zaxxer.hikari.HikariDataSource;

import java.util.List;
import java.util.Optional;

public class CheckProjectTravel {
    public static void main(String[] args) throws Exception {
        // Initialize database
        DatabaseConfig dbConfig = new DatabaseConfig();
        HikariDataSource dataSource = dbConfig.getDataSource();
        ProjectRepository projectRepo = new ProjectRepository(dataSource);
        
        // Check existing projects
        System.out.println("Checking travel field values in database:");
        System.out.println("=========================================");
        
        List<Project> projects = projectRepo.findAll();
        int travelCount = 0;
        int nonTravelCount = 0;
        
        System.out.println("\nSample of projects with '25' prefix:");
        int shown = 0;
        for (Project p : projects) {
            if (p.getProjectId().startsWith("25")) {
                if (p.isTravel()) travelCount++;
                else nonTravelCount++;
                
                // Show first 10 for detail
                if (shown < 10) {
                    System.out.printf("  %s: Travel = %s (ID=%d)%n", p.getProjectId(), p.isTravel() ? "YES" : "NO", p.getId());
                    shown++;
                }
            }
        }
        
        System.out.println("\nSummary of '25' prefixed projects:");
        System.out.printf("  Travel projects: %d%n", travelCount);
        System.out.printf("  Non-travel projects: %d%n", nonTravelCount);
        
        // Test updating a project's travel field
        System.out.println("\n=== Testing Travel Field Update ===");
        Optional<Project> testProj = projects.stream()
            .filter(p -> p.getProjectId().startsWith("25"))
            .findFirst();
            
        if (testProj.isPresent()) {
            Project project = testProj.get();
            System.out.println("Test project: " + project.getProjectId() + " (ID=" + project.getId() + ")");
            System.out.println("Current travel value: " + project.isTravel());
            
            // Toggle the value
            boolean newValue = !project.isTravel();
            project.setTravel(newValue);
            System.out.println("Setting travel to: " + newValue);
            
            // Update in database
            projectRepo.update(project);
            System.out.println("Updated in database");
            
            // Read it back
            Optional<Project> reloaded = projectRepo.findById(project.getId());
            if (reloaded.isPresent()) {
                System.out.println("After reload from DB: " + reloaded.get().isTravel());
                
                if (reloaded.get().isTravel() == newValue) {
                    System.out.println("✓ Update successful!");
                } else {
                    System.out.println("✗ Update failed - value didn't persist!");
                }
                
                // Toggle back to original
                project.setTravel(!newValue);
                projectRepo.update(project);
                System.out.println("Restored original value: " + project.isTravel());
            }
        } else {
            System.out.println("No projects with '25' prefix found to test");
        }
        
        dataSource.close();
        System.out.println("\nDone.");
    }
}