package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.service.RevenueReportService;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class TestRevenueReportDirectly {
    public static void main(String[] args) {
        System.out.println("=== Testing Revenue Report Directly ===");
        
        try {
            // Initialize database
            DatabaseConfig dbConfig = new DatabaseConfig();
            HikariDataSource dataSource = dbConfig.getDataSource();
            
            // Create repositories
            ProjectRepository projectRepo = new ProjectRepository(dataSource);
            ResourceRepository resourceRepo = new ResourceRepository(dataSource);
            AssignmentRepository assignmentRepo = new AssignmentRepository(dataSource);
            ProjectManagerRepository pmRepo = new ProjectManagerRepository(dataSource);
            
            // Create scheduling service
            SchedulingService schedulingService = new SchedulingService(
                projectRepo,
                resourceRepo,
                assignmentRepo,
                pmRepo,
                dataSource
            );
            
            // Get all projects and check their data
            List<Project> allProjects = schedulingService.getAllProjects();
            System.out.println("Total projects in database: " + allProjects.size());
            
            // Count projects with budget data
            long projectsWithBudget = allProjects.stream()
                .filter(p -> p.getBudgetAmount() != null || p.getRevenueAmount() != null)
                .count();
            System.out.println("Projects with budget/revenue data: " + projectsWithBudget);
            
            // Show sample projects
            System.out.println("\nSample projects with financial data:");
            allProjects.stream()
                .filter(p -> p.getBudgetAmount() != null || p.getRevenueAmount() != null)
                .limit(5)
                .forEach(p -> {
                    System.out.printf("  %s: Budget=$%.0f, Revenue=$%.0f, TotalCost=$%.0f%n",
                        p.getProjectId(),
                        p.getBudgetAmount(),
                        p.getRevenueAmount(),
                        p.getTotalCost());
                });
            
            // Calculate totals
            double totalRevenue = allProjects.stream()
                .filter(p -> p.getRevenueAmount() != null)
                .mapToDouble(Project::getRevenueAmount)
                .sum();
            
            double totalBudget = allProjects.stream()
                .filter(p -> p.getBudgetAmount() != null)
                .mapToDouble(Project::getBudgetAmount)
                .sum();
            
            System.out.printf("\nTotal Revenue across all projects: $%,.0f%n", totalRevenue);
            System.out.printf("Total Budget across all projects: $%,.0f%n", totalBudget);
            
            // Now test the revenue report service
            System.out.println("\n=== Testing RevenueReportService ===");
            RevenueReportService reportService = new RevenueReportService(schedulingService);
            
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 12, 31);
            
            System.out.println("Generating report for date range: " + startDate + " to " + endDate);
            
            try {
                File reportFile = reportService.generateReport(startDate, endDate);
                System.out.println("Report generated successfully: " + reportFile.getAbsolutePath());
                System.out.println("Report file size: " + reportFile.length() + " bytes");
                
                // Clean up temp file
                if (reportFile.exists() && reportFile.getName().startsWith("revenue_budget_")) {
                    reportFile.delete();
                    System.out.println("Cleaned up temp report file");
                }
            } catch (Exception e) {
                System.err.println("Failed to generate report: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Shutdown the data source
            dataSource.close();
            System.out.println("\n=== Test Complete ===");
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}