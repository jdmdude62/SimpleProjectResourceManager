package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-based undo manager that keeps deleted items in memory
 * for restoration during the current application session.
 */
public class UndoManager {
    private static final Logger logger = LoggerFactory.getLogger(UndoManager.class);
    private static final int MAX_UNDO_ITEMS = 20; // Keep last 20 deletions
    private static final long UNDO_TIMEOUT_MS = 10000; // 10 seconds to undo
    
    private final Map<String, UndoableAction> undoableActions = new ConcurrentHashMap<>();
    private final LinkedList<String> undoHistory = new LinkedList<>();
    
    // Singleton instance
    private static UndoManager instance;
    
    private UndoManager() {
        // Start cleanup thread to remove expired undo actions
        startCleanupThread();
    }
    
    public static UndoManager getInstance() {
        if (instance == null) {
            synchronized (UndoManager.class) {
                if (instance == null) {
                    instance = new UndoManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Store a deleted project for potential undo
     */
    public String storeDeletedProject(Project project, List<Assignment> assignments, List<Task> tasks) {
        String undoId = generateUndoId("project", project.getId());
        
        ProjectDeletion deletion = new ProjectDeletion(
            project, 
            new ArrayList<>(assignments), 
            tasks != null ? new ArrayList<>(tasks) : new ArrayList<>(),
            System.currentTimeMillis()
        );
        
        undoableActions.put(undoId, deletion);
        addToHistory(undoId);
        
        logger.info("Stored deleted project {} with {} assignments for undo (ID: {})", 
            project.getProjectId(), assignments.size(), undoId);
        
        return undoId;
    }
    
    /**
     * Store a deleted assignment for potential undo
     */
    public String storeDeletedAssignment(Assignment assignment) {
        String undoId = generateUndoId("assignment", assignment.getId());
        
        AssignmentDeletion deletion = new AssignmentDeletion(assignment, System.currentTimeMillis());
        
        undoableActions.put(undoId, deletion);
        addToHistory(undoId);
        
        logger.info("Stored deleted assignment {} for undo (ID: {})", assignment.getId(), undoId);
        
        return undoId;
    }
    
    /**
     * Store a deleted resource for potential undo
     */
    public String storeDeletedResource(Resource resource) {
        String undoId = generateUndoId("resource", resource.getId());
        
        ResourceDeletion deletion = new ResourceDeletion(resource, System.currentTimeMillis());
        
        undoableActions.put(undoId, deletion);
        addToHistory(undoId);
        
        logger.info("Stored deleted resource {} for undo (ID: {})", resource.getName(), undoId);
        
        return undoId;
    }
    
    /**
     * Check if an undo action is still available
     */
    public boolean canUndo(String undoId) {
        UndoableAction action = undoableActions.get(undoId);
        if (action == null) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - action.getTimestamp();
        return elapsed < UNDO_TIMEOUT_MS;
    }
    
    /**
     * Get the undoable action if still valid
     */
    public UndoableAction getUndoableAction(String undoId) {
        if (!canUndo(undoId)) {
            return null;
        }
        return undoableActions.get(undoId);
    }
    
    /**
     * Remove an undo action after it's been used or expired
     */
    public void removeUndoAction(String undoId) {
        undoableActions.remove(undoId);
        undoHistory.remove(undoId);
        logger.debug("Removed undo action: {}", undoId);
    }
    
    /**
     * Get description of what will be restored
     */
    public String getUndoDescription(String undoId) {
        UndoableAction action = undoableActions.get(undoId);
        if (action == null) {
            return null;
        }
        
        if (action instanceof ProjectDeletion) {
            ProjectDeletion pd = (ProjectDeletion) action;
            return String.format("Project %s with %d assignments", 
                pd.project.getProjectId(), pd.assignments.size());
        } else if (action instanceof AssignmentDeletion) {
            return "Assignment";
        } else if (action instanceof ResourceDeletion) {
            ResourceDeletion rd = (ResourceDeletion) action;
            return "Resource: " + rd.resource.getName();
        }
        
        return "Unknown item";
    }
    
    private String generateUndoId(String type, Long id) {
        return String.format("%s_%d_%d", type, id, System.currentTimeMillis());
    }
    
    private void addToHistory(String undoId) {
        undoHistory.addFirst(undoId);
        
        // Limit history size
        while (undoHistory.size() > MAX_UNDO_ITEMS) {
            String oldest = undoHistory.removeLast();
            undoableActions.remove(oldest);
            logger.debug("Removed oldest undo action due to size limit: {}", oldest);
        }
    }
    
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Check every second
                    
                    long now = System.currentTimeMillis();
                    List<String> expired = new ArrayList<>();
                    
                    for (Map.Entry<String, UndoableAction> entry : undoableActions.entrySet()) {
                        if (now - entry.getValue().getTimestamp() > UNDO_TIMEOUT_MS) {
                            expired.add(entry.getKey());
                        }
                    }
                    
                    for (String undoId : expired) {
                        removeUndoAction(undoId);
                        logger.debug("Expired undo action: {}", undoId);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("UndoManager-Cleanup");
        cleanupThread.start();
    }
    
    // Base class for undoable actions
    public abstract static class UndoableAction {
        protected final long timestamp;
        
        protected UndoableAction(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    // Specific deletion types
    public static class ProjectDeletion extends UndoableAction {
        public final Project project;
        public final List<Assignment> assignments;
        public final List<Task> tasks;
        
        public ProjectDeletion(Project project, List<Assignment> assignments, 
                              List<Task> tasks, long timestamp) {
            super(timestamp);
            this.project = project;
            this.assignments = assignments;
            this.tasks = tasks;
        }
    }
    
    public static class AssignmentDeletion extends UndoableAction {
        public final Assignment assignment;
        
        public AssignmentDeletion(Assignment assignment, long timestamp) {
            super(timestamp);
            this.assignment = assignment;
        }
    }
    
    public static class ResourceDeletion extends UndoableAction {
        public final Resource resource;
        
        public ResourceDeletion(Resource resource, long timestamp) {
            super(timestamp);
            this.resource = resource;
        }
    }
}