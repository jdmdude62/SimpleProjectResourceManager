package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ResourceTypeConstants;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for exporting assignment data to SharePoint-compatible formats.
 * This creates CSV files that can be imported into SharePoint lists.
 */
public class SharePointExportService {
    
    private final TaskRepository taskRepository;
    private final ResourceRepository resourceRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    public SharePointExportService(TaskRepository taskRepository, ResourceRepository resourceRepository) {
        this.taskRepository = taskRepository;
        this.resourceRepository = resourceRepository;
    }
    
    /**
     * Export all assignments to CSV format for SharePoint import
     */
    public void exportAssignmentsToCSV(String filePath, Project project) throws IOException {
        List<Task> tasks = taskRepository.findByProjectId(project.getId());
        List<Resource> resources = resourceRepository.findAll();
        
        // Create resource map for quick lookup
        Map<Long, Resource> resourceMap = resources.stream()
            .collect(Collectors.toMap(Resource::getId, r -> r));
        
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header matching SharePoint columns
            writer.write("Technician,Project ID,Project Name,Task Description,Start Date,End Date,");
            writer.write("Location,Status,Materials Status,PM Contact,Notes\n");
            
            // Write task data
            for (Task task : tasks) {
                if (task.getAssignedTo() != null && task.getPlannedStart() != null) {
                    Resource assignedResource = resourceMap.get(task.getAssignedTo());
                    if (assignedResource != null) {
                        writer.write(formatTaskForSharePoint(task, assignedResource, project));
                    }
                }
            }
        }
    }
    
    /**
     * Export assignments for a specific technician
     */
    public void exportTechnicianSchedule(String filePath, Long resourceId, List<Project> projects) throws IOException {
        Resource technician = resourceRepository.findById(resourceId).orElse(null);
        if (technician == null) {
            throw new IllegalArgumentException("Technician not found: " + resourceId);
        }
        
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("Week Starting,Monday,Tuesday,Wednesday,Thursday,Friday\n");
            
            // Group tasks by week
            LocalDate startDate = LocalDate.now().minusWeeks(1);
            LocalDate endDate = LocalDate.now().plusWeeks(4);
            
            LocalDate currentWeek = startDate.with(java.time.DayOfWeek.MONDAY);
            while (currentWeek.isBefore(endDate)) {
                writer.write(formatWeekSchedule(technician, currentWeek, projects));
                currentWeek = currentWeek.plusWeeks(1);
            }
        }
    }
    
    /**
     * Create SharePoint-compatible event format for calendar sync
     */
    public String createSharePointEvent(Task task, Resource technician, Project project) {
        StringBuilder event = new StringBuilder();
        
        // Format as SharePoint calendar event
        event.append("BEGIN:VEVENT\n");
        event.append("SUMMARY:").append(project.getProjectId()).append(" - ").append(task.getTitle()).append("\n");
        event.append("DTSTART:").append(formatDateForICS(task.getPlannedStart())).append("\n");
        event.append("DTEND:").append(formatDateForICS(task.getPlannedEnd())).append("\n");
        event.append("LOCATION:").append(task.getLocation() != null ? task.getLocation() : "TBD").append("\n");
        event.append("DESCRIPTION:").append(formatDescription(task, project)).append("\n");
        event.append("CATEGORIES:").append(task.getPriority() != null ? task.getPriority().toString() : "NORMAL").append("\n");
        event.append("STATUS:").append(mapTaskStatus(task.getStatus())).append("\n");
        event.append("END:VEVENT\n");
        
        return event.toString();
    }
    
    /**
     * Generate batch export for all technicians
     */
    public void exportAllTechnicianCalendars(String outputDirectory, List<Project> projects) throws IOException {
        List<Resource> technicians = resourceRepository.findAll().stream()
            .filter(r -> ResourceTypeConstants.isFieldTechnician(r.getResourceType()) || 
                        ResourceTypeConstants.isInternalEmployee(r.getResourceType()))
            .collect(Collectors.toList());
        
        for (Resource technician : technicians) {
            String fileName = outputDirectory + "/" + 
                             technician.getResourceName().replaceAll("[^a-zA-Z0-9]", "_") + 
                             "_schedule.csv";
            exportTechnicianSchedule(fileName, technician.getId(), projects);
        }
    }
    
    // Helper methods
    
    private String formatTaskForSharePoint(Task task, Resource resource, Project project) {
        StringBuilder row = new StringBuilder();
        
        // Technician
        row.append(escapeCSV(resource.getResourceName())).append(",");
        
        // Project ID
        row.append(escapeCSV(project.getProjectId())).append(",");
        
        // Project Name  
        row.append(escapeCSV(project.getDescription())).append(",");
        
        // Task Description
        row.append(escapeCSV(task.getTitle())).append(",");
        
        // Start Date
        row.append(task.getPlannedStart().format(DATE_FORMAT)).append(",");
        
        // End Date
        LocalDate endDate = task.getPlannedEnd() != null ? task.getPlannedEnd() : task.getPlannedStart();
        row.append(endDate.format(DATE_FORMAT)).append(",");
        
        // Location
        row.append(escapeCSV(task.getLocation() != null ? task.getLocation() : "")).append(",");
        
        // Status
        row.append(task.getStatus() != null ? task.getStatus().toString() : "SCHEDULED").append(",");
        
        // Materials Status
        String materialsStatus = "Pending";
        if (task.getRiskNotes() != null && task.getRiskNotes().toLowerCase().contains("materials ready")) {
            materialsStatus = "Ready";
        }
        row.append(materialsStatus).append(",");
        
        // PM Contact (using project manager ID for now)
        row.append(escapeCSV(project.getProjectManagerId() != null ? "PM-" + project.getProjectManagerId() : "")).append(",");
        
        // Notes (using description for now)
        row.append(escapeCSV(task.getDescription() != null ? task.getDescription() : "")).append("\n");
        
        return row.toString();
    }
    
    private String formatWeekSchedule(Resource technician, LocalDate weekStart, List<Project> projects) {
        StringBuilder week = new StringBuilder();
        week.append(weekStart.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))).append(",");
        
        for (int i = 0; i < 5; i++) { // Monday to Friday
            LocalDate currentDay = weekStart.plusDays(i);
            List<Task> dayTasks = findTasksForDay(technician.getId(), currentDay, projects);
            
            if (!dayTasks.isEmpty()) {
                String daySchedule = dayTasks.stream()
                    .map(t -> getProjectForTask(t, projects).getProjectId() + ": " + t.getTitle())
                    .collect(Collectors.joining("; "));
                week.append(escapeCSV(daySchedule));
            }
            
            if (i < 4) week.append(",");
        }
        
        week.append("\n");
        return week.toString();
    }
    
    private List<Task> findTasksForDay(Long resourceId, LocalDate date, List<Project> projects) {
        return projects.stream()
            .flatMap(p -> taskRepository.findByProjectId(p.getId()).stream())
            .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().equals(resourceId))
            .filter(t -> t.getPlannedStart() != null)
            .filter(t -> !date.isBefore(t.getPlannedStart()) && 
                        !date.isAfter(t.getPlannedEnd() != null ? t.getPlannedEnd() : t.getPlannedStart()))
            .collect(Collectors.toList());
    }
    
    private Project getProjectForTask(Task task, List<Project> projects) {
        return projects.stream()
            .filter(p -> p.getId().equals(task.getProjectId()))
            .findFirst()
            .orElse(null);
    }
    
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String formatDateForICS(LocalDate date) {
        return date.format(DateTimeFormatter.BASIC_ISO_DATE);
    }
    
    private String formatDescription(Task task, Project project) {
        StringBuilder desc = new StringBuilder();
        desc.append("Project: ").append(project.getProjectId()).append("\\n");
        desc.append("Task: ").append(task.getTitle()).append("\\n");
        
        if (task.getDescription() != null) {
            desc.append("Notes: ").append(task.getDescription()).append("\\n");
        }
        
        if (task.getProgressPercentage() != null) {
            desc.append("Progress: ").append(task.getProgressPercentage()).append("%\\n");
        }
        
        return desc.toString();
    }
    
    private String mapTaskStatus(Task.TaskStatus status) {
        if (status == null) return "TENTATIVE";
        
        switch (status) {
            case COMPLETED:
                return "CONFIRMED";
            case IN_PROGRESS:
                return "CONFIRMED";
            case NOT_STARTED:
                return "TENTATIVE";
            default:
                return "TENTATIVE";
        }
    }
}