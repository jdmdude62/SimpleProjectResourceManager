package com.subliminalsearch.simpleprojectresourcemanager.data;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class SampleDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SampleDataGenerator.class);
    
    public static void generateSampleData(SchedulingService schedulingService) {
        logger.info("Generating sample data for demonstration...");
        
        try {
            // Get project managers (they should be created by the migration)
            // IDs: 1=Unassigned, 2=David Thompson, 3=Maria Garcia, 4=James Wilson, 5=Jennifer Lee
            
            // Create sample projects spanning May through September 2025
            Project factoryUpgrade = schedulingService.createProject(
                "FACT-001", 
                "Factory Floor Upgrade - Phase 1", 
                LocalDate.of(2025, 5, 5), 
                LocalDate.of(2025, 6, 30)
            );
            factoryUpgrade.setProjectManagerId(2L); // David Thompson - Engineering
            schedulingService.updateProject(factoryUpgrade);
            
            Project hvacMaintenance = schedulingService.createProject(
                "HVAC-002", 
                "HVAC System Overhaul", 
                LocalDate.of(2025, 5, 12), 
                LocalDate.of(2025, 5, 28)
            );
            hvacMaintenance.setProjectManagerId(3L); // Maria Garcia - Operations
            schedulingService.updateProject(hvacMaintenance);
            
            Project networkInstall = schedulingService.createProject(
                "NET-003", 
                "Corporate Network Infrastructure", 
                LocalDate.of(2025, 6, 1), 
                LocalDate.of(2025, 7, 15)
            );
            networkInstall.setProjectManagerId(5L); // Jennifer Lee - Technology
            schedulingService.updateProject(networkInstall);
            
            Project tunnelVerification = schedulingService.createProject(
                "TUNN-004", 
                "Speed Tunnels Installation & Testing", 
                LocalDate.of(2025, 6, 20), 
                LocalDate.of(2025, 8, 5)
            );
            tunnelVerification.setProjectManagerId(4L); // James Wilson - Infrastructure
            schedulingService.updateProject(tunnelVerification);
            
            Project lightUpgrade = schedulingService.createProject(
                "LIGHT-005", 
                "Smart Traffic Light System Upgrade", 
                LocalDate.of(2025, 7, 10), 
                LocalDate.of(2025, 8, 20)
            );
            lightUpgrade.setProjectManagerId(4L); // James Wilson - Infrastructure
            schedulingService.updateProject(lightUpgrade);
            
            Project warehouseRetrofit = schedulingService.createProject(
                "WARE-006", 
                "Warehouse Automation Retrofit", 
                LocalDate.of(2025, 8, 1), 
                LocalDate.of(2025, 9, 30)
            );
            warehouseRetrofit.setProjectManagerId(3L); // Maria Garcia - Operations
            schedulingService.updateProject(warehouseRetrofit);
            
            Project solarInstall = schedulingService.createProject(
                "SOLAR-007", 
                "Solar Panel Installation Phase 2", 
                LocalDate.of(2025, 8, 15), 
                LocalDate.of(2025, 9, 25)
            );
            solarInstall.setProjectManagerId(2L); // David Thompson - Engineering
            schedulingService.updateProject(solarInstall);
            
            // Create sample resource types
            ResourceType fullTimeInternal = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
            ResourceType partTimeInternal = new ResourceType("Part-Time Employee", ResourceCategory.INTERNAL);
            ResourceType contractor = new ResourceType("Contractor", ResourceCategory.CONTRACTOR);
            ResourceType vendor = new ResourceType("Vendor", ResourceCategory.VENDOR);
            
            // Create sample resources
            Resource johnDoe = schedulingService.createResource("John Smith", "john.smith@company.com", fullTimeInternal);
            Resource sarahJones = schedulingService.createResource("Sarah Johnson", "sarah.johnson@company.com", fullTimeInternal);
            Resource mikeWilson = schedulingService.createResource("Mike Wilson", "mike.wilson@company.com", partTimeInternal);
            Resource aceContractors = schedulingService.createResource("ACE Contractors", "contact@acecontractors.com", contractor);
            Resource techVendor = schedulingService.createResource("TechSolution Vendor", "info@techsolution.com", vendor);
            Resource elecContractor = schedulingService.createResource("Elite Electric", "service@eliteelectric.com", contractor);
            Resource bobMartin = schedulingService.createResource("Bob Martin", "bob.martin@company.com", fullTimeInternal);
            Resource lisaChen = schedulingService.createResource("Lisa Chen", "lisa.chen@company.com", fullTimeInternal);
            
            // Create sample assignments across May-September
            
            // Factory Floor Upgrade - May to June
            schedulingService.createAssignment(
                factoryUpgrade.getId(), 
                johnDoe.getId(), 
                LocalDate.of(2025, 5, 5), 
                LocalDate.of(2025, 5, 30), 
                1, 1 // 1 travel day before and after
            );
            
            schedulingService.createAssignment(
                factoryUpgrade.getId(), 
                aceContractors.getId(), 
                LocalDate.of(2025, 5, 20), 
                LocalDate.of(2025, 6, 25), 
                2, 1 // 2 travel days before, 1 after
            );
            
            // HVAC Maintenance - May
            schedulingService.createAssignment(
                hvacMaintenance.getId(), 
                sarahJones.getId(), 
                LocalDate.of(2025, 5, 12), 
                LocalDate.of(2025, 5, 28), 
                0, 0 // No travel days
            );
            
            // Network Installation - June to July
            schedulingService.createAssignment(
                networkInstall.getId(), 
                techVendor.getId(), 
                LocalDate.of(2025, 6, 1), 
                LocalDate.of(2025, 6, 30), 
                1, 0 // 1 travel day before
            );
            
            schedulingService.createAssignment(
                networkInstall.getId(), 
                mikeWilson.getId(), 
                LocalDate.of(2025, 6, 15), 
                LocalDate.of(2025, 7, 15), 
                0, 0 // No travel days
            );
            
            // Speed Tunnels - June to August
            schedulingService.createAssignment(
                tunnelVerification.getId(), 
                johnDoe.getId(), 
                LocalDate.of(2025, 6, 20), 
                LocalDate.of(2025, 7, 15), 
                1, 1 // Travel days
            );
            
            schedulingService.createAssignment(
                tunnelVerification.getId(), 
                bobMartin.getId(), 
                LocalDate.of(2025, 7, 1), 
                LocalDate.of(2025, 8, 5), 
                0, 0 // No travel days
            );
            
            // Traffic Light Upgrade - July to August
            schedulingService.createAssignment(
                lightUpgrade.getId(), 
                elecContractor.getId(), 
                LocalDate.of(2025, 7, 10), 
                LocalDate.of(2025, 8, 10), 
                1, 1 // Travel days
            );
            
            schedulingService.createAssignment(
                lightUpgrade.getId(), 
                sarahJones.getId(), 
                LocalDate.of(2025, 7, 20), 
                LocalDate.of(2025, 8, 20), 
                0, 0 // No travel days
            );
            
            // Warehouse Retrofit - August to September
            schedulingService.createAssignment(
                warehouseRetrofit.getId(), 
                johnDoe.getId(), 
                LocalDate.of(2025, 8, 1), 
                LocalDate.of(2025, 8, 31), 
                1, 1 // Travel days
            );
            
            schedulingService.createAssignment(
                warehouseRetrofit.getId(), 
                lisaChen.getId(), 
                LocalDate.of(2025, 8, 15), 
                LocalDate.of(2025, 9, 30), 
                0, 0 // No travel days
            );
            
            // Solar Installation - August to September
            schedulingService.createAssignment(
                solarInstall.getId(), 
                aceContractors.getId(), 
                LocalDate.of(2025, 8, 15), 
                LocalDate.of(2025, 9, 15), 
                2, 2 // Travel days
            );
            
            schedulingService.createAssignment(
                solarInstall.getId(), 
                mikeWilson.getId(), 
                LocalDate.of(2025, 8, 20), 
                LocalDate.of(2025, 9, 25), 
                0, 0 // No travel days
            );
            
            // Add a deliberate conflict for testing - Sarah Jones double-booked in August
            schedulingService.createAssignmentWithOverride(
                warehouseRetrofit.getId(), 
                sarahJones.getId(), // Same resource as traffic light project
                LocalDate.of(2025, 8, 15),  // Overlapping dates
                LocalDate.of(2025, 8, 25), 
                0, 0, // No travel days
                "Emergency support needed - approved override"
            );
            
            logger.info("Sample data generation completed successfully");
            logger.info("Created {} projects, {} resources, {} assignments", 
                schedulingService.getProjectCount(),
                schedulingService.getResourceCount(), 
                schedulingService.getAssignmentCount()
            );
            
        } catch (Exception e) {
            logger.error("Error generating sample data", e);
        }
    }
}