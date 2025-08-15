package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

public class CheckTimelineProjects {
    public static void main(String[] args) {
        try {
            DatabaseConfig dbConfig = new DatabaseConfig();
            DataSource dataSource = dbConfig.getDataSource();
            
            // Create repositories
            ProjectRepository projectRepo = new ProjectRepository(dataSource);
            ResourceRepository resourceRepo = new ResourceRepository(dataSource);
            AssignmentRepository assignmentRepo = new AssignmentRepository(dataSource);
            ProjectManagerRepository pmRepo = new ProjectManagerRepository(dataSource);
            
            SchedulingService schedulingService = new SchedulingService(
                projectRepo, resourceRepo, assignmentRepo, pmRepo, (HikariDataSource) dataSource);
            
            LocalDate startDate = LocalDate.of(2025, 8, 1);
            LocalDate endDate = LocalDate.of(2025, 8, 31);
            
            System.out.println("Checking projects for August 2025 timeline display:");
            System.out.println("====================================================\n");
            
            // Get projects the same way the MainController does
            List<Project> projects = schedulingService.getProjectsByDateRange(startDate, endDate);
            
            System.out.println("Projects returned by getProjectsByDateRange (Aug 1-31, 2025):");
            System.out.println("--------------------------------------------------------------");
            System.out.println("Total count: " + projects.size() + "\n");
            
            for (Project p : projects) {
                System.out.printf("ID: %d, Project: %s\n", p.getId(), p.getProjectId());
                System.out.printf("  Description: %s\n", p.getDescription());
                System.out.printf("  Dates: %s to %s\n", p.getStartDate(), p.getEndDate());
                System.out.printf("  PM ID: %s\n", p.getProjectManagerId());
                System.out.printf("  Status: %s\n\n", p.getStatus());
            }
            
            // Check specifically for CH-PBLD-2025-091
            System.out.println("\nSpecific check for CH-PBLD-2025-091:");
            System.out.println("-------------------------------------");
            boolean found = false;
            for (Project p : projects) {
                if (p.getProjectId().equals("CH-PBLD-2025-091")) {
                    found = true;
                    System.out.println("✓ FOUND in the list!");
                    System.out.println("  Internal ID: " + p.getId());
                    break;
                }
            }
            if (!found) {
                System.out.println("✗ NOT FOUND in the list returned by getProjectsByDateRange!");
                
                // Let's check if it exists in the database at all
                List<Project> allProjects = projectRepo.findAll();
                for (Project p : allProjects) {
                    if (p.getProjectId().equals("CH-PBLD-2025-091")) {
                        System.out.println("\nBut it EXISTS in the database:");
                        System.out.printf("  ID: %d\n", p.getId());
                        System.out.printf("  Dates: %s to %s\n", p.getStartDate(), p.getEndDate());
                        System.out.printf("  Status: %s\n", p.getStatus());
                        
                        // Check date overlap logic
                        boolean overlaps = 
                            (p.getStartDate().isBefore(endDate) || p.getStartDate().isEqual(endDate)) &&
                            (p.getEndDate().isAfter(startDate) || p.getEndDate().isEqual(startDate));
                        System.out.printf("  Should overlap with Aug 2025? %s\n", overlaps);
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}