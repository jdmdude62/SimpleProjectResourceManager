package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.OpenItem;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.repository.OpenItemRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class OpenItemService {
    private static final Logger logger = LoggerFactory.getLogger(OpenItemService.class);
    
    private final OpenItemRepository openItemRepository;
    private final ProjectRepository projectRepository;
    private final ResourceRepository resourceRepository;
    private final DatabaseConfig databaseConfig;
    private final ObservableList<OpenItem> allItems = FXCollections.observableArrayList();
    
    public OpenItemService(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        this.openItemRepository = new OpenItemRepository(databaseConfig);
        this.projectRepository = new ProjectRepository(databaseConfig.getDataSource());
        this.resourceRepository = new ResourceRepository(databaseConfig.getDataSource());
        loadAllItems();
    }
    
    public javax.sql.DataSource getDataSource() {
        return databaseConfig.getDataSource();
    }
    
    // Load all items into observable list
    private void loadAllItems() {
        logger.info("Refreshing cache - clearing current items");
        allItems.clear();
        List<OpenItem> fromDb = openItemRepository.findAll();
        logger.info("Repository returned {} items from database", fromDb.size());
        allItems.addAll(fromDb);
        logger.info("Cache now contains {} open items", allItems.size());
        
        // Log details of first few items for debugging
        if (!allItems.isEmpty()) {
            for (int i = 0; i < Math.min(3, allItems.size()); i++) {
                OpenItem item = allItems.get(i);
                logger.info("  Item {}: ID={}, ProjectID={}, Title='{}', Deleted={}", 
                    i, item.getId(), item.getProjectId(), item.getTitle(), item.isDeleted());
            }
        }
    }
    
    // Public method to refresh the cache when needed
    public void refreshCache() {
        logger.info("refreshCache() called - triggering loadAllItems");
        loadAllItems();
    }
    
    // CRUD Operations
    public OpenItem createOpenItem(Long projectId, String title, String description) {
        OpenItem item = new OpenItem(projectId, title);
        item.setDescription(description);
        item.setItemNumber(openItemRepository.generateItemNumber(projectId));
        item.setCreatedBy("System"); // TODO: Get from current user
        
        logger.info("Creating new open item: title={}, projectId={}", title, projectId);
        OpenItem saved = openItemRepository.save(item);
        
        if (saved != null && saved.getId() != null) {
            allItems.add(saved);
            logger.info("Successfully created and cached open item: {} (ID: {}) for project {}", 
                       saved.getTitle(), saved.getId(), projectId);
            logger.info("Cache now contains {} total items", allItems.size());
        } else {
            logger.error("Failed to save open item to repository!");
        }
        
        return saved;
    }
    
    public OpenItem updateOpenItem(OpenItem item) {
        item.setUpdatedBy("System"); // TODO: Get from current user
        OpenItem updated = openItemRepository.save(item);
        
        // Update in observable list - need to find by ID not by object reference
        boolean found = false;
        for (int i = 0; i < allItems.size(); i++) {
            if (allItems.get(i).getId().equals(updated.getId())) {
                allItems.set(i, updated);
                found = true;
                break;
            }
        }
        
        // If not found in list (shouldn't happen but just in case), add it
        if (!found) {
            allItems.add(updated);
            logger.warn("Updated item not found in cache, adding it: {}", updated.getId());
        }
        
        // Check and update health status based on progress
        updateHealthStatus(updated);
        
        return updated;
    }
    
    public void deleteOpenItem(Long itemId) {
        openItemRepository.delete(itemId);
        allItems.removeIf(item -> item.getId().equals(itemId));
        logger.info("Deleted open item with id: {}", itemId);
    }
    
    // Batch operations
    public List<OpenItem> createOpenItemsFromTemplate(Long projectId, String template) {
        List<OpenItem> items = new ArrayList<>();
        
        switch (template.toUpperCase()) {
            case "QUICK_START":
                // Minimal 5-item template for fast project setup
                items.add(createOpenItem(projectId, "Project Kickoff", "Initial meeting and scope confirmation"));
                items.add(createOpenItem(projectId, "Resource Assignment", "Assign team members and confirm availability"));
                items.add(createOpenItem(projectId, "Execute Work", "Complete primary project deliverables"));
                items.add(createOpenItem(projectId, "Quality Check", "Review and verify work meets requirements"));
                items.add(createOpenItem(projectId, "Project Closeout", "Final delivery and documentation"));
                break;
                
            case "FIELD_SERVICE":
                // Field service specific template
                items.add(createOpenItem(projectId, "Site Contact", "Confirm site access and contact person"));
                items.add(createOpenItem(projectId, "Equipment Check", "Verify tools and materials availability"));
                items.add(createOpenItem(projectId, "Travel Arrangements", "Confirm travel logistics if required"));
                items.add(createOpenItem(projectId, "On-Site Work", "Perform scheduled service/installation"));
                items.add(createOpenItem(projectId, "Customer Sign-off", "Obtain customer approval and signature"));
                items.add(createOpenItem(projectId, "Post-Visit Report", "Document work completed and any follow-ups"));
                break;
                
            case "PROJECT_KICKOFF":
                // Client-focused kickoff items
                items.add(createOpenItem(projectId, "Client Meeting", "Schedule and conduct kickoff meeting"));
                items.add(createOpenItem(projectId, "Requirements Review", "Confirm project requirements with client"));
                items.add(createOpenItem(projectId, "Timeline Agreement", "Finalize and approve project schedule"));
                items.add(createOpenItem(projectId, "Access & Permissions", "Obtain necessary site access and permits"));
                items.add(createOpenItem(projectId, "Communication Plan", "Establish reporting and update schedule"));
                break;
                
            case "WEEKLY_REVIEW":
                // Recurring weekly check items
                items.add(createOpenItem(projectId, "Status Update", "Compile weekly progress report"));
                items.add(createOpenItem(projectId, "Risk Review", "Identify and assess project risks"));
                items.add(createOpenItem(projectId, "Schedule Check", "Review schedule and adjust if needed"));
                items.add(createOpenItem(projectId, "Budget Review", "Check budget status and burn rate"));
                items.add(createOpenItem(projectId, "Team Check-in", "Team meeting and blocker resolution"));
                break;
                
            case "INSTALLATION":
                // Detailed installation template
                items.add(createOpenItem(projectId, "Site Survey", "Conduct site assessment"));
                items.add(createOpenItem(projectId, "Equipment Ordering", "Order required equipment"));
                items.add(createOpenItem(projectId, "Pre-Installation Prep", "Prepare site for installation"));
                items.add(createOpenItem(projectId, "Installation", "Install equipment"));
                items.add(createOpenItem(projectId, "Configuration", "Configure systems"));
                items.add(createOpenItem(projectId, "Testing & Commissioning", "Test and commission systems"));
                items.add(createOpenItem(projectId, "Training", "Train end users"));
                items.add(createOpenItem(projectId, "Handover", "Complete project handover"));
                break;
                
            case "PUNCH_LIST":
                // Final walkthrough/completion items
                items.add(createOpenItem(projectId, "Final Inspection", "Complete walkthrough with client"));
                items.add(createOpenItem(projectId, "Deficiency List", "Document any outstanding issues"));
                items.add(createOpenItem(projectId, "Corrections", "Complete all punch list items"));
                items.add(createOpenItem(projectId, "Final Testing", "Verify all systems operational"));
                items.add(createOpenItem(projectId, "Documentation Handover", "Provide all project documentation"));
                items.add(createOpenItem(projectId, "Warranty Information", "Provide warranty and support details"));
                break;
        }
        
        // Set estimated dates based on project
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isPresent()) {
            LocalDate projectStart = project.get().getStartDate();
            LocalDate projectEnd = project.get().getEndDate();
            long totalDays = ChronoUnit.DAYS.between(projectStart, projectEnd);
            long daysPerItem = totalDays / items.size();
            
            LocalDate currentDate = projectStart;
            for (OpenItem item : items) {
                item.setEstimatedStartDate(currentDate);
                currentDate = currentDate.plusDays(daysPerItem);
                item.setEstimatedEndDate(currentDate.minusDays(1));
            }
        }
        
        return items;
    }
    
    // Query operations
    public ObservableList<OpenItem> getAllItems() {
        return allItems;
    }
    
    public ObservableList<OpenItem> getItemsByProject(Long projectId) {
        logger.info("getItemsByProject called for project ID: {}", projectId);
        logger.info("Current cache size: {} items", allItems.size());
        
        List<OpenItem> filtered = allItems.stream()
            .filter(item -> item.getProjectId().equals(projectId))
            .collect(Collectors.toList());
            
        logger.info("Found {} items for project {}", filtered.size(), projectId);
        
        return FXCollections.observableArrayList(filtered);
    }
    
    public ObservableList<OpenItem> getItemsByResource(Long resourceId) {
        return FXCollections.observableArrayList(
            allItems.stream()
                .filter(item -> resourceId.equals(item.getAssignedResourceId()))
                .collect(Collectors.toList())
        );
    }
    
    public ObservableList<OpenItem> getItemsByStatus(OpenItem.ItemStatus status) {
        return FXCollections.observableArrayList(
            allItems.stream()
                .filter(item -> item.getStatus() == status)
                .collect(Collectors.toList())
        );
    }
    
    public ObservableList<OpenItem> getOverdueItems() {
        return FXCollections.observableArrayList(
            openItemRepository.findOverdueItems()
        );
    }
    
    public ObservableList<OpenItem> getAtRiskItems() {
        return FXCollections.observableArrayList(
            openItemRepository.findAtRiskItems()
        );
    }
    
    // Progress tracking
    public void updateProgress(Long itemId, int progressPercentage) {
        Optional<OpenItem> itemOpt = openItemRepository.findById(itemId);
        if (itemOpt.isPresent()) {
            OpenItem item = itemOpt.get();
            item.updateProgress(progressPercentage);
            updateOpenItem(item);
        }
    }
    
    public void markAsStarted(Long itemId) {
        Optional<OpenItem> itemOpt = openItemRepository.findById(itemId);
        if (itemOpt.isPresent()) {
            OpenItem item = itemOpt.get();
            item.setStatus(OpenItem.ItemStatus.IN_PROGRESS);
            item.setActualStartDate(LocalDate.now());
            updateOpenItem(item);
        }
    }
    
    public void markAsCompleted(Long itemId) {
        Optional<OpenItem> itemOpt = openItemRepository.findById(itemId);
        if (itemOpt.isPresent()) {
            OpenItem item = itemOpt.get();
            item.setStatus(OpenItem.ItemStatus.COMPLETED);
            item.setProgressPercentage(100);
            item.setActualEndDate(LocalDate.now());
            updateOpenItem(item);
            
            // Check for dependent items
            checkAndUpdateDependentItems(itemId);
        }
    }
    
    // Health status management
    private void updateHealthStatus(OpenItem item) {
        if (item.getStatus() == OpenItem.ItemStatus.COMPLETED || 
            item.getStatus() == OpenItem.ItemStatus.CANCELLED) {
            return; // No need to update health for completed/cancelled items
        }
        
        LocalDate today = LocalDate.now();
        OpenItem.HealthStatus newStatus = OpenItem.HealthStatus.ON_TRACK;
        
        // Check if overdue
        if (item.getEstimatedEndDate() != null && today.isAfter(item.getEstimatedEndDate())) {
            newStatus = OpenItem.HealthStatus.CRITICAL;
        }
        // Check if at risk (within 3 days of deadline with < 80% progress)
        else if (item.getEstimatedEndDate() != null) {
            long daysRemaining = ChronoUnit.DAYS.between(today, item.getEstimatedEndDate());
            if (daysRemaining <= 3 && item.getProgressPercentage() < 80) {
                newStatus = OpenItem.HealthStatus.AT_RISK;
            }
            // Check if delayed (behind schedule based on linear progress expectation)
            else if (item.getEstimatedStartDate() != null && item.getEstimatedEndDate() != null) {
                long totalDays = ChronoUnit.DAYS.between(item.getEstimatedStartDate(), item.getEstimatedEndDate());
                long elapsedDays = ChronoUnit.DAYS.between(item.getEstimatedStartDate(), today);
                
                if (elapsedDays > 0 && totalDays > 0) {
                    double expectedProgress = (double) elapsedDays / totalDays * 100;
                    if (item.getProgressPercentage() < expectedProgress - 20) {
                        newStatus = OpenItem.HealthStatus.DELAYED;
                    }
                }
            }
        }
        
        if (item.getHealthStatus() != newStatus) {
            item.setHealthStatus(newStatus);
            logger.info("Updated health status for item {} to {}", item.getTitle(), newStatus);
        }
    }
    
    // Dependency management
    public void setDependency(Long itemId, Long dependsOnId) {
        Optional<OpenItem> itemOpt = openItemRepository.findById(itemId);
        Optional<OpenItem> dependsOnOpt = openItemRepository.findById(dependsOnId);
        
        if (itemOpt.isPresent() && dependsOnOpt.isPresent()) {
            OpenItem item = itemOpt.get();
            OpenItem dependsOn = dependsOnOpt.get();
            
            // Validate no circular dependency
            if (!wouldCreateCircularDependency(itemId, dependsOnId)) {
                item.setDependsOnItemId(dependsOnId);
                
                // Update blocks list in the dependency
                String blocksIds = dependsOn.getBlocksItemIds();
                if (blocksIds == null || blocksIds.isEmpty()) {
                    dependsOn.setBlocksItemIds(itemId.toString());
                } else {
                    Set<String> blocks = new HashSet<>(Arrays.asList(blocksIds.split(",")));
                    blocks.add(itemId.toString());
                    dependsOn.setBlocksItemIds(String.join(",", blocks));
                }
                
                updateOpenItem(item);
                updateOpenItem(dependsOn);
            } else {
                logger.warn("Cannot set dependency: would create circular dependency");
            }
        }
    }
    
    private boolean wouldCreateCircularDependency(Long itemId, Long dependsOnId) {
        if (itemId.equals(dependsOnId)) return true;
        
        Set<Long> visited = new HashSet<>();
        Queue<Long> toCheck = new LinkedList<>();
        toCheck.add(dependsOnId);
        
        while (!toCheck.isEmpty()) {
            Long current = toCheck.poll();
            if (current.equals(itemId)) return true;
            
            if (!visited.contains(current)) {
                visited.add(current);
                Optional<OpenItem> currentItem = openItemRepository.findById(current);
                if (currentItem.isPresent() && currentItem.get().getDependsOnItemId() != null) {
                    toCheck.add(currentItem.get().getDependsOnItemId());
                }
            }
        }
        
        return false;
    }
    
    private void checkAndUpdateDependentItems(Long completedItemId) {
        // Find items that depend on the completed item
        List<OpenItem> dependentItems = allItems.stream()
            .filter(item -> completedItemId.equals(item.getDependsOnItemId()))
            .filter(item -> item.getStatus() == OpenItem.ItemStatus.NOT_STARTED)
            .collect(Collectors.toList());
        
        for (OpenItem item : dependentItems) {
            logger.info("Dependency resolved for item: {}", item.getTitle());
            // Optionally auto-start dependent items or send notifications
        }
    }
    
    // Focus view - items needing immediate attention
    public ObservableList<OpenItem> getFocusItems(Long resourceId) {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);
        
        List<OpenItem> focusItems = new ArrayList<>();
        
        for (OpenItem item : allItems) {
            boolean needsAttention = false;
            
            // Items due in next 3 days
            if (item.getEstimatedEndDate() != null && 
                !item.getEstimatedEndDate().isAfter(threeDaysFromNow) &&
                item.getStatus() != OpenItem.ItemStatus.COMPLETED &&
                item.getStatus() != OpenItem.ItemStatus.CANCELLED) {
                needsAttention = true;
            }
            
            // Blocked items
            if (item.getStatus() == OpenItem.ItemStatus.BLOCKED) {
                needsAttention = true;
            }
            
            // Items without updates in 5+ days (if in progress)
            if (item.getStatus() == OpenItem.ItemStatus.IN_PROGRESS &&
                item.getUpdatedAt() != null &&
                item.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(5))) {
                needsAttention = true;
            }
            
            // Items assigned to specific resource (if provided)
            if (needsAttention && resourceId != null) {
                needsAttention = resourceId.equals(item.getAssignedResourceId());
            }
            
            if (needsAttention) {
                focusItems.add(item);
            }
        }
        
        // Sort by priority and due date
        focusItems.sort((a, b) -> {
            // Priority first
            int priorityCompare = a.getPriority().compareTo(b.getPriority());
            if (priorityCompare != 0) return priorityCompare;
            
            // Then by due date (earliest first)
            if (a.getEstimatedEndDate() != null && b.getEstimatedEndDate() != null) {
                return a.getEstimatedEndDate().compareTo(b.getEstimatedEndDate());
            }
            return 0;
        });
        
        return FXCollections.observableArrayList(focusItems);
    }
    
    // Get items that can be worked on today (not blocked, resources available)
    public ObservableList<OpenItem> getActionableItems(Long resourceId) {
        List<OpenItem> actionableItems = allItems.stream()
            .filter(item -> item.getStatus() != OpenItem.ItemStatus.COMPLETED)
            .filter(item -> item.getStatus() != OpenItem.ItemStatus.CANCELLED)
            .filter(item -> item.getStatus() != OpenItem.ItemStatus.BLOCKED)
            .filter(item -> item.getStatus() != OpenItem.ItemStatus.ON_HOLD)
            .filter(item -> item.getDependsOnItemId() == null || isDependencyComplete(item.getDependsOnItemId()))
            .filter(item -> resourceId == null || resourceId.equals(item.getAssignedResourceId()))
            .sorted((a, b) -> {
                // Sort by priority, then by days remaining
                int priorityCompare = a.getPriority().compareTo(b.getPriority());
                if (priorityCompare != 0) return priorityCompare;
                return Integer.compare(a.getDaysRemaining(), b.getDaysRemaining());
            })
            .collect(Collectors.toList());
            
        return FXCollections.observableArrayList(actionableItems);
    }
    
    private boolean isDependencyComplete(Long dependsOnId) {
        Optional<OpenItem> dependency = openItemRepository.findById(dependsOnId);
        return dependency.map(item -> item.getStatus() == OpenItem.ItemStatus.COMPLETED).orElse(true);
    }
    
    // Statistics and reporting
    public Map<String, Object> getProjectStatistics(Long projectId) {
        List<OpenItem> projectItems = getItemsByProject(projectId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", projectItems.size());
        stats.put("completedItems", projectItems.stream()
            .filter(item -> item.getStatus() == OpenItem.ItemStatus.COMPLETED)
            .count());
        stats.put("inProgressItems", projectItems.stream()
            .filter(item -> item.getStatus() == OpenItem.ItemStatus.IN_PROGRESS)
            .count());
        stats.put("overdueItems", projectItems.stream()
            .filter(OpenItem::isOverdue)
            .count());
        stats.put("atRiskItems", projectItems.stream()
            .filter(item -> item.getHealthStatus() != OpenItem.HealthStatus.ON_TRACK)
            .count());
        
        // Calculate overall progress
        double overallProgress = projectItems.stream()
            .mapToInt(OpenItem::getProgressPercentage)
            .average()
            .orElse(0.0);
        stats.put("overallProgress", overallProgress);
        
        return stats;
    }
    
    // Refresh data from database
    public void refresh() {
        loadAllItems();
    }
}