package com.subliminalsearch.simpleprojectresourcemanager.controller;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.service.ClientReportService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class ClientReportController {
    private static final Logger logger = LoggerFactory.getLogger(ClientReportController.class);
    
    @FXML private Label projectIdLabel;
    @FXML private Label projectStatusLabel;
    @FXML private Label clientNameLabel;
    @FXML private Label clientEmailLabel;
    @FXML private CheckBox sendEmailCheckBox;
    @FXML private TextArea emailAddressesTextArea;
    @FXML private TextField subjectField;
    @FXML private TextArea messageTextArea;
    @FXML private Button generateButton;
    @FXML private Button sendButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    
    private Project project;
    private ClientReportService reportService;
    private File generatedReport;
    
    public void initialize() {
        reportService = new ClientReportService();
        
        // Initially disable send button
        sendButton.setDisable(true);
        
        // Setup email fields
        sendEmailCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            emailAddressesTextArea.setDisable(!newVal);
            subjectField.setDisable(!newVal);
            messageTextArea.setDisable(!newVal);
            sendButton.setDisable(!newVal || generatedReport == null);
        });
        
        // Default email template
        messageTextArea.setText("""
            Dear {ClientName},
            
            Please find attached the latest status report for project {ProjectId}.
            
            The report includes:
            • Current project status and progress
            • Timeline and milestones
            • Resource allocation
            • Task completion summary
            
            If you have any questions or concerns, please don't hesitate to contact us.
            
            Best regards,
            Project Management Team
            """);
    }
    
    public void setProject(Project project) {
        this.project = project;
        updateUI();
    }
    
    private void updateUI() {
        if (project != null) {
            projectIdLabel.setText(project.getProjectId());
            projectStatusLabel.setText(project.getStatus().getDisplayName());
            
            if (project.getContactName() != null) {
                clientNameLabel.setText(project.getContactName());
            }
            
            if (project.getContactEmail() != null) {
                clientEmailLabel.setText(project.getContactEmail());
                emailAddressesTextArea.setText(project.getContactEmail());
            }
            
            // Update subject line
            subjectField.setText("Project Status Report - " + project.getProjectId());
            
            // Replace placeholders in message
            String message = messageTextArea.getText();
            message = message.replace("{ClientName}", 
                project.getContactName() != null ? project.getContactName() : "Client");
            message = message.replace("{ProjectId}", project.getProjectId());
            messageTextArea.setText(message);
        }
    }
    
    @FXML
    private void generateReport() {
        if (project == null) {
            showAlert(Alert.AlertType.WARNING, "No Project", "Please select a project first.");
            return;
        }
        
        generateButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate
        statusLabel.setText("Generating report...");
        
        Task<File> task = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Generating PDF report...");
                return reportService.generateProjectReport(project);
            }
            
            @Override
            protected void succeeded() {
                generatedReport = getValue();
                Platform.runLater(() -> {
                    generateButton.setDisable(false);
                    progressBar.setVisible(false);
                    statusLabel.setText("Report generated successfully!");
                    
                    if (sendEmailCheckBox.isSelected()) {
                        sendButton.setDisable(false);
                    }
                    
                    // Ask if user wants to open the report
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Report Generated");
                    alert.setHeaderText("Report generated successfully!");
                    alert.setContentText("Would you like to open the report now?");
                    
                    ButtonType openButton = new ButtonType("Open Report");
                    ButtonType saveAsButton = new ButtonType("Save As...");
                    ButtonType cancelButton = ButtonType.CANCEL;
                    
                    alert.getButtonTypes().setAll(openButton, saveAsButton, cancelButton);
                    
                    alert.showAndWait().ifPresent(response -> {
                        if (response == openButton) {
                            openReport(generatedReport);
                        } else if (response == saveAsButton) {
                            saveReportAs(generatedReport);
                        }
                    });
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    generateButton.setDisable(false);
                    progressBar.setVisible(false);
                    statusLabel.setText("Failed to generate report");
                    
                    Throwable error = getException();
                    logger.error("Failed to generate report", error);
                    showAlert(Alert.AlertType.ERROR, "Generation Failed", 
                             "Failed to generate report: " + error.getMessage());
                });
            }
        };
        
        statusLabel.textProperty().bind(task.messageProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    @FXML
    private void sendReport() {
        if (generatedReport == null || !generatedReport.exists()) {
            showAlert(Alert.AlertType.WARNING, "No Report", "Please generate a report first.");
            return;
        }
        
        String recipients = emailAddressesTextArea.getText().trim();
        if (recipients.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Recipients", "Please enter email addresses.");
            return;
        }
        
        // TODO: Implement email sending functionality
        showAlert(Alert.AlertType.INFORMATION, "Email Feature", 
                 "Email functionality will be implemented in the next phase.\n" +
                 "For now, the report has been saved to:\n" + generatedReport.getAbsolutePath());
    }
    
    @FXML
    private void previewReport() {
        if (generatedReport != null && generatedReport.exists()) {
            openReport(generatedReport);
        } else {
            showAlert(Alert.AlertType.WARNING, "No Report", "Please generate a report first.");
        }
    }
    
    @FXML
    private void close() {
        Stage stage = (Stage) generateButton.getScene().getWindow();
        stage.close();
    }
    
    private void openReport(File report) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(report);
            } catch (IOException e) {
                logger.error("Failed to open report", e);
                showAlert(Alert.AlertType.ERROR, "Open Failed", 
                         "Failed to open report: " + e.getMessage());
            }
        }
    }
    
    private void saveReportAs(File report) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report As");
        fileChooser.setInitialFileName(report.getName());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        Stage stage = (Stage) generateButton.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);
        
        if (selectedFile != null) {
            try {
                java.nio.file.Files.copy(report.toPath(), selectedFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                statusLabel.setText("Report saved to: " + selectedFile.getName());
            } catch (IOException e) {
                logger.error("Failed to save report", e);
                showAlert(Alert.AlertType.ERROR, "Save Failed", 
                         "Failed to save report: " + e.getMessage());
            }
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}