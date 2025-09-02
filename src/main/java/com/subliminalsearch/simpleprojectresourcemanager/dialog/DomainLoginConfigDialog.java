package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class DomainLoginConfigDialog extends Dialog<Void> {
    private static final Logger logger = Logger.getLogger(DomainLoginConfigDialog.class.getName());
    
    private final ResourceRepository resourceRepository;
    private TableView<Resource> tableView;
    private ObservableList<Resource> resources;
    private TextField searchField;
    private Label statusLabel;
    private int modifiedCount = 0;
    
    public DomainLoginConfigDialog(Window owner, javax.sql.DataSource dataSource) {
        this.resourceRepository = new ResourceRepository(dataSource);
        
        setTitle("Configure Domain Logins for SharePoint Integration");
        setHeaderText("Map resources to their Active Directory domain logins for SharePoint synchronization");
        
        initOwner(owner);
        setResizable(true);
        
        DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(createContent());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setPrefSize(800, 600);
        
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                saveChanges();
            }
            return null;
        });
        
        loadResources();
    }
    
    private BorderPane createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Top section with instructions and search
        VBox topSection = new VBox(10);
        
        Label instructionsLabel = new Label(
            "Enter domain logins in one of these formats:\n" +
            "  • username@domain.com (preferred for cloud)\n" +
            "  • DOMAIN\\username (traditional AD format)\n" +
            "  • username (will use default domain)"
        );
        instructionsLabel.setWrapText(true);
        instructionsLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-border-radius: 5;");
        
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        searchField = new TextField();
        searchField.setPromptText("Search by name or email...");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterResources(newVal));
        
        Button clearButton = new Button("Clear Search");
        clearButton.setOnAction(e -> searchField.clear());
        
        Button autoFillButton = new Button("Auto-fill from Email");
        autoFillButton.setTooltip(new Tooltip("Automatically fill domain logins from email addresses"));
        autoFillButton.setOnAction(e -> autoFillFromEmail());
        
        searchBox.getChildren().addAll(
            new Label("Search:"), searchField, clearButton,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            autoFillButton
        );
        
        topSection.getChildren().addAll(instructionsLabel, searchBox);
        
        // Table
        tableView = createTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        
        // Status bar
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-padding: 5;");
        
        root.setTop(topSection);
        root.setCenter(tableView);
        root.setBottom(statusLabel);
        
        return root;
    }
    
    private TableView<Resource> createTable() {
        TableView<Resource> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<Resource, String> nameColumn = new TableColumn<>("Resource Name");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameColumn.setEditable(false);
        nameColumn.setPrefWidth(200);
        
        TableColumn<Resource, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        emailColumn.setEditable(false);
        emailColumn.setPrefWidth(200);
        
        TableColumn<Resource, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getResourceType() != null ? data.getValue().getResourceType().toString() : ""
        ));
        typeColumn.setEditable(false);
        typeColumn.setPrefWidth(100);
        
        TableColumn<Resource, String> domainLoginColumn = new TableColumn<>("Domain Login");
        domainLoginColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getLdapUsername() != null ? data.getValue().getLdapUsername() : ""
        ));
        domainLoginColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        domainLoginColumn.setOnEditCommit(event -> {
            Resource resource = event.getRowValue();
            String newValue = event.getNewValue();
            
            if (validateDomainLogin(newValue)) {
                resource.setLdapUsername(newValue);
                modifiedCount++;
                updateStatus();
            } else if (newValue != null && !newValue.trim().isEmpty()) {
                showValidationError(newValue);
                table.refresh();
            }
        });
        domainLoginColumn.setPrefWidth(250);
        domainLoginColumn.setEditable(true);
        
        // Add delete button column
        TableColumn<Resource, Void> deleteColumn = new TableColumn<>("Delete");
        deleteColumn.setPrefWidth(60);
        deleteColumn.setCellFactory(column -> {
            return new TableCell<Resource, Void>() {
                private final Button deleteButton = new Button("🗑");
                
                {
                    deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-weight: bold;");
                    deleteButton.setTooltip(new Tooltip("Delete this resource"));
                    deleteButton.setOnAction(event -> {
                        Resource resource = getTableView().getItems().get(getIndex());
                        deleteResource(resource);
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteButton);
                    }
                }
            };
        });
        
        table.getColumns().addAll(nameColumn, emailColumn, typeColumn, domainLoginColumn, deleteColumn);
        
        // Add context menu
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem fillFromEmailItem = new MenuItem("Fill from Email");
        fillFromEmailItem.setOnAction(e -> {
            Resource selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getEmail() != null) {
                String email = selected.getEmail();
                if (email.contains("@")) {
                    selected.setLdapUsername(email.substring(0, email.indexOf("@")));
                    modifiedCount++;
                    updateStatus();
                    table.refresh();
                }
            }
        });
        
        MenuItem clearItem = new MenuItem("Clear Domain Login");
        clearItem.setOnAction(e -> {
            Resource selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setLdapUsername(null);
                modifiedCount++;
                updateStatus();
                table.refresh();
            }
        });
        
        contextMenu.getItems().addAll(fillFromEmailItem, clearItem);
        table.setContextMenu(contextMenu);
        
        return table;
    }
    
    private void loadResources() {
        try {
            List<Resource> resourceList = resourceRepository.findAll();
            resources = FXCollections.observableArrayList(resourceList);
            tableView.setItems(resources);
            statusLabel.setText("Loaded " + resources.size() + " resources");
        } catch (Exception e) {
            logger.severe("Failed to load resources: " + e.getMessage());
            showError("Failed to load resources", e.getMessage());
        }
    }
    
    private void filterResources(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            tableView.setItems(resources);
        } else {
            String lowerSearch = searchText.toLowerCase();
            ObservableList<Resource> filtered = resources.filtered(r ->
                (r.getName() != null && r.getName().toLowerCase().contains(lowerSearch)) ||
                (r.getEmail() != null && r.getEmail().toLowerCase().contains(lowerSearch)) ||
                (r.getLdapUsername() != null && r.getLdapUsername().toLowerCase().contains(lowerSearch))
            );
            tableView.setItems(filtered);
        }
    }
    
    private void autoFillFromEmail() {
        int filled = 0;
        for (Resource resource : resources) {
            if ((resource.getLdapUsername() == null || resource.getLdapUsername().isEmpty()) 
                && resource.getEmail() != null && resource.getEmail().contains("@")) {
                String username = resource.getEmail().substring(0, resource.getEmail().indexOf("@"));
                resource.setLdapUsername(username);
                filled++;
                modifiedCount++;
            }
        }
        tableView.refresh();
        updateStatus();
        
        if (filled > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Auto-fill Complete");
            alert.setHeaderText(null);
            alert.setContentText("Filled " + filled + " domain logins from email addresses.");
            alert.showAndWait();
        }
    }
    
    private boolean validateDomainLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            return true; // Empty is valid (clears the field)
        }
        
        login = login.trim();
        
        // Basic validation patterns
        // username@domain.com format
        if (login.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            return true;
        }
        
        // DOMAIN\\username format
        if (login.matches("[A-Z0-9]+\\\\\\\\[a-zA-Z0-9._%+-]+")) {
            return true;
        }
        
        // Simple username (will use default domain)
        if (login.matches("[a-zA-Z0-9._%+-]+")) {
            return true;
        }
        
        return false;
    }
    
    private void showValidationError(String invalidLogin) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Domain Login");
        alert.setHeaderText("Invalid format: " + invalidLogin);
        alert.setContentText(
            "Please use one of these formats:\n" +
            "  • username@domain.com\n" +
            "  • DOMAIN\\username\n" +
            "  • username"
        );
        alert.showAndWait();
    }
    
    private void saveChanges() {
        if (modifiedCount == 0) {
            return;
        }
        
        try {
            int savedCount = 0;
            for (Resource resource : resources) {
                resourceRepository.update(resource);
                savedCount++;
            }
            
            logger.info("Saved domain logins for " + savedCount + " resources");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Changes Saved");
            alert.setHeaderText(null);
            alert.setContentText("Successfully updated domain logins for " + savedCount + " resources.");
            alert.showAndWait();
            
        } catch (Exception e) {
            logger.severe("Failed to save domain logins: " + e.getMessage());
            showError("Failed to save changes", e.getMessage());
        }
    }
    
    private void updateStatus() {
        int configured = (int) resources.stream()
            .filter(r -> r.getLdapUsername() != null && !r.getLdapUsername().isEmpty())
            .count();
        statusLabel.setText(String.format("Configured: %d/%d | Modified: %d", 
            configured, resources.size(), modifiedCount));
    }
    
    private void deleteResource(Resource resource) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Resource");
        confirmAlert.setHeaderText("Delete " + resource.getName() + "?");
        confirmAlert.setContentText("Are you sure you want to delete this resource?\n" +
            "This will remove the resource and all associated assignments.\n\n" +
            "This action cannot be undone.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Delete from database
                resourceRepository.delete(resource.getId());
                
                // Remove from list
                resources.remove(resource);
                tableView.refresh();
                
                logger.info("Deleted resource: " + resource.getName());
                updateStatus();
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Resource Deleted");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Resource '" + resource.getName() + "' has been deleted successfully.");
                successAlert.showAndWait();
                
            } catch (Exception e) {
                logger.severe("Failed to delete resource: " + e.getMessage());
                showError("Delete Failed", "Failed to delete resource: " + e.getMessage());
            }
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}