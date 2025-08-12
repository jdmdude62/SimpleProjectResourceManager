package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.model.TaskDependency;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class CriticalPathView {
    private final Stage stage;
    private final Project project;
    private final TaskRepository taskRepository;
    
    private Canvas canvas;
    private ScrollPane scrollPane;
    private List<Task> allTasks;
    private Map<Long, TaskNode> taskNodes;
    private List<Long> criticalPath;
    private Map<Long, Integer> taskLevels;
    private double zoom = 1.0;
    private String currentLayoutMode = "Hierarchical";
    private boolean showCriticalPathOnly = false;
    private boolean showSlackValues = true;
    
    private static final int NODE_WIDTH = 180;
    private static final int NODE_HEIGHT = 100;
    private static final int HORIZONTAL_GAP = 50;
    private static final int VERTICAL_GAP = 80;
    
    private class TaskNode {
        Task task;
        double x, y;
        int earliestStart;
        int earliestFinish;
        int latestStart;
        int latestFinish;
        int slack;
        boolean isCritical;
        
        TaskNode(Task task) {
            this.task = task;
        }
    }
    
    public CriticalPathView(Project project, TaskRepository taskRepository) {
        this.project = project;
        this.taskRepository = taskRepository;
        this.stage = new Stage();
        this.taskNodes = new HashMap<>();
        this.criticalPath = new ArrayList<>();
        this.taskLevels = new HashMap<>();
        this.allTasks = new ArrayList<>(); // Initialize to prevent NPE
        
        initialize();
    }
    
    private void initialize() {
        stage.setTitle("Critical Path Network - " + project.getProjectId());
        
        // Load task data first, before creating UI components that depend on it
        loadTaskData();
        calculateCriticalPath();
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Controls bar
        HBox controls = createControlsBar();
        
        // Canvas in scroll pane
        scrollPane = new ScrollPane();
        canvas = new Canvas(2000, 1500);
        scrollPane.setContent(canvas);
        scrollPane.setPannable(true);
        
        // Legend
        HBox legend = createLegend();
        
        // Info panel - now allTasks is initialized
        VBox infoPanel = createInfoPanel();
        
        root.getChildren().addAll(controls, scrollPane, legend, infoPanel);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/css/critical-path.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        
        layoutNodes();
        drawNetwork();
    }
    
    private HBox createControlsBar() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd;");
        
        // Zoom controls
        Button zoomInBtn = new Button("🔍+");
        Button zoomOutBtn = new Button("🔍-");
        Button resetZoomBtn = new Button("Reset Zoom");
        
        zoomInBtn.setOnAction(e -> {
            zoom *= 1.2;
            canvas.setScaleX(zoom);
            canvas.setScaleY(zoom);
        });
        
        zoomOutBtn.setOnAction(e -> {
            zoom /= 1.2;
            canvas.setScaleX(zoom);
            canvas.setScaleY(zoom);
        });
        
        resetZoomBtn.setOnAction(e -> {
            zoom = 1.0;
            canvas.setScaleX(zoom);
            canvas.setScaleY(zoom);
        });
        
        // Layout options
        ComboBox<String> layoutMode = new ComboBox<>();
        layoutMode.getItems().addAll("Hierarchical", "Timeline", "Compact");
        layoutMode.setValue("Hierarchical");
        layoutMode.setOnAction(e -> {
            currentLayoutMode = layoutMode.getValue();
            layoutNodes();
            drawNetwork();
        });
        
        // Filter options
        CheckBox showCriticalOnly = new CheckBox("Show Critical Path Only");
        showCriticalOnly.setOnAction(e -> {
            showCriticalPathOnly = showCriticalOnly.isSelected();
            drawNetwork();
        });
        
        CheckBox showSlack = new CheckBox("Show Slack Values");
        showSlack.setSelected(true);
        showSlack.setOnAction(e -> {
            showSlackValues = showSlack.isSelected();
            drawNetwork();
        });
        
        // Export button
        Button exportBtn = new Button("Export Diagram");
        exportBtn.setOnAction(e -> exportDiagram());
        
        controls.getChildren().addAll(
            new Label("Zoom:"), zoomInBtn, zoomOutBtn, resetZoomBtn,
            new Separator(),
            new Label("Layout:"), layoutMode,
            new Separator(),
            showCriticalOnly, showSlack,
            new Separator(),
            exportBtn
        );
        
        return controls;
    }
    
    private void loadTaskData() {
        allTasks = taskRepository.findByProjectId(project.getId());
        
        // Create task nodes
        for (Task task : allTasks) {
            TaskNode node = new TaskNode(task);
            taskNodes.put(task.getId(), node);
        }
        
        // Load dependencies
        for (Task task : allTasks) {
            List<TaskDependency> deps = taskRepository.findDependenciesBySuccessor(task.getId());
            // Dependencies are loaded with the tasks
        }
    }
    
    private void calculateCriticalPath() {
        if (allTasks.isEmpty()) return;
        
        // Forward pass - calculate earliest start and finish times
        Map<Long, Set<Long>> predecessors = new HashMap<>();
        Map<Long, Set<Long>> successors = new HashMap<>();
        
        for (Task task : allTasks) {
            List<TaskDependency> deps = taskRepository.findDependenciesBySuccessor(task.getId());
            for (TaskDependency dep : deps) {
                predecessors.computeIfAbsent(task.getId(), k -> new HashSet<>()).add(dep.getPredecessorId());
                successors.computeIfAbsent(dep.getPredecessorId(), k -> new HashSet<>()).add(task.getId());
            }
        }
        
        // Find tasks with no predecessors (start nodes)
        List<Task> startTasks = allTasks.stream()
            .filter(t -> !predecessors.containsKey(t.getId()) || predecessors.get(t.getId()).isEmpty())
            .collect(Collectors.toList());
        
        // Forward pass
        Queue<Long> queue = new LinkedList<>();
        Set<Long> processed = new HashSet<>();
        
        for (Task task : startTasks) {
            TaskNode node = taskNodes.get(task.getId());
            node.earliestStart = 0;
            node.earliestFinish = getDuration(task);
            queue.offer(task.getId());
        }
        
        while (!queue.isEmpty()) {
            Long taskId = queue.poll();
            if (processed.contains(taskId)) continue;
            processed.add(taskId);
            
            TaskNode currentNode = taskNodes.get(taskId);
            Set<Long> taskSuccessors = successors.get(taskId);
            
            if (taskSuccessors != null) {
                for (Long successorId : taskSuccessors) {
                    TaskNode successorNode = taskNodes.get(successorId);
                    if (successorNode != null) {
                        int proposedStart = currentNode.earliestFinish;
                        if (proposedStart > successorNode.earliestStart) {
                            successorNode.earliestStart = proposedStart;
                            successorNode.earliestFinish = successorNode.earliestStart + getDuration(successorNode.task);
                        }
                        
                        // Check if all predecessors have been processed
                        Set<Long> successorPreds = predecessors.get(successorId);
                        if (successorPreds == null || successorPreds.stream().allMatch(processed::contains)) {
                            queue.offer(successorId);
                        }
                    }
                }
            }
        }
        
        // Find the project end time (maximum earliest finish)
        int projectEndTime = taskNodes.values().stream()
            .mapToInt(n -> n.earliestFinish)
            .max()
            .orElse(0);
        
        // Backward pass - calculate latest start and finish times
        processed.clear();
        
        // Find tasks with no successors (end nodes)
        List<Task> endTasks = allTasks.stream()
            .filter(t -> !successors.containsKey(t.getId()) || successors.get(t.getId()).isEmpty())
            .collect(Collectors.toList());
        
        for (Task task : endTasks) {
            TaskNode node = taskNodes.get(task.getId());
            node.latestFinish = projectEndTime;
            node.latestStart = node.latestFinish - getDuration(task);
            queue.offer(task.getId());
        }
        
        while (!queue.isEmpty()) {
            Long taskId = queue.poll();
            if (processed.contains(taskId)) continue;
            processed.add(taskId);
            
            TaskNode currentNode = taskNodes.get(taskId);
            Set<Long> taskPredecessors = predecessors.get(taskId);
            
            if (taskPredecessors != null) {
                for (Long predecessorId : taskPredecessors) {
                    TaskNode predecessorNode = taskNodes.get(predecessorId);
                    if (predecessorNode != null) {
                        int proposedFinish = currentNode.latestStart;
                        if (predecessorNode.latestFinish == 0 || proposedFinish < predecessorNode.latestFinish) {
                            predecessorNode.latestFinish = proposedFinish;
                            predecessorNode.latestStart = predecessorNode.latestFinish - getDuration(predecessorNode.task);
                        }
                        
                        // Check if all successors have been processed
                        Set<Long> predSuccessors = successors.get(predecessorId);
                        if (predSuccessors == null || predSuccessors.stream().allMatch(processed::contains)) {
                            queue.offer(predecessorId);
                        }
                    }
                }
            }
        }
        
        // Calculate slack and identify critical path
        criticalPath.clear();
        for (TaskNode node : taskNodes.values()) {
            node.slack = node.latestStart - node.earliestStart;
            node.isCritical = (node.slack == 0);
            if (node.isCritical) {
                criticalPath.add(node.task.getId());
            }
        }
    }
    
    private int getDuration(Task task) {
        if (task.getPlannedStart() != null && task.getPlannedEnd() != null) {
            return (int) ChronoUnit.DAYS.between(task.getPlannedStart(), task.getPlannedEnd()) + 1;
        }
        return task.getEstimatedHours() != null ? (int) Math.ceil(task.getEstimatedHours() / 8.0) : 5;
    }
    
    private void layoutNodes() {
        if (taskNodes.isEmpty()) return;
        
        switch (currentLayoutMode) {
            case "Timeline":
                layoutNodesTimeline();
                break;
            case "Compact":
                layoutNodesCompact();
                break;
            case "Hierarchical":
            default:
                layoutNodesHierarchical();
                break;
        }
    }
    
    private void layoutNodesHierarchical() {
        // Calculate levels using topological sort
        Map<Long, Set<Long>> predecessors = new HashMap<>();
        Map<Long, Set<Long>> successors = new HashMap<>();
        
        for (Task task : allTasks) {
            List<TaskDependency> deps = taskRepository.findDependenciesBySuccessor(task.getId());
            for (TaskDependency dep : deps) {
                predecessors.computeIfAbsent(task.getId(), k -> new HashSet<>()).add(dep.getPredecessorId());
                successors.computeIfAbsent(dep.getPredecessorId(), k -> new HashSet<>()).add(task.getId());
            }
        }
        
        // Assign levels
        taskLevels.clear();
        Queue<Long> queue = new LinkedList<>();
        
        // Start with tasks that have no predecessors
        for (Task task : allTasks) {
            if (!predecessors.containsKey(task.getId()) || predecessors.get(task.getId()).isEmpty()) {
                taskLevels.put(task.getId(), 0);
                queue.offer(task.getId());
            }
        }
        
        while (!queue.isEmpty()) {
            Long taskId = queue.poll();
            int currentLevel = taskLevels.get(taskId);
            
            Set<Long> taskSuccessors = successors.get(taskId);
            if (taskSuccessors != null) {
                for (Long successorId : taskSuccessors) {
                    int newLevel = currentLevel + 1;
                    taskLevels.merge(successorId, newLevel, Math::max);
                    
                    // Check if all predecessors have been assigned levels
                    Set<Long> successorPreds = predecessors.get(successorId);
                    if (successorPreds == null || successorPreds.stream().allMatch(taskLevels::containsKey)) {
                        if (!queue.contains(successorId)) {
                            queue.offer(successorId);
                        }
                    }
                }
            }
        }
        
        // Group tasks by level
        Map<Integer, List<Long>> levelGroups = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : taskLevels.entrySet()) {
            levelGroups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        
        // Position nodes
        int maxLevel = levelGroups.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        
        for (int level = 0; level <= maxLevel; level++) {
            List<Long> tasksAtLevel = levelGroups.get(level);
            if (tasksAtLevel != null) {
                int count = tasksAtLevel.size();
                for (int i = 0; i < count; i++) {
                    TaskNode node = taskNodes.get(tasksAtLevel.get(i));
                    if (node != null) {
                        node.x = 50 + level * (NODE_WIDTH + HORIZONTAL_GAP);
                        node.y = 50 + i * (NODE_HEIGHT + VERTICAL_GAP);
                    }
                }
            }
        }
    }
    
    private void layoutNodesTimeline() {
        // Layout based on planned start dates
        if (allTasks.isEmpty()) return;
        
        LocalDate minDate = allTasks.stream()
            .map(Task::getPlannedStart)
            .filter(Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());
            
        LocalDate maxDate = allTasks.stream()
            .map(Task::getPlannedEnd)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now().plusMonths(1));
            
        long totalDays = ChronoUnit.DAYS.between(minDate, maxDate);
        double pixelsPerDay = 1800.0 / Math.max(totalDays, 1);
        
        int row = 0;
        for (Task task : allTasks) {
            TaskNode node = taskNodes.get(task.getId());
            if (node != null && task.getPlannedStart() != null) {
                long daysFromStart = ChronoUnit.DAYS.between(minDate, task.getPlannedStart());
                node.x = 50 + (int)(daysFromStart * pixelsPerDay);
                node.y = 50 + (row % 10) * (NODE_HEIGHT + VERTICAL_GAP);
                row++;
            }
        }
    }
    
    private void layoutNodesCompact() {
        // Compact grid layout
        int cols = (int) Math.ceil(Math.sqrt(taskNodes.size()));
        int row = 0, col = 0;
        
        for (TaskNode node : taskNodes.values()) {
            node.x = 50 + col * (NODE_WIDTH + 20);
            node.y = 50 + row * (NODE_HEIGHT + 20);
            
            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
    }
    
    private void drawNetwork() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw dependencies (arrows)
        gc.setLineWidth(2);
        for (Task task : allTasks) {
            List<TaskDependency> deps = taskRepository.findDependenciesBySuccessor(task.getId());
            for (TaskDependency dep : deps) {
                TaskNode fromNode = taskNodes.get(dep.getPredecessorId());
                TaskNode toNode = taskNodes.get(task.getId());
                
                if (fromNode != null && toNode != null) {
                    boolean isCritical = fromNode.isCritical && toNode.isCritical;
                    
                    // Skip non-critical paths if only showing critical path
                    if (showCriticalPathOnly && !isCritical) {
                        continue;
                    }
                    
                    gc.setStroke(isCritical ? Color.RED : Color.GRAY);
                    gc.setLineWidth(isCritical ? 3 : 2);
                    
                    drawArrow(gc, 
                        fromNode.x + NODE_WIDTH, fromNode.y + NODE_HEIGHT / 2,
                        toNode.x, toNode.y + NODE_HEIGHT / 2);
                }
            }
        }
        
        // Draw nodes
        for (TaskNode node : taskNodes.values()) {
            // Skip non-critical nodes if only showing critical path
            if (showCriticalPathOnly && !node.isCritical) {
                continue;
            }
            drawTaskNode(gc, node);
        }
    }
    
    private void drawTaskNode(GraphicsContext gc, TaskNode node) {
        double x = node.x;
        double y = node.y;
        
        // Node background
        if (node.isCritical) {
            gc.setFill(Color.web("#ffcccc"));
            gc.setStroke(Color.RED);
            gc.setLineWidth(3);
        } else {
            gc.setFill(Color.WHITE);
            gc.setStroke(Color.GRAY);
            gc.setLineWidth(2);
        }
        
        gc.fillRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 10, 10);
        gc.strokeRoundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 10, 10);
        
        // Task information
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        String title = node.task.getTitle();
        if (title.length() > 20) {
            title = title.substring(0, 20) + "...";
        }
        gc.fillText(title, x + 10, y + 20);
        
        gc.setFont(Font.font("System", 10));
        gc.fillText("Duration: " + getDuration(node.task) + " days", x + 10, y + 40);
        
        if (showSlackValues) {
            gc.fillText("ES: " + node.earliestStart + " | EF: " + node.earliestFinish, x + 10, y + 55);
            gc.fillText("LS: " + node.latestStart + " | LF: " + node.latestFinish, x + 10, y + 70);
            
            if (node.slack > 0) {
                gc.setFill(Color.GREEN);
                gc.fillText("Slack: " + node.slack + " days", x + 10, y + 85);
            } else {
                gc.setFill(Color.RED);
                gc.fillText("CRITICAL", x + 10, y + 85);
            }
        } else {
            // Just show if it's critical or not
            if (node.isCritical) {
                gc.setFill(Color.RED);
                gc.fillText("CRITICAL PATH", x + 10, y + 55);
            }
        }
    }
    
    private void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2) {
        gc.strokeLine(x1, y1, x2, y2);
        
        // Arrowhead
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double arrowLength = 10;
        double arrowAngle = Math.PI / 6;
        
        double x3 = x2 - arrowLength * Math.cos(angle - arrowAngle);
        double y3 = y2 - arrowLength * Math.sin(angle - arrowAngle);
        double x4 = x2 - arrowLength * Math.cos(angle + arrowAngle);
        double y4 = y2 - arrowLength * Math.sin(angle + arrowAngle);
        
        gc.strokeLine(x2, y2, x3, y3);
        gc.strokeLine(x2, y2, x4, y4);
    }
    
    private HBox createLegend() {
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(5));
        legend.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #ddd;");
        
        // Critical path indicator
        HBox criticalItem = new HBox(5);
        Label criticalBox = new Label("█");
        criticalBox.setTextFill(Color.RED);
        Label criticalLabel = new Label("Critical Path");
        criticalItem.getChildren().addAll(criticalBox, criticalLabel);
        
        // Non-critical indicator
        HBox nonCriticalItem = new HBox(5);
        Label nonCriticalBox = new Label("█");
        nonCriticalBox.setTextFill(Color.GRAY);
        Label nonCriticalLabel = new Label("Non-Critical");
        nonCriticalItem.getChildren().addAll(nonCriticalBox, nonCriticalLabel);
        
        // Legend labels
        Label esLabel = new Label("ES = Earliest Start");
        Label efLabel = new Label("EF = Earliest Finish");
        Label lsLabel = new Label("LS = Latest Start");
        Label lfLabel = new Label("LF = Latest Finish");
        
        legend.getChildren().addAll(
            criticalItem, nonCriticalItem,
            new Separator(),
            esLabel, efLabel, lsLabel, lfLabel
        );
        
        return legend;
    }
    
    private VBox createInfoPanel() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd;");
        
        Label titleLabel = new Label("Project Critical Path Analysis");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        int criticalTaskCount = criticalPath.size();
        int totalTasks = allTasks.size();
        
        Label statsLabel = new Label(String.format(
            "Total Tasks: %d | Critical Tasks: %d | Critical Percentage: %.1f%%",
            totalTasks, criticalTaskCount, 
            totalTasks > 0 ? (criticalTaskCount * 100.0 / totalTasks) : 0
        ));
        
        int totalDuration = taskNodes.values().stream()
            .mapToInt(n -> n.earliestFinish)
            .max()
            .orElse(0);
        
        Label durationLabel = new Label("Project Duration: " + totalDuration + " days");
        
        panel.getChildren().addAll(titleLabel, statsLabel, durationLabel);
        
        return panel;
    }
    
    private void exportDiagram() {
        // This would implement export functionality
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export");
        alert.setHeaderText(null);
        alert.setContentText("Export functionality would save the diagram as PNG/PDF");
        alert.showAndWait();
    }
    
    public void show() {
        stage.show();
        stage.centerOnScreen();
    }
}