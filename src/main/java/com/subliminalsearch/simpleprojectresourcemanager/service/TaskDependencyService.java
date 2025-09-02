package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.model.TaskDependency;
import com.subliminalsearch.simpleprojectresourcemanager.model.TaskDependency.DependencyType;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskDependencyRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TaskDependencyService {
    
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    
    public TaskDependencyService(TaskRepository taskRepository, TaskDependencyRepository dependencyRepository) {
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
    }
    
    /**
     * Cascade dates for dependent tasks when a predecessor task's dates change
     */
    public void cascadeDates(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null || task.getPlannedStart() == null || task.getPlannedEnd() == null) {
            return;
        }
        
        // Find all tasks that depend on this task
        List<TaskDependency> successors = dependencyRepository.findSuccessors(taskId);
        
        for (TaskDependency dependency : successors) {
            Task successorTask = taskRepository.findById(dependency.getSuccessorId()).orElse(null);
            if (successorTask == null) {
                continue;
            }
            
            LocalDate newStartDate = calculateSuccessorStartDate(task, successorTask, dependency);
            
            if (newStartDate != null && !newStartDate.equals(successorTask.getPlannedStart())) {
                // Calculate duration
                long duration = 0;
                if (successorTask.getPlannedStart() != null && successorTask.getPlannedEnd() != null) {
                    duration = ChronoUnit.DAYS.between(successorTask.getPlannedStart(), successorTask.getPlannedEnd());
                }
                
                // Update successor task dates
                successorTask.setPlannedStart(newStartDate);
                successorTask.setPlannedEnd(newStartDate.plusDays(duration));
                
                // Save the updated task
                taskRepository.update(successorTask);
                
                // Recursively cascade to dependent tasks
                cascadeDates(successorTask.getId());
            }
        }
    }
    
    /**
     * Calculate the start date for a successor task based on dependency type
     */
    private LocalDate calculateSuccessorStartDate(Task predecessor, Task successor, TaskDependency dependency) {
        DependencyType type = dependency.getDependencyType();
        int lagDays = dependency.getLagDays() != null ? dependency.getLagDays() : 0;
        
        switch (type) {
            case FINISH_TO_START:
                // Successor starts after predecessor finishes
                return predecessor.getPlannedEnd().plusDays(lagDays + 1);
                
            case START_TO_START:
                // Successor starts when predecessor starts
                return predecessor.getPlannedStart().plusDays(lagDays);
                
            case FINISH_TO_FINISH:
                // Successor finishes when predecessor finishes
                // Calculate based on successor duration
                if (successor.getPlannedStart() != null && successor.getPlannedEnd() != null) {
                    long duration = ChronoUnit.DAYS.between(successor.getPlannedStart(), successor.getPlannedEnd());
                    return predecessor.getPlannedEnd().plusDays(lagDays).minusDays(duration);
                }
                return null;
                
            case START_TO_FINISH:
                // Successor finishes when predecessor starts (rare)
                // Calculate based on successor duration
                if (successor.getPlannedStart() != null && successor.getPlannedEnd() != null) {
                    long duration = ChronoUnit.DAYS.between(successor.getPlannedStart(), successor.getPlannedEnd());
                    return predecessor.getPlannedStart().plusDays(lagDays).minusDays(duration);
                }
                return null;
                
            default:
                return null;
        }
    }
    
    /**
     * Validate that adding a dependency won't create date conflicts
     */
    public boolean validateDependency(Long predecessorId, Long successorId, DependencyType type) {
        // Check for cyclic dependency
        if (dependencyRepository.hasCyclicDependency(predecessorId, successorId)) {
            return false;
        }
        
        // Get both tasks
        Task predecessor = taskRepository.findById(predecessorId).orElse(null);
        Task successor = taskRepository.findById(successorId).orElse(null);
        
        if (predecessor == null || successor == null) {
            return false;
        }
        
        // Check if dates are compatible based on dependency type
        if (predecessor.getPlannedStart() == null || predecessor.getPlannedEnd() == null ||
            successor.getPlannedStart() == null || successor.getPlannedEnd() == null) {
            return true; // Can't validate without dates
        }
        
        switch (type) {
            case FINISH_TO_START:
                // Successor should start after predecessor finishes
                return !successor.getPlannedStart().isBefore(predecessor.getPlannedEnd());
                
            case START_TO_START:
                // Successor should start at or after predecessor starts
                return !successor.getPlannedStart().isBefore(predecessor.getPlannedStart());
                
            case FINISH_TO_FINISH:
                // Successor should finish at or after predecessor finishes
                return !successor.getPlannedEnd().isBefore(predecessor.getPlannedEnd());
                
            case START_TO_FINISH:
                // Successor should finish at or after predecessor starts
                return !successor.getPlannedEnd().isBefore(predecessor.getPlannedStart());
                
            default:
                return true;
        }
    }
    
    /**
     * Find the critical path through the project
     */
    public List<Long> findCriticalPath(Long projectId) {
        List<Task> projectTasks = taskRepository.findByProjectId(projectId);
        List<TaskDependency> projectDependencies = dependencyRepository.findByProjectId(projectId);
        
        if (projectTasks.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Build dependency graph
        Map<Long, List<Long>> predecessors = new HashMap<>();
        Map<Long, List<Long>> successors = new HashMap<>();
        
        for (TaskDependency dep : projectDependencies) {
            predecessors.computeIfAbsent(dep.getSuccessorId(), k -> new ArrayList<>()).add(dep.getPredecessorId());
            successors.computeIfAbsent(dep.getPredecessorId(), k -> new ArrayList<>()).add(dep.getSuccessorId());
        }
        
        // Find tasks with no predecessors (start tasks)
        List<Task> startTasks = new ArrayList<>();
        for (Task task : projectTasks) {
            if (!predecessors.containsKey(task.getId())) {
                startTasks.add(task);
            }
        }
        
        // Calculate early start and early finish for all tasks
        Map<Long, LocalDate> earlyStart = new HashMap<>();
        Map<Long, LocalDate> earlyFinish = new HashMap<>();
        Map<Long, LocalDate> lateStart = new HashMap<>();
        Map<Long, LocalDate> lateFinish = new HashMap<>();
        Map<Long, Long> slack = new HashMap<>();
        
        // Forward pass - calculate early start and finish
        Queue<Task> queue = new LinkedList<>(startTasks);
        Set<Long> processed = new HashSet<>();
        
        while (!queue.isEmpty()) {
            Task task = queue.poll();
            if (processed.contains(task.getId())) {
                continue;
            }
            
            // Calculate early start
            LocalDate es = task.getPlannedStart();
            if (predecessors.containsKey(task.getId())) {
                for (Long predId : predecessors.get(task.getId())) {
                    if (earlyFinish.containsKey(predId)) {
                        LocalDate predFinish = earlyFinish.get(predId);
                        if (es == null || predFinish.plusDays(1).isAfter(es)) {
                            es = predFinish.plusDays(1);
                        }
                    }
                }
            }
            
            if (es != null && task.getPlannedEnd() != null) {
                earlyStart.put(task.getId(), es);
                long duration = ChronoUnit.DAYS.between(task.getPlannedStart(), task.getPlannedEnd());
                earlyFinish.put(task.getId(), es.plusDays(duration));
                
                // Add successors to queue
                if (successors.containsKey(task.getId())) {
                    for (Long succId : successors.get(task.getId())) {
                        Task succ = projectTasks.stream()
                            .filter(t -> t.getId().equals(succId))
                            .findFirst()
                            .orElse(null);
                        if (succ != null) {
                            queue.offer(succ);
                        }
                    }
                }
            }
            
            processed.add(task.getId());
        }
        
        // Find project end date
        LocalDate projectEnd = projectTasks.stream()
            .map(t -> earlyFinish.get(t.getId()))
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);
        
        if (projectEnd == null) {
            return new ArrayList<>();
        }
        
        // Backward pass - calculate late start and finish
        for (Task task : projectTasks) {
            if (!successors.containsKey(task.getId()) || successors.get(task.getId()).isEmpty()) {
                // End tasks
                lateFinish.put(task.getId(), projectEnd);
                if (task.getPlannedStart() != null && task.getPlannedEnd() != null) {
                    long duration = ChronoUnit.DAYS.between(task.getPlannedStart(), task.getPlannedEnd());
                    lateStart.put(task.getId(), projectEnd.minusDays(duration));
                }
            }
        }
        
        // Calculate slack and identify critical path
        List<Long> criticalPath = new ArrayList<>();
        for (Task task : projectTasks) {
            if (earlyStart.containsKey(task.getId()) && lateStart.containsKey(task.getId())) {
                long taskSlack = ChronoUnit.DAYS.between(earlyStart.get(task.getId()), lateStart.get(task.getId()));
                slack.put(task.getId(), taskSlack);
                
                if (taskSlack == 0) {
                    criticalPath.add(task.getId());
                }
            }
        }
        
        return criticalPath;
    }
}