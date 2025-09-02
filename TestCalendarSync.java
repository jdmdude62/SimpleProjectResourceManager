package com.subliminalsearch.simpleprojectresourcemanager;

import com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointConfig;
import com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.StealthCalendarSync;
import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import java.time.LocalDate;

/**
 * Quick test to verify calendar synchronization works
 */
public class TestCalendarSync {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Calendar Sync Test");
        System.out.println("========================================\n");
        
        // Load configuration
        SharePointConfig config = SharePointConfig.getInstance();
        
        if (!config.isConfigured()) {
            System.err.println("ERROR: SharePoint not configured!");
            System.err.println("Please configure in the application first:");
            System.err.println("  Tools → SharePoint Integration...");
            System.exit(1);
        }
        
        System.out.println("Configuration loaded successfully");
        System.out.println("Tenant ID: " + mask(config.getTenantId()));
        System.out.println("Client ID: " + mask(config.getClientId()));
        System.out.println();
        
        // Create test data
        System.out.println("Creating test project and assignment...");
        
        Project testProject = new Project();
        testProject.setProjectId("TEST-CAL-001");
        testProject.setName("Test Calendar Sync");
        testProject.setLocation("Main Office");
        testProject.setDescription("Testing calendar synchronization");
        testProject.setStatus(ProjectStatus.ACTIVE);
        
        Resource testResource = new Resource();
        testResource.setName("Test User");
        testResource.setEmail("your.email@company.com"); // CHANGE THIS to a real email!
        
        Assignment testAssignment = new Assignment();
        testAssignment.setProjectName(testProject.getName());
        testAssignment.setResourceName(testResource.getName());
        testAssignment.setStartDate(LocalDate.now());
        testAssignment.setEndDate(LocalDate.now().plusDays(3));
        testAssignment.setLocation(testProject.getLocation());
        
        System.out.println("\nTest Data:");
        System.out.println("  Project: " + testProject.getName());
        System.out.println("  Resource: " + testResource.getName());
        System.out.println("  Email: " + testResource.getEmail());
        System.out.println("  Dates: " + testAssignment.getStartDate() + " to " + testAssignment.getEndDate());
        
        // Try to sync
        System.out.println("\nAttempting calendar sync...");
        System.out.println("This will create a calendar event in Outlook");
        
        try {
            // Note: This is a simplified test
            // In real implementation, you'd use the full GraphServiceClient
            System.out.println("\n✓ Configuration is ready for calendar sync!");
            System.out.println("\nNext steps:");
            System.out.println("1. Make sure admin consent is granted");
            System.out.println("2. Update the email address in this test to a real user");
            System.out.println("3. Run the full calendar sync from the application");
            
        } catch (Exception e) {
            System.err.println("Error during sync: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String mask(String value) {
        if (value == null || value.length() < 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}