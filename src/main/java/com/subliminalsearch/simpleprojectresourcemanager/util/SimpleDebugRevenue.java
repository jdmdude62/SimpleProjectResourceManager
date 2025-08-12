package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectManagerRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.zaxxer.hikari.HikariDataSource;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleDebugRevenue {
    public static void main(String[] args) {
        System.out.println("=== Simple Revenue Debug ===");
        
        try {
            // Initialize database
            DatabaseConfig dbConfig = new DatabaseConfig();
            HikariDataSource dataSource = dbConfig.getDataSource();
            
            ProjectRepository projectRepo = new ProjectRepository(dataSource);
            ResourceRepository resourceRepo = new ResourceRepository(dataSource);
            AssignmentRepository assignmentRepo = new AssignmentRepository(dataSource);
            ProjectManagerRepository pmRepo = new ProjectManagerRepository(dataSource);
            
            SchedulingService schedulingService = new SchedulingService(
                projectRepo,
                resourceRepo,
                assignmentRepo,
                pmRepo,
                dataSource
            );
            
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 8, 12);
            
            System.out.println("Date range: " + startDate + " to " + endDate);
            
            // Get all projects
            List<Project> allProjects = schedulingService.getAllProjects();
            System.out.println("\nTotal projects in database: " + allProjects.size());
            
            // Count projects with budget data
            List<Project> projectsWithBudget = allProjects.stream()
                .filter(p -> p.getBudgetAmount() != null || p.getRevenueAmount() != null)
                .collect(Collectors.toList());
            System.out.println("Projects with budget/revenue data: " + projectsWithBudget.size());
            
            // Show first 5 projects with dates and budget
            System.out.println("\nFirst 5 projects with budget data:");
            projectsWithBudget.stream().limit(5).forEach(p -> {
                System.out.printf("  %s: %s to %s - Budget: $%.0f, Revenue: $%.0f%n",
                    p.getProjectId(),
                    p.getStartDate(),
                    p.getEndDate(),
                    p.getBudgetAmount() != null ? p.getBudgetAmount() : 0.0,
                    p.getRevenueAmount() != null ? p.getRevenueAmount() : 0.0
                );
            });
            
            // Filter projects by date range (same logic as RevenueReportService line 121)
            List<Project> filteredProjects = schedulingService.getAllProjects().stream()
                .filter(p -> !p.getEndDate().isBefore(startDate) && !p.getStartDate().isAfter(endDate))
                .filter(p -> p.getBudgetAmount() != null || p.getRevenueAmount() != null)
                .collect(Collectors.toList());
            
            System.out.println("\nProjects matching date filter: " + filteredProjects.size());
            
            // Calculate totals
            double totalRevenue = filteredProjects.stream()
                .mapToDouble(p -> p.getRevenueAmount() != null ? p.getRevenueAmount() : 0)
                .sum();
            
            double totalBudget = filteredProjects.stream()
                .mapToDouble(p -> p.getBudgetAmount() != null ? p.getBudgetAmount() : 0)
                .sum();
            
            double totalCost = filteredProjects.stream()
                .mapToDouble(p -> p.getTotalCost() != null ? p.getTotalCost() : 0)
                .sum();
            
            System.out.println("\n=== Financial Totals ===");
            System.out.printf("Total Revenue: $%,.0f%n", totalRevenue);
            System.out.printf("Total Budget: $%,.0f%n", totalBudget);
            System.out.printf("Total Cost: $%,.0f%n", totalCost);
            
            // Show filtered projects
            if (!filteredProjects.isEmpty()) {
                System.out.println("\nFiltered projects (first 10):");
                filteredProjects.stream().limit(10).forEach(p -> {
                    System.out.printf("  %s: %s to %s - Revenue: $%.0f%n",
                        p.getProjectId(),
                        p.getStartDate(),
                        p.getEndDate(),
                        p.getRevenueAmount() != null ? p.getRevenueAmount() : 0.0
                    );
                });
            }
            
            // Check for 2024 projects specifically
            List<Project> projects2024 = allProjects.stream()
                .filter(p -> p.getStartDate() != null && p.getStartDate().getYear() == 2024)
                .filter(p -> p.getBudgetAmount() != null || p.getRevenueAmount() != null)
                .collect(Collectors.toList());
            
            System.out.println("\n2024 projects with budget data: " + projects2024.size());
            if (!projects2024.isEmpty()) {
                System.out.println("First 5 2024 projects:");
                projects2024.stream().limit(5).forEach(p -> {
                    System.out.printf("  %s: %s - Revenue: $%.0f%n",
                        p.getProjectId(),
                        p.getStartDate(),
                        p.getRevenueAmount() != null ? p.getRevenueAmount() : 0.0
                    );
                });
            }
            
            System.out.println("\n=== Debug Complete ===");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("Error during debug: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}