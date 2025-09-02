package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import javax.sql.DataSource;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class InspectProjectData {
    public static void main(String[] args) {
        try {
            // Initialize database
            DatabaseConfig dbConfig = new DatabaseConfig();
            DataSource dataSource = dbConfig.getDataSource();
            
            ProjectRepository projectRepo = new ProjectRepository(dataSource);
            AssignmentRepository assignmentRepo = new AssignmentRepository(dataSource);
            
            String projectId = "CH-PBLD-2025-097";
            System.out.println("=== Inspecting Project: " + projectId + " ===\n");
            
            // Get project details
            Optional<Project> projectOpt = projectRepo.findByProjectId(projectId);
            if (projectOpt.isPresent()) {
                Project project = projectOpt.get();
                System.out.println("Project Details:");
                System.out.println("  ID: " + project.getProjectId());
                System.out.println("  Description: " + project.getDescription());
                System.out.println("  Start Date: " + project.getStartDate());
                System.out.println("  End Date: " + project.getEndDate());
                System.out.println("  Duration: " + java.time.temporal.ChronoUnit.DAYS.between(
                    project.getStartDate(), project.getEndDate()) + " days");
                System.out.println("\nClient Information:");
                System.out.println("  Name: " + project.getContactName());
                System.out.println("  Company: " + project.getContactCompany());
                System.out.println("  Email: " + project.getContactEmail());
                System.out.println("  Phone: " + project.getContactPhone());
                System.out.println("  Address: " + project.getContactAddress());
                
                // Get assignments for this project
                System.out.println("\nAssignments for this project:");
                List<Assignment> assignments = assignmentRepo.findByProjectId(project.getId());
                if (assignments.isEmpty()) {
                    System.out.println("  No assignments found");
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    for (Assignment assignment : assignments) {
                        System.out.println("\n  Assignment ID: " + assignment.getId());
                        System.out.println("    Resource ID: " + assignment.getResourceId());
                        System.out.println("    Start Date: " + assignment.getStartDate().format(formatter));
                        System.out.println("    End Date: " + assignment.getEndDate().format(formatter));
                        long days = java.time.temporal.ChronoUnit.DAYS.between(
                            assignment.getStartDate(), assignment.getEndDate()) + 1;
                        System.out.println("    Duration: " + days + " days");
                        System.out.println("    Bar width would be: " + (days * 30) + " pixels");
                        System.out.println("    Address visible: " + (days >= 4 ? "YES" : "NO (needs 4+ days)"));
                    }
                }
            } else {
                System.out.println("Project not found: " + projectId);
            }
            
            // Check other projects with addresses
            System.out.println("\n=== Projects with Addresses ===");
            List<Project> allProjects = projectRepo.findAll();
            int count = 0;
            for (Project p : allProjects) {
                if (p.getContactAddress() != null && !p.getContactAddress().trim().isEmpty()) {
                    count++;
                    System.out.println("  " + p.getProjectId() + ": " + p.getContactAddress());
                }
            }
            if (count == 0) {
                System.out.println("  No projects have addresses saved yet");
            }
            
            System.out.println("\n=== Assignments 4+ Days (Address Visible) ===");
            List<Assignment> allAssignments = assignmentRepo.findAll();
            count = 0;
            for (Assignment a : allAssignments) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    a.getStartDate(), a.getEndDate()) + 1;
                if (days >= 4) {
                    count++;
                    Optional<Project> pOpt = projectRepo.findById(a.getProjectId());
                    String projectIdStr = "Unknown";
                    String address = "No address";
                    if (pOpt.isPresent()) {
                        Project p = pOpt.get();
                        projectIdStr = p.getProjectId();
                        if (p.getContactAddress() != null && !p.getContactAddress().trim().isEmpty()) {
                            address = p.getContactAddress();
                        }
                    }
                    System.out.println("  " + projectIdStr + " (" + days + " days): " + address);
                    if (count >= 5) {
                        System.out.println("  ... and more");
                        break;
                    }
                }
            }
            if (count == 0) {
                System.out.println("  No assignments are 4+ days long");
            }
            
        } catch (Exception e) {
            System.err.println("Error inspecting data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}