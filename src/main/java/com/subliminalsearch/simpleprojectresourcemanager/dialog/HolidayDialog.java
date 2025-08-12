package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.CompanyHoliday;
import com.subliminalsearch.simpleprojectresourcemanager.model.HolidayType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

public class HolidayDialog extends Dialog<CompanyHoliday> {
    private static final Logger logger = LoggerFactory.getLogger(HolidayDialog.class);
    
    private final TextField nameField;
    private final DatePicker datePicker;
    private final ComboBox<HolidayType> typeCombo;
    private final TextArea descriptionArea;
    private final CheckBox recurringCheckBox;
    private final TextField recurrenceRuleField;
    private final CheckBox workingHolidayAllowedCheckBox;
    private final TextField departmentField;
    private final CheckBox activeCheckBox;

    public HolidayDialog() {
        this(null);
    }
    
    public HolidayDialog(CompanyHoliday holiday) {
        setTitle(holiday == null ? "New Company Holiday" : "Edit Company Holiday");
        setHeaderText(null);
        
        // Initialize components
        nameField = new TextField();
        nameField.setPromptText("Holiday name (e.g., Independence Day)");
        nameField.setPrefWidth(300);
        
        datePicker = new DatePicker(LocalDate.now());
        
        typeCombo = new ComboBox<>(FXCollections.observableArrayList(HolidayType.values()));
        typeCombo.setPrefWidth(200);
        
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Holiday description (optional)");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPrefWidth(300);
        
        recurringCheckBox = new CheckBox("Recurring Holiday");
        recurrenceRuleField = new TextField();
        recurrenceRuleField.setPromptText("e.g., ANNUAL:THIRD_MONDAY_JANUARY, ANNUAL:FIXED_DATE");
        recurrenceRuleField.setPrefWidth(250);
        recurrenceRuleField.setDisable(true);
        
        workingHolidayAllowedCheckBox = new CheckBox("Allow working during holiday");
        workingHolidayAllowedCheckBox.setTooltip(new Tooltip("Check if resources can be assigned to work during this holiday with proper authorization"));
        
        departmentField = new TextField();
        departmentField.setPromptText("Department (leave empty for company-wide)");
        departmentField.setPrefWidth(200);
        
        activeCheckBox = new CheckBox("Active");
        activeCheckBox.setSelected(true);
        
        // Set up form layout
        GridPane grid = createFormLayout();
        
        // Set up recurring checkbox behavior
        recurringCheckBox.setOnAction(e -> {
            recurrenceRuleField.setDisable(!recurringCheckBox.isSelected());
            if (!recurringCheckBox.isSelected()) {
                recurrenceRuleField.clear();
            }
        });
        
        // Populate fields if editing
        if (holiday != null) {
            populateFields(holiday);
        } else {
            // Set defaults for new holiday
            typeCombo.setValue(HolidayType.COMPANY);
        }
        
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Set validation AFTER adding button types
        setupValidation();
        
        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return createHolidayFromFields(holiday);
            }
            return null;
        });
        
        // Focus on name field
        nameField.requestFocus();
    }
    
    private GridPane createFormLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        int row = 0;
        
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row++);
        
        grid.add(new Label("Date:"), 0, row);
        grid.add(datePicker, 1, row++);
        
        grid.add(new Label("Type:"), 0, row);
        grid.add(typeCombo, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionArea, 1, row++);
        
        // Recurrence section
        VBox recurrenceBox = new VBox(5);
        recurrenceBox.getChildren().addAll(recurringCheckBox, recurrenceRuleField);
        grid.add(new Label("Recurrence:"), 0, row);
        grid.add(recurrenceBox, 1, row++);
        
        grid.add(new Label("Work Override:"), 0, row);
        grid.add(workingHolidayAllowedCheckBox, 1, row++);
        
        grid.add(new Label("Department:"), 0, row);
        grid.add(departmentField, 1, row++);
        
        grid.add(new Label("Status:"), 0, row);
        grid.add(activeCheckBox, 1, row++);
        
        return grid;
    }
    
    private void setupValidation() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        
        // Enable OK button only when required fields are filled
        okButton.disableProperty().bind(
            nameField.textProperty().isEmpty()
                .or(datePicker.valueProperty().isNull())
                .or(typeCombo.valueProperty().isNull())
        );
    }
    
    private void populateFields(CompanyHoliday holiday) {
        nameField.setText(holiday.getName());
        datePicker.setValue(holiday.getDate());
        typeCombo.setValue(holiday.getType());
        descriptionArea.setText(holiday.getDescription() != null ? holiday.getDescription() : "");
        recurringCheckBox.setSelected(holiday.isRecurring());
        recurrenceRuleField.setText(holiday.getRecurrenceRule() != null ? holiday.getRecurrenceRule() : "");
        workingHolidayAllowedCheckBox.setSelected(holiday.isWorkingHolidayAllowed());
        departmentField.setText(holiday.getDepartment() != null ? holiday.getDepartment() : "");
        activeCheckBox.setSelected(holiday.isActive());
        
        // Update field state based on recurring checkbox
        recurrenceRuleField.setDisable(!recurringCheckBox.isSelected());
    }
    
    private CompanyHoliday createHolidayFromFields(CompanyHoliday existing) {
        CompanyHoliday holiday = existing != null ? existing : new CompanyHoliday();
        
        holiday.setName(nameField.getText().trim());
        holiday.setDate(datePicker.getValue());
        holiday.setType(typeCombo.getValue());
        
        String description = descriptionArea.getText().trim();
        holiday.setDescription(description.isEmpty() ? null : description);
        
        holiday.setRecurring(recurringCheckBox.isSelected());
        
        String recurrenceRule = recurrenceRuleField.getText().trim();
        holiday.setRecurrenceRule(recurrenceRule.isEmpty() ? null : recurrenceRule);
        
        holiday.setWorkingHolidayAllowed(workingHolidayAllowedCheckBox.isSelected());
        
        String department = departmentField.getText().trim();
        holiday.setDepartment(department.isEmpty() ? null : department);
        
        holiday.setActive(activeCheckBox.isSelected());
        
        return holiday;
    }
    
    // Static utility methods for showing details and confirmation dialogs
    public static void showHolidayDetails(CompanyHoliday holiday) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Holiday Details");
        alert.setHeaderText(String.format("%s - %s", holiday.getName(), holiday.getDate()));
        
        StringBuilder content = new StringBuilder();
        content.append(String.format("Name: %s\n", holiday.getName()));
        content.append(String.format("Date: %s\n", holiday.getDate()));
        content.append(String.format("Type: %s\n", holiday.getType()));
        
        if (holiday.getDescription() != null) {
            content.append(String.format("Description: %s\n", holiday.getDescription()));
        }
        
        if (holiday.isRecurring()) {
            content.append(String.format("Recurring: %s\n", holiday.getRecurrenceRule()));
        }
        
        content.append(String.format("Work Override: %s\n", 
            holiday.isWorkingHolidayAllowed() ? "Allowed with authorization" : "Not allowed"));
        
        if (holiday.getDepartment() != null) {
            content.append(String.format("Department: %s\n", holiday.getDepartment()));
        } else {
            content.append("Scope: Company-wide\n");
        }
        
        content.append(String.format("Status: %s\n", holiday.isActive() ? "Active" : "Inactive"));
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
    
    public static boolean showDeleteConfirmation(CompanyHoliday holiday) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Holiday");
        alert.setHeaderText("Are you sure you want to delete this holiday?");
        alert.setContentText(String.format("%s (%s)\n\nThis action cannot be undone.", 
            holiday.getName(), 
            holiday.getDate()));
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}