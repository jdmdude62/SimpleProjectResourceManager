package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.view.OpenItemsGridView;
import com.subliminalsearch.simpleprojectresourcemanager.view.OpenItemsKanbanView;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class OpenItemsManagementDialog extends Dialog<Void> {
    private final Project project;
    private final DatabaseConfig databaseConfig;
    private OpenItemsGridView gridView;
    private OpenItemsKanbanView kanbanView;
    
    public OpenItemsManagementDialog(Project project, DatabaseConfig databaseConfig, Window owner) {
        this.project = project;
        this.databaseConfig = databaseConfig;
        
        initializeDialog(owner);
        setupContent();
    }
    
    private void initializeDialog(Window owner) {
        setTitle("Open Items - " + project.getProjectId());
        setHeaderText("Manage open items for project: " + project.getDescription());
        
        // Set dialog size to 90% of owner window
        if (owner != null) {
            setWidth(owner.getWidth() * 0.9);
            setHeight(owner.getHeight() * 0.9);
        } else {
            setWidth(1200);
            setHeight(800);
        }
        
        setResizable(true);
        
        // Add close button
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }
    
    private void setupContent() {
        BorderPane contentPane = new BorderPane();
        contentPane.setPadding(new Insets(10));
        
        // Create tab pane for different views
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Grid View Tab
        Tab gridTab = new Tab("Grid View");
        gridView = new OpenItemsGridView(databaseConfig);
        gridView.setProject(project.getId()); // This will now refresh cache
        gridTab.setContent(gridView);
        
        // Kanban View Tab
        Tab kanbanTab = new Tab("Kanban View");
        kanbanView = new OpenItemsKanbanView(databaseConfig);
        // Don't set project yet to avoid double refresh
        kanbanTab.setContent(kanbanView);
        
        tabPane.getTabs().addAll(gridTab, kanbanTab);
        
        // Add listener to refresh views when switching tabs
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == gridTab) {
                // Refresh grid view when switching to it
                gridView.setProject(project.getId());
            } else if (newTab == kanbanTab) {
                // Refresh kanban view when switching to it (also sets it the first time)
                kanbanView.setProject(project.getId());
            }
        });
        
        // Also set up cross-refresh when items are added/modified
        // Grid view should notify kanban and vice versa
        gridView.setOnItemsChanged(() -> {
            if (tabPane.getSelectionModel().getSelectedItem() != gridTab) {
                kanbanView.setProject(project.getId());
            }
        });
        
        kanbanView.setOnItemsChanged(() -> {
            if (tabPane.getSelectionModel().getSelectedItem() != kanbanTab) {
                gridView.setProject(project.getId());
            }
        });
        
        // Add project info bar at top
        Label projectInfo = new Label(String.format(
            "Project: %s | Start: %s | End: %s | Status: %s",
            project.getProjectId(),
            project.getStartDate() != null ? project.getStartDate().toString() : "N/A",
            project.getEndDate() != null ? project.getEndDate().toString() : "N/A",
            project.getStatus() != null ? project.getStatus().getDisplayName() : "N/A"
        ));
        projectInfo.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-font-weight: bold;");
        
        VBox container = new VBox(5);
        container.getChildren().addAll(projectInfo, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        
        contentPane.setCenter(container);
        
        getDialogPane().setContent(contentPane);
        
        // Apply CSS styling
        getDialogPane().setStyle("-fx-min-width: 1000; -fx-min-height: 600;");
    }
    
    public void refresh() {
        if (gridView != null) {
            gridView.setProject(project.getId());
        }
        if (kanbanView != null) {
            kanbanView.setProject(project.getId());
        }
    }
}