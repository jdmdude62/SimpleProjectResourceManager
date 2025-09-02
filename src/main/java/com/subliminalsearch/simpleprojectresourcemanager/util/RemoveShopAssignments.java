package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveShopAssignments {
    
    public static void main(String[] args) {
        System.setProperty("javafx.headless", "true");
        System.setProperty("java.awt.headless", "true");
        
        DatabaseConfig dbConfig = new DatabaseConfig();
        AssignmentRepository assignmentRepo = new AssignmentRepository(dbConfig.getDataSource());
        ProjectRepository projectRepo = new ProjectRepository(dbConfig.getDataSource());
        
        try {
            // Find SHOP project
            List<Project> shopProjects = projectRepo.findAll().stream()
                .filter(p -> "SHOP".equalsIgnoreCase(p.getProjectId()))
                .collect(Collectors.toList());
            
            if (shopProjects.isEmpty()) {
                System.out.println("No SHOP project found.");
                return;
            }
            
            for (Project shopProject : shopProjects) {
                System.out.println("Found SHOP project: " + shopProject.getProjectId() + " (ID: " + shopProject.getId() + ")");
                
                // Get all assignments for this SHOP project
                List<Assignment> shopAssignments = assignmentRepo.findByProjectId(shopProject.getId());
                
                System.out.println("Found " + shopAssignments.size() + " SHOP assignments");
                
                if (!shopAssignments.isEmpty()) {
                    // Delete them
                    System.out.println("\nDeleting SHOP assignments...");
                    for (Assignment assignment : shopAssignments) {
                        assignmentRepo.delete(assignment.getId());
                        System.out.println("  Deleted assignment ID " + assignment.getId() + 
                            " (Resource: " + assignment.getResourceId() + 
                            ", Dates: " + assignment.getStartDate() + " to " + assignment.getEndDate() + ")");
                    }
                    
                    System.out.println("\nSuccessfully deleted " + shopAssignments.size() + " SHOP assignments.");
                    System.out.println("You can now use Auto-Assign SHOP to create weekly blocks.");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConfig.shutdown();
        }
    }
}