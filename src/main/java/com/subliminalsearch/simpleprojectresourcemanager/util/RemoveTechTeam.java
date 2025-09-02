package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveTechTeam {
    
    public static void main(String[] args) {
        // Force non-GUI mode
        System.setProperty("javafx.headless", "true");
        System.setProperty("java.awt.headless", "true");
        
        DatabaseConfig dbConfig = new DatabaseConfig();
        ResourceRepository resourceRepo = new ResourceRepository(dbConfig.getDataSource());
        
        try {
            // Get all resources
            List<Resource> allResources = resourceRepo.findAll();
            
            // Filter Tech Team resources
            List<Resource> techTeamResources = allResources.stream()
                .filter(r -> r.getName().startsWith("Tech Team"))
                .collect(Collectors.toList());
            
            if (techTeamResources.isEmpty()) {
                System.out.println("No Tech Team resources found.");
            } else {
                System.out.println("Found " + techTeamResources.size() + " Tech Team resources:");
                for (Resource r : techTeamResources) {
                    System.out.println("  - " + r.getName() + " (ID: " + r.getId() + ", Active: " + r.isActive() + ")");
                }
                
                // Delete them
                System.out.println("\nDeleting Tech Team resources...");
                for (Resource r : techTeamResources) {
                    resourceRepo.delete(r.getId());
                    System.out.println("  Deleted: " + r.getName());
                }
                
                System.out.println("\nSuccessfully deleted " + techTeamResources.size() + " Tech Team resources.");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConfig.shutdown();
        }
    }
}