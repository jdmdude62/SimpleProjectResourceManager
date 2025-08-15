package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectManagerRepository;

import javax.sql.DataSource;
import java.util.List;

public class CheckPMIssue {
    public static void main(String[] args) {
        try {
            System.out.println("Checking Project Manager Issue...\n");
            
            // Initialize database
            DatabaseConfig dbConfig = new DatabaseConfig();
            DataSource dataSource = dbConfig.getDataSource();
            ProjectRepository projectRepo = new ProjectRepository(dataSource);
            ProjectManagerRepository pmRepo = new ProjectManagerRepository(dataSource);
            
            // Get all project managers
            List<ProjectManager> managers = pmRepo.findAll();
            System.out.println("Project Managers in Database:");
            System.out.println("==============================");
            for (ProjectManager pm : managers) {
                System.out.println("ID: " + pm.getId() + ", Name: " + pm.getName() + ", Email: " + pm.getEmail());
            }
            
            // Check for Paula Poodle
            ProjectManager paula = managers.stream()
                .filter(pm -> pm.getName().contains("Paula"))
                .findFirst()
                .orElse(null);
                
            if (paula != null) {
                System.out.println("\n✓ Found Paula Poodle with ID: " + paula.getId());
                
                // Check projects assigned to Paula
                List<Project> allProjects = projectRepo.findAll();
                System.out.println("\nProjects assigned to Paula Poodle:");
                System.out.println("===================================");
                for (Project p : allProjects) {
                    if (p.getProjectManagerId() != null && p.getProjectManagerId().equals(paula.getId())) {
                        System.out.println("- " + p.getProjectId() + ": " + p.getDescription());
                    }
                }
            } else {
                System.out.println("\n✗ Paula Poodle NOT found in project_managers table!");
            }
            
            // Check the filter dropdown values
            System.out.println("\nAll Project Manager Names (for filter dropdown):");
            System.out.println("================================================");
            System.out.println("\"All Managers\" (hardcoded option)");
            for (ProjectManager pm : managers) {
                System.out.println("\"" + pm.getName() + "\"");
            }
            
            System.out.println("\n✓ Check complete!");
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // DataSource will be closed automatically
        }
    }
}