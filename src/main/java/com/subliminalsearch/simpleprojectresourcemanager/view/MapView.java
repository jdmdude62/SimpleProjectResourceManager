package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
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
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MapView {
    private final Stage stage;
    private final Project project;
    private final TaskRepository taskRepository;
    private final ResourceRepository resourceRepository;
    private final AssignmentRepository assignmentRepository;
    
    private Canvas mapCanvas;
    private VBox routePanel;
    private List<Task> tasksWithLocations;
    private List<TaskLocation> locations;
    private List<TaskLocation> optimizedRoute;
    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private boolean showLabelsEnabled = true;
    private boolean showRouteEnabled = true;
    private String selectedTechnician = "All Technicians";
    
    private static class TaskLocation {
        Task task;
        double x, y;  // Map coordinates
        String address;
        boolean visited;
        int orderIndex;
        
        TaskLocation(Task task, double x, double y) {
            this.task = task;
            this.x = x;
            this.y = y;
            this.address = task.getLocation();
            this.visited = false;
            this.orderIndex = -1;
        }
    }
    
    public MapView(Project project, TaskRepository taskRepository, ResourceRepository resourceRepository, AssignmentRepository assignmentRepository) {
        this(project, taskRepository, resourceRepository, assignmentRepository, null);
    }
    
    public MapView(Project project, TaskRepository taskRepository, ResourceRepository resourceRepository, AssignmentRepository assignmentRepository, javafx.stage.Window owner) {
        this.project = project;
        this.taskRepository = taskRepository;
        this.resourceRepository = resourceRepository;
        this.assignmentRepository = assignmentRepository;
        this.stage = new Stage();
        this.locations = new ArrayList<>();
        this.optimizedRoute = new ArrayList<>();
        
        if (owner != null) {
            this.stage.initOwner(owner);
        }
        
        initialize(owner);
    }
    
    private void initialize(javafx.stage.Window owner) {
        stage.setTitle("Field Service Map - " + project.getProjectId());
        
        BorderPane root = new BorderPane();
        
        // Top controls
        HBox controls = createControlsBar();
        root.setTop(controls);
        
        // Center map
        ScrollPane scrollPane = new ScrollPane();
        mapCanvas = new Canvas(1200, 800);
        scrollPane.setContent(mapCanvas);
        scrollPane.setPannable(true);
        root.setCenter(scrollPane);
        
        // Right panel for route details
        routePanel = createRoutePanel();
        ScrollPane routeScroll = new ScrollPane(routePanel);
        routeScroll.setFitToWidth(true);
        routeScroll.setPrefWidth(350);
        root.setRight(routeScroll);
        
        // Bottom status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/css/map.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        
        // Position on the same screen as owner
        if (owner != null) {
            DialogUtils.positionStageOnOwnerScreen(stage, owner, 0.9, 0.85);
        } else {
            stage.centerOnScreen();
        }
        
        loadTaskLocations();
        generateMockLocations();
        drawMap();
    }
    
    private HBox createControlsBar() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd;");
        
        // Date filter
        Label dateLabel = new Label("Date:");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setOnAction(e -> {
            filterByDate(datePicker.getValue());
            drawMap();
        });
        
        // Technician filter - load actual resources assigned to this project
        Label techLabel = new Label("Technician:");
        ComboBox<String> techFilter = new ComboBox<>();
        techFilter.getItems().add("All Technicians");
        
        // Get unique resources assigned to this project
        List<Assignment> projectAssignments = assignmentRepository.findByProjectId(project.getId());
        Set<Long> resourceIds = projectAssignments.stream()
            .map(Assignment::getResourceId)
            .collect(Collectors.toSet());
        
        for (Long resourceId : resourceIds) {
            Optional<Resource> resource = resourceRepository.findById(resourceId);
            if (resource.isPresent()) {
                techFilter.getItems().add(resource.get().getName());
            }
        }
        
        techFilter.setValue("All Technicians");
        techFilter.setOnAction(e -> {
            selectedTechnician = techFilter.getValue();
            filterByTechnician();
            drawMap();
        });
        
        // Route optimization
        Button optimizeBtn = new Button("🛣 Optimize Route");
        optimizeBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        optimizeBtn.setOnAction(e -> {
            optimizeRoute();
            drawMap();
            updateRoutePanel();
        });
        
        // View options
        CheckBox showLabels = new CheckBox("Show Labels");
        showLabels.setSelected(true);
        showLabels.setOnAction(e -> {
            showLabelsEnabled = showLabels.isSelected();
            drawMap();
        });
        
        CheckBox showRoute = new CheckBox("Show Route");
        showRoute.setSelected(true);
        showRoute.setOnAction(e -> {
            showRouteEnabled = showRoute.isSelected();
            drawMap();
        });
        
        // Zoom controls
        Button zoomIn = new Button("🔍+");
        Button zoomOut = new Button("🔍-");
        Button resetView = new Button("Reset View");
        
        zoomIn.setOnAction(e -> {
            zoom *= 1.2;
            drawMap();
        });
        
        zoomOut.setOnAction(e -> {
            zoom /= 1.2;
            drawMap();
        });
        
        resetView.setOnAction(e -> {
            zoom = 1.0;
            offsetX = 0;
            offsetY = 0;
            drawMap();
        });
        
        // Export button
        Button exportBtn = new Button("📤 Export Route");
        exportBtn.setOnAction(e -> exportRoute());
        
        controls.getChildren().addAll(
            dateLabel, datePicker,
            new Separator(),
            techLabel, techFilter,
            new Separator(),
            optimizeBtn,
            new Separator(),
            showLabels, showRoute,
            new Separator(),
            zoomIn, zoomOut, resetView,
            new Separator(),
            exportBtn
        );
        
        return controls;
    }
    
    private VBox createRoutePanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: white;");
        
        Label title = new Label("Route Details");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Separator separator = new Separator();
        
        panel.getChildren().addAll(title, separator);
        
        return panel;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #ddd;");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        Label locationsLabel = new Label("Locations: 0");
        Label distanceLabel = new Label("Total Distance: 0 miles");
        Label timeLabel = new Label("Est. Travel Time: 0 hours");
        Label efficiencyLabel = new Label("Route Efficiency: N/A");
        
        statusBar.getChildren().addAll(locationsLabel, distanceLabel, timeLabel, efficiencyLabel);
        
        return statusBar;
    }
    
    private void loadTaskLocations() {
        List<Task> allTasks = taskRepository.findByProjectId(project.getId());
        
        tasksWithLocations = allTasks.stream()
            .filter(t -> t.getLocation() != null && !t.getLocation().isEmpty())
            .collect(Collectors.toList());
    }
    
    private void generateMockLocations() {
        // Generate mock coordinates for demo purposes
        // In a real application, these would come from geocoding addresses
        Random random = new Random(42); // Fixed seed for consistency
        
        locations.clear();
        
        // Add office/warehouse as starting point
        Task officeTask = new Task();
        officeTask.setTitle("Office/Warehouse");
        officeTask.setLocation("Main Office");
        TaskLocation office = new TaskLocation(officeTask, 400, 400);
        office.orderIndex = 0;
        locations.add(office);
        
        // Add task locations
        for (int i = 0; i < tasksWithLocations.size(); i++) {
            Task task = tasksWithLocations.get(i);
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 50 + random.nextDouble() * 250;
            double x = 400 + distance * Math.cos(angle);
            double y = 400 + distance * Math.sin(angle);
            
            TaskLocation location = new TaskLocation(task, x, y);
            locations.add(location);
        }
        
        // If no real tasks, add some demo locations
        if (tasksWithLocations.isEmpty()) {
            String[] demoLocations = {
                "123 Main St - Install Equipment",
                "456 Oak Ave - Maintenance Check",
                "789 Pine Rd - Repair HVAC",
                "321 Elm St - Safety Inspection",
                "654 Maple Dr - Equipment Upgrade"
            };
            
            for (String loc : demoLocations) {
                Task demoTask = new Task();
                demoTask.setTitle(loc.split(" - ")[1]);
                demoTask.setLocation(loc.split(" - ")[0]);
                demoTask.setPriority(Task.TaskPriority.values()[random.nextInt(4)]);
                demoTask.setStatus(Task.TaskStatus.NOT_STARTED);
                
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = 50 + random.nextDouble() * 250;
                double x = 400 + distance * Math.cos(angle);
                double y = 400 + distance * Math.sin(angle);
                
                TaskLocation location = new TaskLocation(demoTask, x, y);
                locations.add(location);
            }
        }
    }
    
    private void drawMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());
        
        // Apply zoom and offset
        gc.save();
        gc.scale(zoom, zoom);
        gc.translate(offsetX, offsetY);
        
        // Draw grid for reference
        drawGrid(gc);
        
        // Draw optimized route if available and enabled
        if (!optimizedRoute.isEmpty() && showRouteEnabled) {
            drawRoute(gc);
        }
        
        // Draw locations
        for (TaskLocation location : locations) {
            drawLocation(gc, location);
        }
        
        gc.restore();
        
        // Update status bar
        updateStatusBar();
    }
    
    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5);
        
        // Draw vertical lines
        for (int x = 0; x < 1200; x += 50) {
            gc.strokeLine(x, 0, x, 800);
        }
        
        // Draw horizontal lines
        for (int y = 0; y < 800; y += 50) {
            gc.strokeLine(0, y, 1200, y);
        }
    }
    
    private void drawRoute(GraphicsContext gc) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(3);
        gc.setLineDashes(5);
        
        for (int i = 0; i < optimizedRoute.size() - 1; i++) {
            TaskLocation from = optimizedRoute.get(i);
            TaskLocation to = optimizedRoute.get(i + 1);
            
            gc.strokeLine(from.x, from.y, to.x, to.y);
            
            // Draw arrow
            drawArrow(gc, from.x, from.y, to.x, to.y);
        }
        
        gc.setLineDashes(null);
    }
    
    private void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2) {
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
    
    private void drawLocation(GraphicsContext gc, TaskLocation location) {
        double x = location.x;
        double y = location.y;
        double size = 20;
        
        // Determine color based on priority or status
        Color fillColor;
        if (location.task.getTitle().equals("Office/Warehouse")) {
            fillColor = Color.GREEN;
            size = 25;
        } else if (location.task.getPriority() == Task.TaskPriority.CRITICAL) {
            fillColor = Color.RED;
        } else if (location.task.getPriority() == Task.TaskPriority.HIGH) {
            fillColor = Color.ORANGE;
        } else {
            fillColor = Color.BLUE;
        }
        
        // Draw location marker
        gc.setFill(fillColor);
        gc.fillOval(x - size/2, y - size/2, size, size);
        
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeOval(x - size/2, y - size/2, size, size);
        
        // Draw order number if part of route
        if (location.orderIndex >= 0) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 12));
            String orderText = String.valueOf(location.orderIndex);
            gc.fillText(orderText, x - 5, y + 5);
        }
        
        // Draw label if enabled
        if (showLabelsEnabled) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", 10));
            String label = location.task.getTitle();
            if (label.length() > 20) {
                label = label.substring(0, 20) + "...";
            }
            gc.fillText(label, x + size/2 + 5, y);
            
            // Draw address
            if (location.address != null) {
                gc.setFont(Font.font("System", 9));
                gc.setFill(Color.GRAY);
                gc.fillText(location.address, x + size/2 + 5, y + 12);
            }
        }
    }
    
    private void optimizeRoute() {
        // Simple nearest neighbor algorithm for demo
        // In production, would use more sophisticated routing algorithms
        
        optimizedRoute.clear();
        Set<TaskLocation> unvisited = new HashSet<>(locations);
        
        // Start from office
        TaskLocation current = locations.stream()
            .filter(l -> l.task.getTitle().equals("Office/Warehouse"))
            .findFirst()
            .orElse(locations.get(0));
        
        optimizedRoute.add(current);
        current.orderIndex = 0;
        unvisited.remove(current);
        
        int orderIndex = 1;
        while (!unvisited.isEmpty()) {
            TaskLocation nearest = findNearest(current, unvisited);
            if (nearest != null) {
                nearest.orderIndex = orderIndex++;
                optimizedRoute.add(nearest);
                unvisited.remove(nearest);
                current = nearest;
            } else {
                break;
            }
        }
        
        // Return to office
        TaskLocation office = locations.stream()
            .filter(l -> l.task.getTitle().equals("Office/Warehouse"))
            .findFirst()
            .orElse(null);
        
        if (office != null && !optimizedRoute.get(optimizedRoute.size() - 1).equals(office)) {
            optimizedRoute.add(office);
        }
    }
    
    private TaskLocation findNearest(TaskLocation from, Set<TaskLocation> candidates) {
        TaskLocation nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (TaskLocation candidate : candidates) {
            double distance = calculateDistance(from, candidate);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
            }
        }
        
        return nearest;
    }
    
    private double calculateDistance(TaskLocation from, TaskLocation to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    private void updateRoutePanel() {
        routePanel.getChildren().clear();
        
        Label title = new Label("Optimized Route");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Separator separator = new Separator();
        
        routePanel.getChildren().addAll(title, separator);
        
        double totalDistance = 0;
        double totalTime = 0;
        
        for (int i = 0; i < optimizedRoute.size(); i++) {
            TaskLocation location = optimizedRoute.get(i);
            
            VBox stopCard = new VBox(5);
            stopCard.setPadding(new Insets(10));
            stopCard.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #ddd; -fx-border-radius: 5;");
            
            HBox header = new HBox(10);
            Label stopNumber = new Label("Stop " + i);
            stopNumber.setFont(Font.font("System", FontWeight.BOLD, 12));
            stopNumber.setTextFill(Color.BLUE);
            
            Label taskTitle = new Label(location.task.getTitle());
            taskTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
            
            header.getChildren().addAll(stopNumber, taskTitle);
            
            Label address = new Label(location.address != null ? location.address : "No address");
            address.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
            
            stopCard.getChildren().addAll(header, address);
            
            if (location.task.getPriority() != null) {
                Label priority = new Label("Priority: " + location.task.getPriority());
                priority.setStyle("-fx-font-size: 10px;");
                stopCard.getChildren().add(priority);
            }
            
            if (i < optimizedRoute.size() - 1) {
                double distance = calculateDistance(location, optimizedRoute.get(i + 1));
                totalDistance += distance;
                
                Label distanceLabel = new Label(String.format("→ %.1f miles to next stop", distance / 50));
                distanceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #007bff;");
                stopCard.getChildren().add(distanceLabel);
                
                double time = distance / 50 * 15; // Assume 15 minutes per mile
                totalTime += time;
            }
            
            routePanel.getChildren().add(stopCard);
        }
        
        // Add summary
        Separator sumSep = new Separator();
        routePanel.getChildren().add(sumSep);
        
        VBox summary = new VBox(5);
        summary.setPadding(new Insets(10));
        summary.setStyle("-fx-background-color: #e8f4f8; -fx-border-color: #007bff;");
        
        Label summaryTitle = new Label("Route Summary");
        summaryTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        Label stops = new Label("Total Stops: " + (optimizedRoute.size() - 1));
        Label distance = new Label(String.format("Total Distance: %.1f miles", totalDistance / 50));
        Label time = new Label(String.format("Est. Travel Time: %.1f hours", totalTime / 60));
        
        // Add service time
        double serviceTime = (optimizedRoute.size() - 1) * 30; // 30 minutes per stop
        Label service = new Label(String.format("Est. Service Time: %.1f hours", serviceTime / 60));
        Label total = new Label(String.format("Total Time: %.1f hours", (totalTime + serviceTime) / 60));
        total.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        summary.getChildren().addAll(summaryTitle, stops, distance, time, service, total);
        routePanel.getChildren().add(summary);
    }
    
    private void updateStatusBar() {
        // Check if the canvas is in a scene before trying to update
        if (mapCanvas.getScene() == null) {
            return;
        }
        
        HBox statusBar = (HBox) ((BorderPane) mapCanvas.getScene().getRoot()).getBottom();
        
        Label locationsLabel = (Label) statusBar.getChildren().get(0);
        locationsLabel.setText("Locations: " + locations.size());
        
        if (!optimizedRoute.isEmpty()) {
            double totalDistance = 0;
            for (int i = 0; i < optimizedRoute.size() - 1; i++) {
                totalDistance += calculateDistance(optimizedRoute.get(i), optimizedRoute.get(i + 1));
            }
            
            Label distanceLabel = (Label) statusBar.getChildren().get(1);
            distanceLabel.setText(String.format("Total Distance: %.1f miles", totalDistance / 50));
            
            Label timeLabel = (Label) statusBar.getChildren().get(2);
            double travelTime = totalDistance / 50 * 15 / 60; // 15 min per mile
            timeLabel.setText(String.format("Est. Travel Time: %.1f hours", travelTime));
            
            Label efficiencyLabel = (Label) statusBar.getChildren().get(3);
            efficiencyLabel.setText("Route Efficiency: Optimized");
        }
    }
    
    private void filterByDate(LocalDate date) {
        // Filter locations by selected date
        List<Task> dateTasks = tasksWithLocations.stream()
            .filter(t -> t.getPlannedStart() != null && t.getPlannedStart().equals(date))
            .collect(Collectors.toList());
        
        // Regenerate locations for filtered tasks
        generateMockLocations();
    }
    
    private void filterByTechnician() {
        // Filter locations by selected technician
        if (!selectedTechnician.equals("All Technicians")) {
            // Find the resource by name
            Resource selectedResource = null;
            List<Assignment> projectAssignments = assignmentRepository.findByProjectId(project.getId());
            for (Assignment assignment : projectAssignments) {
                Optional<Resource> resource = resourceRepository.findById(assignment.getResourceId());
                if (resource.isPresent() && resource.get().getName().equals(selectedTechnician)) {
                    selectedResource = resource.get();
                    break;
                }
            }
            
            if (selectedResource != null) {
                // Filter tasks to only those assigned to this resource
                final Long resourceId = selectedResource.getId();
                List<TaskLocation> filtered = locations.stream()
                    .filter(loc -> {
                        // Check if this task has an assignment for the selected resource
                        List<Assignment> taskAssignments = assignmentRepository.findByProjectId(project.getId());
                        return taskAssignments.stream()
                            .anyMatch(a -> a.getResourceId().equals(resourceId));
                    })
                    .collect(Collectors.toList());
                
                if (!filtered.isEmpty()) {
                    locations = filtered;
                }
            }
        } else {
            // Reset to all locations
            generateMockLocations();
        }
    }
    
    private void exportRoute() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Route");
        alert.setHeaderText(null);
        alert.setContentText("Route exported to CSV/PDF format (implementation pending)");
        alert.showAndWait();
    }
    
    public void show() {
        stage.show();
        stage.toFront();
        stage.requestFocus();
        // Temporarily set always on top to ensure it appears above the Task List
        stage.setAlwaysOnTop(true);
        // Remove always on top after a short delay
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
            javafx.application.Platform.runLater(() -> stage.setAlwaysOnTop(false));
        });
    }
}