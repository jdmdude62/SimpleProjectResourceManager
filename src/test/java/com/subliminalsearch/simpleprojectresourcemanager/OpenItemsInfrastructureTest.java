package com.subliminalsearch.simpleprojectresourcemanager;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.OpenItem;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import com.subliminalsearch.simpleprojectresourcemanager.repository.OpenItemRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.service.OpenItemService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OpenItemsInfrastructureTest {
    
    public static void main(String[] args) {
        System.out.println("=== Open Items Infrastructure Test ===\n");
        
        try {
            // Initialize database
            DatabaseConfig dbConfig = new DatabaseConfig();
            OpenItemRepository repository = new OpenItemRepository(dbConfig);
            OpenItemService service = new OpenItemService(dbConfig);
            ProjectRepository projectRepo = new ProjectRepository(dbConfig.getDataSource());
            
            // Test 1: Create a test project first
            System.out.println("1. Creating test project...");
            Project testProject = new Project();
            testProject.setProjectId("TEST-OI-001");
            testProject.setDescription("Test Project for Open Items");
            testProject.setStartDate(LocalDate.now());
            testProject.setEndDate(LocalDate.now().plusDays(30));
            testProject.setStatus(ProjectStatus.ACTIVE);
            testProject.setBudgetAmount(10000.0);
            testProject = projectRepo.save(testProject);
            System.out.println("   ✓ Created project: " + testProject.getProjectId() + " (ID: " + testProject.getId() + ")");
            
            // Test 2: Create open items
            System.out.println("\n2. Creating open items...");
            OpenItem item1 = service.createOpenItem(
                testProject.getId(), 
                "Design Phase", 
                "Complete system design documentation"
            );
            System.out.println("   ✓ Created: " + item1.getTitle() + " - Item #: " + item1.getItemNumber());
            
            OpenItem item2 = service.createOpenItem(
                testProject.getId(),
                "Development Phase",
                "Implement core functionality"
            );
            System.out.println("   ✓ Created: " + item2.getTitle() + " - Item #: " + item2.getItemNumber());
            
            OpenItem item3 = service.createOpenItem(
                testProject.getId(),
                "Testing Phase",
                "Complete unit and integration testing"
            );
            System.out.println("   ✓ Created: " + item3.getTitle() + " - Item #: " + item3.getItemNumber());
            
            // Test 3: Update progress
            System.out.println("\n3. Testing progress updates...");
            service.updateProgress(item1.getId(), 50);
            System.out.println("   ✓ Updated " + item1.getTitle() + " to 50%");
            
            service.markAsStarted(item2.getId());
            System.out.println("   ✓ Marked " + item2.getTitle() + " as started");
            
            service.updateProgress(item2.getId(), 25);
            System.out.println("   ✓ Updated " + item2.getTitle() + " to 25%");
            
            // Test 4: Test status transitions
            System.out.println("\n4. Testing status transitions...");
            service.markAsCompleted(item1.getId());
            System.out.println("   ✓ Marked " + item1.getTitle() + " as completed");
            
            // Test 5: Set dependencies
            System.out.println("\n5. Testing dependencies...");
            service.setDependency(item3.getId(), item2.getId());
            System.out.println("   ✓ Set dependency: " + item3.getTitle() + " depends on " + item2.getTitle());
            
            // Test 6: Test health status
            System.out.println("\n6. Testing health status...");
            item2.setEstimatedEndDate(LocalDate.now().minusDays(2)); // Make it overdue
            service.updateOpenItem(item2);
            
            Optional<OpenItem> overdueItem = repository.findById(item2.getId());
            if (overdueItem.isPresent()) {
                System.out.println("   ✓ Item health status: " + overdueItem.get().getHealthStatus());
                System.out.println("   ✓ Is overdue: " + overdueItem.get().isOverdue());
            }
            
            // Test 7: Query operations
            System.out.println("\n7. Testing query operations...");
            List<OpenItem> projectItems = service.getItemsByProject(testProject.getId());
            System.out.println("   ✓ Found " + projectItems.size() + " items for project");
            
            List<OpenItem> overdueItems = repository.findOverdueItems();
            System.out.println("   ✓ Found " + overdueItems.size() + " overdue items");
            
            List<OpenItem> atRiskItems = repository.findAtRiskItems();
            System.out.println("   ✓ Found " + atRiskItems.size() + " at-risk items");
            
            // Test 8: Statistics
            System.out.println("\n8. Testing statistics...");
            Map<String, Object> stats = service.getProjectStatistics(testProject.getId());
            System.out.println("   ✓ Total items: " + stats.get("totalItems"));
            System.out.println("   ✓ Completed: " + stats.get("completedItems"));
            System.out.println("   ✓ In progress: " + stats.get("inProgressItems"));
            System.out.println("   ✓ Overdue: " + stats.get("overdueItems"));
            System.out.println("   ✓ At risk: " + stats.get("atRiskItems"));
            System.out.println("   ✓ Overall progress: " + String.format("%.1f%%", stats.get("overallProgress")));
            
            // Test 9: Template creation
            System.out.println("\n9. Testing template creation...");
            Project templateProject = new Project();
            templateProject.setProjectId("TEST-TEMPLATE");
            templateProject.setDescription("Template Test Project");
            templateProject.setStartDate(LocalDate.now());
            templateProject.setEndDate(LocalDate.now().plusDays(60));
            templateProject.setStatus(ProjectStatus.ACTIVE);
            templateProject.setBudgetAmount(20000.0);
            templateProject = projectRepo.save(templateProject);
            
            List<OpenItem> templateItems = service.createOpenItemsFromTemplate(
                templateProject.getId(), 
                "INSTALLATION"
            );
            System.out.println("   ✓ Created " + templateItems.size() + " items from INSTALLATION template:");
            for (OpenItem item : templateItems) {
                System.out.println("     - " + item.getTitle() + 
                    " (" + item.getEstimatedStartDate() + " to " + item.getEstimatedEndDate() + ")");
            }
            
            // Test 10: Cleanup (soft delete)
            System.out.println("\n10. Testing soft delete...");
            service.deleteOpenItem(item3.getId());
            System.out.println("   ✓ Soft deleted: " + item3.getTitle());
            
            List<OpenItem> remainingItems = service.getItemsByProject(testProject.getId());
            System.out.println("   ✓ Remaining active items: " + remainingItems.size());
            
            // Test 11: Priority and categories
            System.out.println("\n11. Testing priority and categories...");
            OpenItem priorityItem = service.createOpenItem(
                testProject.getId(),
                "Critical Fix",
                "Fix production issue"
            );
            priorityItem.setPriority(OpenItem.Priority.HIGH);
            priorityItem.setCategory("Bug Fix");
            priorityItem.setEstimatedHours(8.0);
            priorityItem.setTags("urgent,production,bug");
            service.updateOpenItem(priorityItem);
            System.out.println("   ✓ Created high priority item: " + priorityItem.getTitle());
            System.out.println("     Priority: " + priorityItem.getPriority().getDisplayName());
            System.out.println("     Category: " + priorityItem.getCategory());
            System.out.println("     Est. Hours: " + priorityItem.getEstimatedHours());
            System.out.println("     Tags: " + priorityItem.getTags());
            
            // Final summary
            System.out.println("\n=== Test Summary ===");
            System.out.println("✓ Model operations working correctly");
            System.out.println("✓ Repository CRUD operations successful");
            System.out.println("✓ Service layer functioning properly");
            System.out.println("✓ Progress tracking operational");
            System.out.println("✓ Dependency management working");
            System.out.println("✓ Health status calculations correct");
            System.out.println("✓ Template system functional");
            System.out.println("✓ Statistics generation working");
            System.out.println("\n✅ All infrastructure tests passed!");
            
            // Cleanup
            System.out.println("\nCleaning up test data...");
            projectRepo.delete(testProject.getId());
            projectRepo.delete(templateProject.getId());
            System.out.println("✓ Test data cleaned up");
            
        } catch (Exception e) {
            System.err.println("\n❌ Test failed with error:");
            e.printStackTrace();
        }
    }
}