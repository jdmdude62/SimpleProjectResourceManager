package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import java.time.LocalDate;
import java.util.List;

public class DebugTimelineData {
    
    public static void main(String[] args) {
        System.out.println("=== Debug Timeline Data ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        
        try {
            // Create repositories
            ProjectRepository projectRepo = new ProjectRepository(dataSource);
            AssignmentRepository assignmentRepo = new AssignmentRepository(dataSource);
            ResourceRepository resourceRepo = new ResourceRepository(dataSource);
            
            // Check projects
            System.out.println("Projects:");
            List<Project> projects = projectRepo.findAll();
            for (Project p : projects) {
                System.out.println("  ID: " + p.getId() + 
                    ", ProjectId: " + p.getProjectId() + 
                    ", Desc: " + (p.getDescription() != null ? p.getDescription().substring(0, Math.min(30, p.getDescription().length())) : "null"));
            }
            
            // Check resources
            System.out.println("\nResources:");
            List<Resource> resources = resourceRepo.findAll();
            for (Resource r : resources) {
                System.out.println("  ID: " + r.getId() + ", Name: " + r.getName());
            }
            
            // Check assignments for January 2025
            System.out.println("\nAssignments for January 2025:");
            LocalDate jan1 = LocalDate.of(2025, 1, 1);
            LocalDate jan31 = LocalDate.of(2025, 1, 31);
            List<Assignment> assignments = assignmentRepo.findByDateRange(jan1, jan31);
            
            System.out.println("Found " + assignments.size() + " assignments:");
            for (Assignment a : assignments) {
                Resource resource = resources.stream()
                    .filter(r -> r.getId().equals(a.getResourceId()))
                    .findFirst()
                    .orElse(null);
                Project project = projects.stream()
                    .filter(p -> p.getId().equals(a.getProjectId()))
                    .findFirst()
                    .orElse(null);
                    
                System.out.println("  Assignment ID: " + a.getId() +
                    ", Project: " + (project != null ? project.getProjectId() : "null") +
                    ", Resource: " + (resource != null ? resource.getName() : "null") +
                    ", Dates: " + a.getStartDate() + " to " + a.getEndDate());
            }
            
            System.out.println("\n=== Debug Complete ===");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}