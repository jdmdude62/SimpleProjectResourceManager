package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.UtilizationSettings;
import com.subliminalsearch.simpleprojectresourcemanager.model.UtilizationSettings.IndustryPreset;
import com.subliminalsearch.simpleprojectresourcemanager.model.UtilizationSettings.CalculationMethod;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class UtilizationSettingsDialog extends Dialog<UtilizationSettings> {
    
    private UtilizationSettings settings;
    
    // Controls
    private ToggleGroup presetGroup;
    private CheckBox includeWeekendsCheck;
    private CheckBox includeSaturdaysCheck;
    private CheckBox includeHolidaysCheck;
    private CheckBox countPtoCheck;
    private CheckBox countShopCheck;
    private CheckBox countTrainingCheck;
    
    private Slider workWeekSlider;
    private Slider hoursPerDaySlider;
    private Slider overtimeSlider;
    
    private Slider targetUtilizationSlider;
    private Slider minUtilizationSlider;
    private Slider overallocationSlider;
    
    private Slider targetBillableSlider;
    private Slider minBillableSlider;
    
    private ComboBox<CalculationMethod> methodCombo;
    
    private TextArea previewArea;
    
    public UtilizationSettingsDialog(UtilizationSettings currentSettings, Window owner) {
        this.settings = currentSettings != null ? currentSettings : new UtilizationSettings();
        
        setTitle("Resource Utilization Settings");
        setHeaderText("Configure how resource utilization is calculated");
        
        if (owner != null) {
            initOwner(owner);
        }
        
        // Create the main content
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Tab 1: Industry Presets
        Tab presetsTab = new Tab("Industry Presets");
        presetsTab.setContent(createPresetsPane());
        
        // Tab 2: Basic Settings
        Tab basicTab = new Tab("Basic Settings");
        basicTab.setContent(createBasicSettingsPane());
        
        // Tab 3: Targets
        Tab targetsTab = new Tab("Targets & Thresholds");
        targetsTab.setContent(createTargetsPane());
        
        // Tab 4: Preview
        Tab previewTab = new Tab("Preview");
        previewTab.setContent(createPreviewPane());
        
        tabPane.getTabs().addAll(presetsTab, basicTab, targetsTab, previewTab);
        
        // Set up dialog
        getDialogPane().setContent(tabPane);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefSize(750, 600);
        
        // Apply current settings to controls
        applySettingsToControls();
        
        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                saveControlsToSettings();
                return settings;
            }
            return null;
        });
    }
    
    private ScrollPane createPresetsPane() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        Label title = new Label("Select an Industry Preset");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Label subtitle = new Label("Choose a preset configuration based on your industry, then fine-tune as needed.");
        subtitle.setWrapText(true);
        
        content.getChildren().addAll(title, subtitle, new Separator());
        
        presetGroup = new ToggleGroup();
        
        for (IndustryPreset preset : IndustryPreset.values()) {
            VBox presetBox = createPresetOption(preset);
            content.getChildren().add(presetBox);
        }
        
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        return scroll;
    }
    
    private VBox createPresetOption(IndustryPreset preset) {
        RadioButton radio = new RadioButton();
        radio.setToggleGroup(presetGroup);
        radio.setUserData(preset);
        
        Label nameLabel = new Label(preset.getDisplayName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        Label descLabel = new Label(preset.getDescription());
        descLabel.setTextFill(Color.GRAY);
        descLabel.setWrapText(true);
        
        // Show key settings for this preset
        String details = getPresetDetails(preset);
        Label detailsLabel = new Label(details);
        detailsLabel.setFont(Font.font("System", 11));
        detailsLabel.setTextFill(Color.DARKBLUE);
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(radio, nameLabel);
        
        VBox content = new VBox(5);
        content.setPadding(new Insets(0, 0, 10, 30));
        content.getChildren().addAll(descLabel, detailsLabel);
        
        VBox presetBox = new VBox(5);
        presetBox.getChildren().addAll(header, content);
        
        // Apply preset when selected
        radio.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                settings.applyPreset(preset);
                applySettingsToControls();
                updatePreview();
            }
        });
        
        return presetBox;
    }
    
    private String getPresetDetails(IndustryPreset preset) {
        UtilizationSettings temp = new UtilizationSettings();
        temp.applyPreset(preset);
        
        StringBuilder details = new StringBuilder();
        details.append("• Work Week: ").append((int)temp.getStandardWorkWeek()).append(" hrs");
        details.append("  • Target Util: ").append((int)temp.getTargetUtilization()).append("%");
        details.append("  • Target Billable: ").append((int)temp.getTargetBillable()).append("%");
        if (temp.isIncludeWeekends()) details.append("  • Includes weekends");
        if (temp.isIncludeSaturdays()) details.append("  • Includes Saturdays");
        if (temp.isCountShopAsUtilized()) details.append("  • SHOP counts as utilized");
        
        return details.toString();
    }
    
    private ScrollPane createBasicSettingsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // Section: Time Inclusion
        Label timeLabel = new Label("Time Inclusion");
        timeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        grid.add(timeLabel, 0, row++, 2, 1);
        
        includeWeekendsCheck = new CheckBox("Include all weekends in available time");
        grid.add(includeWeekendsCheck, 0, row++, 2, 1);
        
        includeSaturdaysCheck = new CheckBox("Include Saturdays only");
        grid.add(includeSaturdaysCheck, 0, row++, 2, 1);
        
        includeHolidaysCheck = new CheckBox("Include company holidays in available time");
        grid.add(includeHolidaysCheck, 0, row++, 2, 1);
        
        // Disable Saturdays checkbox if full weekends are included
        includeWeekendsCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            includeSaturdaysCheck.setDisable(isSelected);
            if (isSelected) includeSaturdaysCheck.setSelected(false);
        });
        
        grid.add(new Separator(), 0, row++, 2, 1);
        
        // Section: What Counts as Utilized
        Label utilLabel = new Label("What Counts as Utilized Time");
        utilLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        grid.add(utilLabel, 0, row++, 2, 1);
        
        countPtoCheck = new CheckBox("Count PTO/vacation as utilized time");
        grid.add(countPtoCheck, 0, row++, 2, 1);
        
        countShopCheck = new CheckBox("Count SHOP assignments as utilized time");
        grid.add(countShopCheck, 0, row++, 2, 1);
        
        countTrainingCheck = new CheckBox("Count TRAINING assignments as utilized time");
        grid.add(countTrainingCheck, 0, row++, 2, 1);
        
        grid.add(new Separator(), 0, row++, 2, 1);
        
        // Section: Working Hours
        Label hoursLabel = new Label("Working Hours");
        hoursLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        grid.add(hoursLabel, 0, row++, 2, 1);
        
        grid.add(new Label("Standard work week:"), 0, row);
        workWeekSlider = createSlider(35, 50, 40, 5);
        Label workWeekValue = new Label("40 hours");
        workWeekSlider.valueProperty().addListener((obs, old, val) -> 
            workWeekValue.setText(String.format("%.0f hours", val.doubleValue())));
        HBox workWeekBox = new HBox(10, workWeekSlider, workWeekValue);
        grid.add(workWeekBox, 1, row++);
        
        grid.add(new Label("Hours per day:"), 0, row);
        hoursPerDaySlider = createSlider(7, 12, 8, 1);
        Label hoursValue = new Label("8.0 hours");
        hoursPerDaySlider.valueProperty().addListener((obs, old, val) -> 
            hoursValue.setText(String.format("%.1f hours", val.doubleValue())));
        HBox hoursBox = new HBox(10, hoursPerDaySlider, hoursValue);
        grid.add(hoursBox, 1, row++);
        
        grid.add(new Separator(), 0, row++, 2, 1);
        
        // Section: Calculation Method
        Label methodLabel = new Label("Calculation Method");
        methodLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        grid.add(methodLabel, 0, row++, 2, 1);
        
        grid.add(new Label("Method:"), 0, row);
        methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll(CalculationMethod.values());
        methodCombo.setConverter(new StringConverter<CalculationMethod>() {
            @Override
            public String toString(CalculationMethod method) {
                return method != null ? method.getDisplayName() : "";
            }
            @Override
            public CalculationMethod fromString(String string) {
                return null;
            }
        });
        methodCombo.setPrefWidth(300);
        grid.add(methodCombo, 1, row++);
        
        // Add change listeners to update preview
        includeWeekendsCheck.selectedProperty().addListener((obs, old, val) -> updatePreview());
        includeSaturdaysCheck.selectedProperty().addListener((obs, old, val) -> updatePreview());
        includeHolidaysCheck.selectedProperty().addListener((obs, old, val) -> updatePreview());
        countPtoCheck.selectedProperty().addListener((obs, old, val) -> updatePreview());
        countShopCheck.selectedProperty().addListener((obs, old, val) -> updatePreview());
        countTrainingCheck.selectedProperty().addListener((obs, old, val) -> updatePreview());
        workWeekSlider.valueProperty().addListener((obs, old, val) -> updatePreview());
        hoursPerDaySlider.valueProperty().addListener((obs, old, val) -> updatePreview());
        methodCombo.valueProperty().addListener((obs, old, val) -> updatePreview());
        
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        return scroll;
    }
    
    private ScrollPane createTargetsPane() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // Section: Utilization Targets
        Label utilLabel = new Label("Utilization Targets");
        utilLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        grid.add(utilLabel, 0, row++, 2, 1);
        
        grid.add(new Label("Target utilization:"), 0, row);
        targetUtilizationSlider = createSlider(60, 95, 80, 5);
        Label targetUtilValue = new Label("80%");
        targetUtilizationSlider.valueProperty().addListener((obs, old, val) -> 
            targetUtilValue.setText(String.format("%.0f%%", val.doubleValue())));
        HBox targetUtilBox = new HBox(10, targetUtilizationSlider, targetUtilValue);
        grid.add(targetUtilBox, 1, row++);
        
        grid.add(new Label("Minimum acceptable:"), 0, row);
        minUtilizationSlider = createSlider(40, 80, 65, 5);
        Label minUtilValue = new Label("65%");
        minUtilizationSlider.valueProperty().addListener((obs, old, val) -> 
            minUtilValue.setText(String.format("%.0f%%", val.doubleValue())));
        HBox minUtilBox = new HBox(10, minUtilizationSlider, minUtilValue);
        grid.add(minUtilBox, 1, row++);
        
        grid.add(new Label("Overallocation alert:"), 0, row);
        overallocationSlider = createSlider(100, 150, 110, 10);
        Label overValue = new Label("110%");
        overallocationSlider.valueProperty().addListener((obs, old, val) -> 
            overValue.setText(String.format("%.0f%%", val.doubleValue())));
        HBox overBox = new HBox(10, overallocationSlider, overValue);
        grid.add(overBox, 1, row++);
        
        grid.add(new Separator(), 0, row++, 2, 1);
        
        // Section: Billable Targets
        Label billLabel = new Label("Billable Targets");
        billLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        grid.add(billLabel, 0, row++, 2, 1);
        
        grid.add(new Label("Target billable:"), 0, row);
        targetBillableSlider = createSlider(50, 100, 75, 5);
        Label targetBillValue = new Label("75%");
        targetBillableSlider.valueProperty().addListener((obs, old, val) -> 
            targetBillValue.setText(String.format("%.0f%%", val.doubleValue())));
        HBox targetBillBox = new HBox(10, targetBillableSlider, targetBillValue);
        grid.add(targetBillBox, 1, row++);
        
        grid.add(new Label("Minimum billable:"), 0, row);
        minBillableSlider = createSlider(30, 80, 60, 5);
        Label minBillValue = new Label("60%");
        minBillableSlider.valueProperty().addListener((obs, old, val) -> 
            minBillValue.setText(String.format("%.0f%%", val.doubleValue())));
        HBox minBillBox = new HBox(10, minBillableSlider, minBillValue);
        grid.add(minBillBox, 1, row++);
        
        // Add change listeners to update preview
        targetUtilizationSlider.valueProperty().addListener((obs, old, val) -> updatePreview());
        minUtilizationSlider.valueProperty().addListener((obs, old, val) -> updatePreview());
        overallocationSlider.valueProperty().addListener((obs, old, val) -> updatePreview());
        targetBillableSlider.valueProperty().addListener((obs, old, val) -> updatePreview());
        minBillableSlider.valueProperty().addListener((obs, old, val) -> updatePreview());
        
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        return scroll;
    }
    
    private VBox createPreviewPane() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label title = new Label("Example Calculation");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        Label subtitle = new Label("Shows how utilization would be calculated with current settings:");
        
        previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setPrefRowCount(20);
        previewArea.setFont(Font.font("Courier New", 12));
        
        content.getChildren().addAll(title, subtitle, previewArea);
        
        updatePreview();
        
        return content;
    }
    
    private Slider createSlider(double min, double max, double value, double tick) {
        Slider slider = new Slider(min, max, value);
        slider.setMajorTickUnit(tick);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setShowTickMarks(true);
        slider.setPrefWidth(200);
        return slider;
    }
    
    private void applySettingsToControls() {
        includeWeekendsCheck.setSelected(settings.isIncludeWeekends());
        includeSaturdaysCheck.setSelected(settings.isIncludeSaturdays());
        includeHolidaysCheck.setSelected(settings.isIncludeHolidays());
        countPtoCheck.setSelected(settings.isCountPtoAsUtilized());
        countShopCheck.setSelected(settings.isCountShopAsUtilized());
        countTrainingCheck.setSelected(settings.isCountTrainingAsUtilized());
        
        workWeekSlider.setValue(settings.getStandardWorkWeek());
        hoursPerDaySlider.setValue(settings.getHoursPerDay());
        
        targetUtilizationSlider.setValue(settings.getTargetUtilization());
        minUtilizationSlider.setValue(settings.getMinimumUtilization());
        overallocationSlider.setValue(settings.getOverallocationAlert());
        
        targetBillableSlider.setValue(settings.getTargetBillable());
        minBillableSlider.setValue(settings.getMinimumBillable());
        
        methodCombo.setValue(settings.getCalculationMethod());
    }
    
    private void saveControlsToSettings() {
        settings.setIncludeWeekends(includeWeekendsCheck.isSelected());
        settings.setIncludeSaturdays(includeSaturdaysCheck.isSelected());
        settings.setIncludeHolidays(includeHolidaysCheck.isSelected());
        settings.setCountPtoAsUtilized(countPtoCheck.isSelected());
        settings.setCountShopAsUtilized(countShopCheck.isSelected());
        settings.setCountTrainingAsUtilized(countTrainingCheck.isSelected());
        
        settings.setStandardWorkWeek(workWeekSlider.getValue());
        settings.setHoursPerDay(hoursPerDaySlider.getValue());
        
        settings.setTargetUtilization(targetUtilizationSlider.getValue());
        settings.setMinimumUtilization(minUtilizationSlider.getValue());
        settings.setOverallocationAlert(overallocationSlider.getValue());
        
        settings.setTargetBillable(targetBillableSlider.getValue());
        settings.setMinimumBillable(minBillableSlider.getValue());
        
        settings.setCalculationMethod(methodCombo.getValue());
    }
    
    private void updatePreview() {
        saveControlsToSettings();
        
        StringBuilder preview = new StringBuilder();
        preview.append("EXAMPLE: Resource 'John Smith' in August 2025\n");
        preview.append("=" .repeat(50)).append("\n\n");
        
        preview.append("GIVEN:\n");
        preview.append("• Date Range: Aug 1-31 (31 calendar days)\n");
        preview.append("• Weekdays: 21 days (Mon-Fri)\n");
        preview.append("• Saturdays: 5 days\n");
        preview.append("• Sundays: 5 days\n");
        preview.append("• Holidays: 0 days (no August holidays)\n");
        preview.append("• Assignments: 15 days on projects, 3 days SHOP\n");
        preview.append("• PTO: 2 days vacation\n\n");
        
        preview.append("CALCULATION:\n");
        
        // Calculate available days based on settings
        int availableDays = 21; // weekdays
        if (settings.isIncludeWeekends()) {
            availableDays += 10; // add weekends
        } else if (settings.isIncludeSaturdays()) {
            availableDays += 5; // add Saturdays only
        }
        
        preview.append("• Available Days: ").append(availableDays);
        preview.append(" (weekdays");
        if (settings.isIncludeWeekends()) {
            preview.append(" + weekends");
        } else if (settings.isIncludeSaturdays()) {
            preview.append(" + Saturdays");
        }
        preview.append(")\n");
        
        // Calculate utilized days
        int utilizedDays = 15; // project days
        if (settings.isCountShopAsUtilized()) {
            utilizedDays += 3;
        }
        if (settings.isCountPtoAsUtilized()) {
            utilizedDays += 2;
        }
        
        preview.append("• Utilized Days: ").append(utilizedDays);
        preview.append(" (projects");
        if (settings.isCountShopAsUtilized()) {
            preview.append(" + SHOP");
        }
        if (settings.isCountPtoAsUtilized()) {
            preview.append(" + PTO");
        }
        preview.append(")\n");
        
        // Calculate percentages
        double utilization = (utilizedDays * 100.0) / availableDays;
        double billable = (15 * 100.0) / Math.max(utilizedDays, 1);
        
        preview.append("\n");
        preview.append("RESULTS:\n");
        preview.append(String.format("• Utilization: %.1f%% (%d/%d days)\n", utilization, utilizedDays, availableDays));
        preview.append(String.format("• Billable: %.1f%% (%d/%d utilized days)\n", billable, 15, utilizedDays));
        
        preview.append("\n");
        preview.append("COLOR CODING:\n");
        if (utilization > settings.getOverallocationAlert()) {
            preview.append("• Bar Color: RED (overallocated)\n");
        } else if (utilization >= settings.getTargetUtilization()) {
            if (billable >= settings.getTargetBillable()) {
                preview.append("• Bar Color: GREEN (on target)\n");
            } else {
                preview.append("• Bar Color: YELLOW (low billable)\n");
            }
        } else if (utilization >= settings.getMinimumUtilization()) {
            preview.append("• Bar Color: BLUE (below target)\n");
        } else {
            preview.append("• Bar Color: GRAY (underutilized)\n");
        }
        
        previewArea.setText(preview.toString());
    }
}