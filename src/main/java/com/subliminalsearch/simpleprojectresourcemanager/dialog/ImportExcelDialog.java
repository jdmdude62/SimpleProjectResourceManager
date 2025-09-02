package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.util.TechScheduleImporter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Optional;

public class ImportExcelDialog extends Dialog<ButtonType> {
    private final SchedulingService schedulingService;
    private TextField filePathField;
    private TextArea previewArea;
    private TextArea resultArea;
    private Button browseButton;
    private Button importButton;
    private ProgressBar progressBar;
    private Label statusLabel;
    
    public ImportExcelDialog(SchedulingService schedulingService, Window owner) {
        this.schedulingService = schedulingService;
        
        setTitle("Import Tech Schedule from Excel");
        setHeaderText("Import technician schedules from Excel spreadsheet");
        initOwner(owner);
        
        // Create content
        VBox content = new VBox(5); // Reduced spacing from 10 to 5
        content.setPadding(new Insets(10)); // Reduced padding from 20 to 10
        content.setPrefWidth(700); // Increased width for better readability
        content.setPrefHeight(600); // Increased height to accommodate taller textarea
        
        // File selection section
        Label fileLabel = new Label("Excel File:");
        filePathField = new TextField();
        filePathField.setPromptText("Select Excel file...");
        filePathField.setEditable(false);
        
        browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> browseForFile());
        
        HBox fileBox = new HBox(10);
        fileBox.getChildren().addAll(filePathField, browseButton);
        HBox.setHgrow(filePathField, Priority.ALWAYS);
        
        // Instructions - made more compact
        Label instructionsLabel = new Label("Expected Format:");
        instructionsLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        TextArea instructionsArea = new TextArea(
            "• Each worksheet = one technician (name must match resource name)\n" +
            "• Column B: month/year markers, PM names, Shop/Open, Training, Time Off/Holiday\n" +
            "• Project assignments in colored cells (Format: 'ProjectID - Description')\n" +
            "• Days aligned by weekday, merged cells = multi-day assignments"
        );
        instructionsArea.setEditable(false);
        instructionsArea.setPrefRowCount(3); // Reduced from 8 to 3 rows
        instructionsArea.setWrapText(true);
        instructionsArea.setStyle("-fx-font-size: 10px;");
        
        // Preview section - made smaller
        Label previewLabel = new Label("Import Preview:");
        previewLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setPrefRowCount(2); // Reduced from 5 to 2 rows
        previewArea.setPromptText("Select a file to preview import...");
        previewArea.setStyle("-fx-font-size: 10px;");
        
        // Import button
        importButton = new Button("Import");
        importButton.setDisable(true);
        importButton.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        importButton.setOnAction(e -> performImport());
        
        // Progress section
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 11px;");
        
        // Result section - made much taller
        Label resultLabel = new Label("Import Results:");
        resultLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefRowCount(20); // Increased from 8 to 20 rows for better visibility
        resultArea.setPromptText("Import results will appear here...");
        resultArea.setWrapText(true);
        VBox.setVgrow(resultArea, Priority.ALWAYS); // Allow it to expand
        
        // Add all components with minimal separators
        content.getChildren().addAll(
            fileLabel,
            fileBox,
            instructionsLabel,
            instructionsArea,
            previewLabel,
            previewArea,
            importButton,
            progressBar,
            statusLabel,
            resultLabel,
            resultArea
        );
        
        // Set content
        getDialogPane().setContent(content);
        
        // Add buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        // Disable close button during import
        Button closeButton = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setText("Close");
    }
    
    private void browseForFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Tech Schedule Excel File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Set initial directory
        File initialDir = new File(System.getProperty("user.home"));
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }
        
        File selectedFile = fileChooser.showOpenDialog(getOwner());
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
            importButton.setDisable(false);
            previewFile(selectedFile);
        }
    }
    
    private void previewFile(File file) {
        previewArea.setText("File: " + file.getName() + "\n");
        previewArea.appendText("Size: " + (file.length() / 1024) + " KB\n");
        previewArea.appendText("\nReady to import. Click 'Import' to begin.\n");
        previewArea.appendText("\nNote: Existing projects and assignments will not be duplicated.");
    }
    
    private void performImport() {
        String filePath = filePathField.getText();
        if (filePath == null || filePath.isEmpty()) {
            showAlert("No File Selected", "Please select an Excel file to import.");
            return;
        }
        
        // Confirm import
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Import");
        confirm.setHeaderText("Import Tech Schedule");
        confirm.setContentText(
            "This will import projects and assignments from:\n" +
            filePath + "\n\n" +
            "Existing projects will be updated.\n" +
            "New assignments will be created.\n\n" +
            "Continue with import?"
        );
        
        Optional<ButtonType> confirmResult = confirm.showAndWait();
        if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) {
            return;
        }
        
        // Disable controls during import
        browseButton.setDisable(true);
        importButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        statusLabel.setText("Importing...");
        resultArea.clear();
        
        // Perform import in background
        Task<TechScheduleImporter.ImportResult> importTask = new Task<>() {
            @Override
            protected TechScheduleImporter.ImportResult call() throws Exception {
                updateMessage("Reading Excel file...");
                
                TechScheduleImporter importer = new TechScheduleImporter(schedulingService);
                TechScheduleImporter.ImportResult result = importer.importExcelFile(filePath);
                
                return result;
            }
        };
        
        importTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> statusLabel.setText(newMsg));
        });
        
        importTask.setOnSucceeded(e -> {
            TechScheduleImporter.ImportResult result = importTask.getValue();
            
            // Display results
            resultArea.setText(result.getSummary());
            
            // Update status
            if (result.hasErrors()) {
                statusLabel.setText("Import completed with errors");
                statusLabel.setStyle("-fx-text-fill: orange;");
            } else {
                statusLabel.setText("Import completed successfully!");
                statusLabel.setStyle("-fx-text-fill: green;");
            }
            
            // Re-enable controls
            browseButton.setDisable(false);
            importButton.setDisable(false);
            progressBar.setVisible(false);
        });
        
        importTask.setOnFailed(e -> {
            Throwable error = importTask.getException();
            resultArea.setText("Import failed:\n" + error.getMessage());
            statusLabel.setText("Import failed");
            statusLabel.setStyle("-fx-text-fill: red;");
            
            // Re-enable controls
            browseButton.setDisable(false);
            importButton.setDisable(false);
            progressBar.setVisible(false);
            
            error.printStackTrace();
        });
        
        // Start import
        Thread importThread = new Thread(importTask);
        importThread.setDaemon(true);
        importThread.start();
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(getOwner());
        alert.showAndWait();
    }
}