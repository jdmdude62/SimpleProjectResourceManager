package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ResourceQualificationsView {
    private static final Logger logger = LoggerFactory.getLogger(ResourceQualificationsView.class);
    
    private final Stage stage;
    private final SchedulingService schedulingService;
    private final CertificationRepository certificationRepo;
    private final SkillRepository skillRepo;
    private final ResourceCertificationRepository resourceCertRepo;
    private final ResourceSkillRepository resourceSkillRepo;
    
    private ComboBox<Resource> resourceCombo;
    private TabPane tabPane;
    private TableView<ResourceCertification> certificationTable;
    private TableView<ResourceSkill> skillTable;
    private ObservableList<ResourceCertification> resourceCertifications;
    private ObservableList<ResourceSkill> resourceSkills;
    private Resource selectedResource;
    
    public ResourceQualificationsView(Window owner, SchedulingService schedulingService) {
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.initOwner(owner);
        this.stage.setTitle("Resource Qualifications Management");
        this.schedulingService = schedulingService;
        
        // Initialize repositories
        DatabaseConfig dbConfig = new DatabaseConfig();
        this.certificationRepo = new CertificationRepository(dbConfig.getDataSource());
        this.skillRepo = new SkillRepository(dbConfig.getDataSource());
        this.resourceCertRepo = new ResourceCertificationRepository(dbConfig.getDataSource());
        this.resourceSkillRepo = new ResourceSkillRepository(dbConfig.getDataSource());
        
        initializeUI();
        loadResources();
        
        // Set size
        this.stage.setWidth(1200);
        this.stage.setHeight(700);
        
        // Center on owner if available
        if (owner != null) {
            stage.setX(owner.getX() + (owner.getWidth() - 1200) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 700) / 2);
        }
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Header
        Label titleLabel = new Label("Manage Resource Qualifications");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        // Resource selector
        HBox resourceSelector = new HBox(10);
        resourceSelector.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label resourceLabel = new Label("Select Resource:");
        resourceLabel.setStyle("-fx-font-size: 14px;");
        
        resourceCombo = new ComboBox<>();
        resourceCombo.setPrefWidth(300);
        resourceCombo.setPromptText("Select a resource...");
        resourceCombo.setOnAction(e -> loadResourceQualifications());
        
        // Set converter to display resource names properly
        resourceCombo.setConverter(new javafx.util.StringConverter<Resource>() {
            @Override
            public String toString(Resource resource) {
                return resource != null ? resource.getName() : "";
            }
            
            @Override
            public Resource fromString(String string) {
                return resourceCombo.getItems().stream()
                    .filter(r -> r.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        resourceSelector.getChildren().addAll(resourceLabel, resourceCombo);
        
        // Tab pane for certifications and skills
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Certifications tab
        Tab certTab = new Tab("Certifications");
        certTab.setContent(createCertificationsTab());
        
        // Skills tab
        Tab skillsTab = new Tab("Skills");
        skillsTab.setContent(createSkillsTab());
        
        tabPane.getTabs().addAll(certTab, skillsTab);
        
        // Layout
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        root.getChildren().addAll(titleLabel, resourceSelector, tabPane);
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
    }
    
    private VBox createCertificationsTab() {
        VBox certBox = new VBox(10);
        certBox.setPadding(new Insets(10));
        
        // Toolbar
        HBox toolbar = new HBox(10);
        Button addCertBtn = new Button("Add Certification");
        Button editCertBtn = new Button("Edit");
        Button removeCertBtn = new Button("Remove");
        Button refreshCertBtn = new Button("Refresh");
        
        addCertBtn.setOnAction(e -> addCertificationToResource());
        editCertBtn.setOnAction(e -> editResourceCertification());
        removeCertBtn.setOnAction(e -> removeResourceCertification());
        refreshCertBtn.setOnAction(e -> loadResourceCertifications());
        
        toolbar.getChildren().addAll(addCertBtn, editCertBtn, removeCertBtn, refreshCertBtn);
        
        // Table
        certificationTable = new TableView<>();
        resourceCertifications = FXCollections.observableArrayList();
        certificationTable.setItems(resourceCertifications);
        
        // Columns
        TableColumn<ResourceCertification, String> certNameCol = new TableColumn<>("Certification");
        certNameCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCertificationName()));
        certNameCol.setPrefWidth(250);
        
        TableColumn<ResourceCertification, String> dateObtainedCol = new TableColumn<>("Date Obtained");
        dateObtainedCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDateObtained();
            return new SimpleStringProperty(date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
        });
        dateObtainedCol.setPrefWidth(120);
        
        TableColumn<ResourceCertification, String> expiryDateCol = new TableColumn<>("Expiry Date");
        expiryDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getExpiryDate();
            String dateStr = date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "Never";
            if (cellData.getValue().isExpired()) {
                dateStr += " (EXPIRED)";
            } else if (cellData.getValue().isExpiringSoon()) {
                dateStr += " (EXPIRING SOON)";
            }
            return new SimpleStringProperty(dateStr);
        });
        expiryDateCol.setPrefWidth(150);
        
        TableColumn<ResourceCertification, String> certNumberCol = new TableColumn<>("Cert Number");
        certNumberCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCertificationNumber()));
        certNumberCol.setPrefWidth(150);
        
        TableColumn<ResourceCertification, String> proficiencyCol = new TableColumn<>("Proficiency");
        proficiencyCol.setCellValueFactory(cellData -> {
            Integer score = cellData.getValue().getProficiencyScore();
            String desc = cellData.getValue().getProficiencyDescription();
            return new SimpleStringProperty(score != null ? score + " - " + desc : "Not Rated");
        });
        proficiencyCol.setPrefWidth(150);
        
        TableColumn<ResourceCertification, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getNotes()));
        notesCol.setPrefWidth(200);
        
        certificationTable.getColumns().addAll(certNameCol, dateObtainedCol, expiryDateCol, 
            certNumberCol, proficiencyCol, notesCol);
        
        VBox.setVgrow(certificationTable, Priority.ALWAYS);
        certBox.getChildren().addAll(toolbar, certificationTable);
        
        return certBox;
    }
    
    private VBox createSkillsTab() {
        VBox skillBox = new VBox(10);
        skillBox.setPadding(new Insets(10));
        
        // Toolbar
        HBox toolbar = new HBox(10);
        Button addSkillBtn = new Button("Add Skill");
        Button editSkillBtn = new Button("Edit");
        Button removeSkillBtn = new Button("Remove");
        Button refreshSkillBtn = new Button("Refresh");
        
        addSkillBtn.setOnAction(e -> addSkillToResource());
        editSkillBtn.setOnAction(e -> editResourceSkill());
        removeSkillBtn.setOnAction(e -> removeResourceSkill());
        refreshSkillBtn.setOnAction(e -> loadResourceSkills());
        
        toolbar.getChildren().addAll(addSkillBtn, editSkillBtn, removeSkillBtn, refreshSkillBtn);
        
        // Table
        skillTable = new TableView<>();
        resourceSkills = FXCollections.observableArrayList();
        skillTable.setItems(resourceSkills);
        
        // Columns
        TableColumn<ResourceSkill, String> skillNameCol = new TableColumn<>("Skill");
        skillNameCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSkillName()));
        skillNameCol.setPrefWidth(250);
        
        TableColumn<ResourceSkill, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSkillCategory()));
        categoryCol.setPrefWidth(150);
        
        TableColumn<ResourceSkill, String> proficiencyCol = new TableColumn<>("Proficiency");
        proficiencyCol.setCellValueFactory(cellData -> {
            Integer level = cellData.getValue().getProficiencyLevel();
            String desc = cellData.getValue().getProficiencyDescription();
            return new SimpleStringProperty(level != null ? level + " - " + desc : "Not Rated");
        });
        proficiencyCol.setPrefWidth(150);
        
        TableColumn<ResourceSkill, String> yearsExpCol = new TableColumn<>("Years Experience");
        yearsExpCol.setCellValueFactory(cellData -> {
            Integer years = cellData.getValue().getYearsOfExperience();
            return new SimpleStringProperty(years != null ? years.toString() : "");
        });
        yearsExpCol.setPrefWidth(120);
        
        TableColumn<ResourceSkill, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getNotes()));
        notesCol.setPrefWidth(300);
        
        skillTable.getColumns().addAll(skillNameCol, categoryCol, proficiencyCol, yearsExpCol, notesCol);
        
        VBox.setVgrow(skillTable, Priority.ALWAYS);
        skillBox.getChildren().addAll(toolbar, skillTable);
        
        return skillBox;
    }
    
    private void loadResources() {
        try {
            List<Resource> resources = schedulingService.getAllResources();
            resourceCombo.getItems().clear();
            resourceCombo.getItems().addAll(resources);
        } catch (Exception e) {
            logger.error("Failed to load resources", e);
            showError("Failed to load resources: " + e.getMessage());
        }
    }
    
    private void loadResourceQualifications() {
        selectedResource = resourceCombo.getValue();
        if (selectedResource == null) {
            resourceCertifications.clear();
            resourceSkills.clear();
            return;
        }
        
        loadResourceCertifications();
        loadResourceSkills();
    }
    
    private void loadResourceCertifications() {
        if (selectedResource == null) return;
        
        try {
            resourceCertifications.clear();
            List<ResourceCertification> certs = resourceCertRepo.findByResourceId(selectedResource.getId());
            
            // Load certification names
            for (ResourceCertification rc : certs) {
                Optional<Certification> cert = certificationRepo.findById(rc.getCertificationId());
                cert.ifPresent(c -> rc.setCertificationName(c.getName()));
            }
            
            resourceCertifications.addAll(certs);
        } catch (Exception e) {
            logger.error("Failed to load resource certifications", e);
            showError("Failed to load certifications: " + e.getMessage());
        }
    }
    
    private void loadResourceSkills() {
        if (selectedResource == null) return;
        
        try {
            resourceSkills.clear();
            List<ResourceSkill> skills = resourceSkillRepo.findByResourceId(selectedResource.getId());
            
            // Load skill names and categories
            for (ResourceSkill rs : skills) {
                Optional<Skill> skill = skillRepo.findById(rs.getSkillId());
                skill.ifPresent(s -> {
                    rs.setSkillName(s.getName());
                    rs.setSkillCategory(s.getCategory());
                });
            }
            
            resourceSkills.addAll(skills);
        } catch (Exception e) {
            logger.error("Failed to load resource skills", e);
            showError("Failed to load skills: " + e.getMessage());
        }
    }
    
    private void addCertificationToResource() {
        if (selectedResource == null) {
            showWarning("Please select a resource first.");
            return;
        }
        
        Dialog<ResourceCertification> dialog = new Dialog<>();
        dialog.setTitle("Add Certification to " + selectedResource.getName());
        dialog.setHeaderText("Select certification and enter details");
        dialog.initOwner(stage);
        
        // Form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        ComboBox<Certification> certCombo = new ComboBox<>();
        List<Certification> availableCerts = certificationRepo.findActive();
        certCombo.getItems().addAll(availableCerts);
        certCombo.setPromptText("Select certification");
        certCombo.setPrefWidth(300);
        certCombo.setConverter(new javafx.util.StringConverter<Certification>() {
            @Override
            public String toString(Certification cert) {
                return cert != null ? cert.getName() : "";
            }
            @Override
            public Certification fromString(String string) {
                return certCombo.getItems().stream()
                    .filter(c -> c.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        DatePicker dateObtainedPicker = new DatePicker(LocalDate.now());
        DatePicker expiryDatePicker = new DatePicker();
        TextField certNumberField = new TextField();
        certNumberField.setPromptText("Certification number (optional)");
        
        ComboBox<Integer> proficiencyCombo = new ComboBox<>();
        proficiencyCombo.getItems().addAll(1, 2, 3, 4, 5);
        proficiencyCombo.setValue(3);
        proficiencyCombo.setPromptText("Select proficiency level");
        
        Label proficiencyLabel = new Label("3 - Intermediate");
        proficiencyCombo.setOnAction(e -> {
            Integer val = proficiencyCombo.getValue();
            if (val != null) {
                switch (val) {
                    case 1: proficiencyLabel.setText("1 - Beginner"); break;
                    case 2: proficiencyLabel.setText("2 - Basic"); break;
                    case 3: proficiencyLabel.setText("3 - Intermediate"); break;
                    case 4: proficiencyLabel.setText("4 - Advanced"); break;
                    case 5: proficiencyLabel.setText("5 - Expert"); break;
                }
            }
        });
        
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes (optional)");
        notesArea.setPrefRowCount(3);
        
        grid.add(new Label("Certification:*"), 0, 0);
        grid.add(certCombo, 1, 0);
        grid.add(new Label("Date Obtained:*"), 0, 1);
        grid.add(dateObtainedPicker, 1, 1);
        grid.add(new Label("Expiry Date:"), 0, 2);
        grid.add(expiryDatePicker, 1, 2);
        grid.add(new Label("Cert Number:"), 0, 3);
        grid.add(certNumberField, 1, 3);
        grid.add(new Label("Proficiency:*"), 0, 4);
        grid.add(proficiencyCombo, 1, 4);
        grid.add(proficiencyLabel, 2, 4);
        grid.add(new Label("Notes:"), 0, 5);
        grid.add(notesArea, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && certCombo.getValue() != null) {
                ResourceCertification rc = new ResourceCertification();
                rc.setResourceId(selectedResource.getId());
                rc.setCertificationId(certCombo.getValue().getId());
                rc.setDateObtained(dateObtainedPicker.getValue());
                rc.setExpiryDate(expiryDatePicker.getValue());
                rc.setCertificationNumber(certNumberField.getText());
                rc.setProficiencyScore(proficiencyCombo.getValue());
                rc.setNotes(notesArea.getText());
                return rc;
            }
            return null;
        });
        
        Optional<ResourceCertification> result = dialog.showAndWait();
        result.ifPresent(rc -> {
            try {
                resourceCertRepo.save(rc);
                loadResourceCertifications();
                showInfo("Certification added successfully.");
            } catch (Exception e) {
                logger.error("Failed to add certification", e);
                showError("Failed to add certification: " + e.getMessage());
            }
        });
    }
    
    private void addSkillToResource() {
        if (selectedResource == null) {
            showWarning("Please select a resource first.");
            return;
        }
        
        Dialog<ResourceSkill> dialog = new Dialog<>();
        dialog.setTitle("Add Skill to " + selectedResource.getName());
        dialog.setHeaderText("Select skill and enter details");
        dialog.initOwner(stage);
        
        // Form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Category filter ComboBox
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().add("All Skills");
        List<String> categories = skillRepo.findAllCategories();
        categoryCombo.getItems().addAll(categories);
        categoryCombo.setValue("All Skills");
        categoryCombo.setPrefWidth(300);
        
        ComboBox<Skill> skillCombo = new ComboBox<>();
        List<Skill> allSkills = skillRepo.findActive();
        skillCombo.getItems().addAll(allSkills);
        skillCombo.setPromptText("Select skill");
        skillCombo.setPrefWidth(300);
        skillCombo.setConverter(new javafx.util.StringConverter<Skill>() {
            @Override
            public String toString(Skill skill) {
                return skill != null ? skill.getName() : "";
            }
            @Override
            public Skill fromString(String string) {
                return skillCombo.getItems().stream()
                    .filter(s -> s.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        // Category filter action
        categoryCombo.setOnAction(e -> {
            String selectedCategory = categoryCombo.getValue();
            skillCombo.getItems().clear();
            
            if ("All Skills".equals(selectedCategory)) {
                skillCombo.getItems().addAll(allSkills);
            } else {
                List<Skill> filteredSkills = allSkills.stream()
                    .filter(s -> selectedCategory.equals(s.getCategory()))
                    .collect(java.util.stream.Collectors.toList());
                skillCombo.getItems().addAll(filteredSkills);
            }
            skillCombo.setValue(null); // Clear selection when category changes
        });
        
        ComboBox<Integer> proficiencyCombo = new ComboBox<>();
        proficiencyCombo.getItems().addAll(1, 2, 3, 4, 5);
        proficiencyCombo.setValue(3);
        proficiencyCombo.setPromptText("Select proficiency level");
        
        Label proficiencyLabel = new Label("3 - Intermediate");
        proficiencyCombo.setOnAction(e -> {
            Integer val = proficiencyCombo.getValue();
            if (val != null) {
                switch (val) {
                    case 1: proficiencyLabel.setText("1 - Beginner"); break;
                    case 2: proficiencyLabel.setText("2 - Basic"); break;
                    case 3: proficiencyLabel.setText("3 - Intermediate"); break;
                    case 4: proficiencyLabel.setText("4 - Advanced"); break;
                    case 5: proficiencyLabel.setText("5 - Expert"); break;
                }
            }
        });
        
        TextField yearsExpField = new TextField();
        yearsExpField.setPromptText("Years of experience (optional)");
        
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes (optional)");
        notesArea.setPrefRowCount(3);
        
        grid.add(new Label("Category:"), 0, 0);
        grid.add(categoryCombo, 1, 0);
        grid.add(new Label("Skill:*"), 0, 1);
        grid.add(skillCombo, 1, 1);
        grid.add(new Label("Proficiency:*"), 0, 2);
        grid.add(proficiencyCombo, 1, 2);
        grid.add(proficiencyLabel, 2, 2);
        grid.add(new Label("Years Experience:"), 0, 3);
        grid.add(yearsExpField, 1, 3);
        grid.add(new Label("Notes:"), 0, 4);
        grid.add(notesArea, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && skillCombo.getValue() != null) {
                ResourceSkill rs = new ResourceSkill();
                rs.setResourceId(selectedResource.getId());
                rs.setSkillId(skillCombo.getValue().getId());
                rs.setProficiencyLevel(proficiencyCombo.getValue());
                
                String yearsText = yearsExpField.getText().trim();
                if (!yearsText.isEmpty()) {
                    try {
                        rs.setYearsOfExperience(Integer.parseInt(yearsText));
                    } catch (NumberFormatException e) {
                        // Ignore invalid input
                    }
                }
                
                rs.setNotes(notesArea.getText());
                return rs;
            }
            return null;
        });
        
        Optional<ResourceSkill> result = dialog.showAndWait();
        result.ifPresent(rs -> {
            try {
                resourceSkillRepo.save(rs);
                loadResourceSkills();
                showInfo("Skill added successfully.");
            } catch (Exception e) {
                logger.error("Failed to add skill", e);
                showError("Failed to add skill: " + e.getMessage());
            }
        });
    }
    
    private void editResourceCertification() {
        ResourceCertification selected = certificationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a certification to edit.");
            return;
        }
        
        Dialog<ResourceCertification> dialog = new Dialog<>();
        dialog.setTitle("Edit Certification for " + selectedResource.getName());
        dialog.setHeaderText("Update certification details");
        dialog.initOwner(stage);
        
        // Form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Get the current certification
        Certification currentCert = certificationRepo.findById(selected.getCertificationId()).orElse(null);
        
        ComboBox<Certification> certCombo = new ComboBox<>();
        List<Certification> availableCerts = certificationRepo.findActive();
        certCombo.getItems().addAll(availableCerts);
        certCombo.setValue(currentCert);
        certCombo.setPrefWidth(300);
        certCombo.setConverter(new javafx.util.StringConverter<Certification>() {
            @Override
            public String toString(Certification cert) {
                return cert != null ? cert.getName() : "";
            }
            @Override
            public Certification fromString(String string) {
                return certCombo.getItems().stream()
                    .filter(c -> c.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        DatePicker dateObtainedPicker = new DatePicker(selected.getDateObtained());
        DatePicker expiryDatePicker = new DatePicker(selected.getExpiryDate());
        TextField certNumberField = new TextField(selected.getCertificationNumber() != null ? selected.getCertificationNumber() : "");
        certNumberField.setPromptText("Certification number (optional)");
        
        ComboBox<Integer> proficiencyCombo = new ComboBox<>();
        proficiencyCombo.getItems().addAll(1, 2, 3, 4, 5);
        proficiencyCombo.setValue(selected.getProficiencyScore());
        
        Label proficiencyLabel = new Label(getProficiencyText(selected.getProficiencyScore()));
        proficiencyCombo.setOnAction(e -> {
            Integer val = proficiencyCombo.getValue();
            if (val != null) {
                proficiencyLabel.setText(getProficiencyText(val));
            }
        });
        
        TextArea notesArea = new TextArea(selected.getNotes() != null ? selected.getNotes() : "");
        notesArea.setPromptText("Notes (optional)");
        notesArea.setPrefRowCount(3);
        
        grid.add(new Label("Certification:*"), 0, 0);
        grid.add(certCombo, 1, 0);
        grid.add(new Label("Date Obtained:*"), 0, 1);
        grid.add(dateObtainedPicker, 1, 1);
        grid.add(new Label("Expiry Date:"), 0, 2);
        grid.add(expiryDatePicker, 1, 2);
        grid.add(new Label("Cert Number:"), 0, 3);
        grid.add(certNumberField, 1, 3);
        grid.add(new Label("Proficiency:*"), 0, 4);
        grid.add(proficiencyCombo, 1, 4);
        grid.add(proficiencyLabel, 2, 4);
        grid.add(new Label("Notes:"), 0, 5);
        grid.add(notesArea, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && certCombo.getValue() != null) {
                selected.setCertificationId(certCombo.getValue().getId());
                selected.setDateObtained(dateObtainedPicker.getValue());
                selected.setExpiryDate(expiryDatePicker.getValue());
                selected.setCertificationNumber(certNumberField.getText().trim().isEmpty() ? null : certNumberField.getText().trim());
                selected.setProficiencyScore(proficiencyCombo.getValue());
                selected.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                return selected;
            }
            return null;
        });
        
        Optional<ResourceCertification> result = dialog.showAndWait();
        result.ifPresent(rc -> {
            try {
                resourceCertRepo.save(rc);
                loadResourceCertifications();
                showInfo("Certification updated successfully.");
            } catch (Exception e) {
                logger.error("Failed to update certification", e);
                showError("Failed to update certification: " + e.getMessage());
            }
        });
    }
    
    private void editResourceSkill() {
        ResourceSkill selected = skillTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a skill to edit.");
            return;
        }
        
        Dialog<ResourceSkill> dialog = new Dialog<>();
        dialog.setTitle("Edit Skill for " + selectedResource.getName());
        dialog.setHeaderText("Update skill details");
        dialog.initOwner(stage);
        
        // Form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Get the current skill
        Skill currentSkill = skillRepo.findById(selected.getSkillId()).orElse(null);
        
        ComboBox<Skill> skillCombo = new ComboBox<>();
        List<Skill> availableSkills = skillRepo.findActive();
        skillCombo.getItems().addAll(availableSkills);
        skillCombo.setValue(currentSkill);
        skillCombo.setPrefWidth(300);
        skillCombo.setConverter(new javafx.util.StringConverter<Skill>() {
            @Override
            public String toString(Skill skill) {
                return skill != null ? skill.getName() : "";
            }
            @Override
            public Skill fromString(String string) {
                return skillCombo.getItems().stream()
                    .filter(s -> s.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        ComboBox<Integer> proficiencyCombo = new ComboBox<>();
        proficiencyCombo.getItems().addAll(1, 2, 3, 4, 5);
        proficiencyCombo.setValue(selected.getProficiencyLevel());
        
        Label proficiencyLabel = new Label(getProficiencyText(selected.getProficiencyLevel()));
        proficiencyCombo.setOnAction(e -> {
            Integer val = proficiencyCombo.getValue();
            if (val != null) {
                proficiencyLabel.setText(getProficiencyText(val));
            }
        });
        
        TextField yearsExpField = new TextField();
        if (selected.getYearsOfExperience() != null) {
            yearsExpField.setText(selected.getYearsOfExperience().toString());
        }
        yearsExpField.setPromptText("Years of experience (optional)");
        
        TextArea notesArea = new TextArea(selected.getNotes() != null ? selected.getNotes() : "");
        notesArea.setPromptText("Notes (optional)");
        notesArea.setPrefRowCount(3);
        
        grid.add(new Label("Skill:*"), 0, 0);
        grid.add(skillCombo, 1, 0);
        grid.add(new Label("Proficiency:*"), 0, 1);
        grid.add(proficiencyCombo, 1, 1);
        grid.add(proficiencyLabel, 2, 1);
        grid.add(new Label("Years Experience:"), 0, 2);
        grid.add(yearsExpField, 1, 2);
        grid.add(new Label("Notes:"), 0, 3);
        grid.add(notesArea, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && skillCombo.getValue() != null) {
                selected.setSkillId(skillCombo.getValue().getId());
                selected.setProficiencyLevel(proficiencyCombo.getValue());
                
                // Parse years experience
                String yearsText = yearsExpField.getText().trim();
                if (!yearsText.isEmpty()) {
                    try {
                        selected.setYearsOfExperience(Integer.parseInt(yearsText));
                    } catch (NumberFormatException ex) {
                        selected.setYearsOfExperience(null);
                    }
                } else {
                    selected.setYearsOfExperience(null);
                }
                
                selected.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                return selected;
            }
            return null;
        });
        
        Optional<ResourceSkill> result = dialog.showAndWait();
        result.ifPresent(rs -> {
            try {
                resourceSkillRepo.save(rs);
                loadResourceSkills();
                showInfo("Skill updated successfully.");
            } catch (Exception e) {
                logger.error("Failed to update skill", e);
                showError("Failed to update skill: " + e.getMessage());
            }
        });
    }
    
    private void removeResourceCertification() {
        ResourceCertification selected = certificationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a certification to remove.");
            return;
        }
        
        Alert confirm = DialogUtils.createScreenAwareAlert(Alert.AlertType.CONFIRMATION, stage);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Certification");
        confirm.setContentText("Are you sure you want to remove this certification from the resource?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                resourceCertRepo.delete(selected.getId());
                loadResourceCertifications();
                showInfo("Certification removed successfully.");
            } catch (Exception e) {
                logger.error("Failed to remove certification", e);
                showError("Failed to remove certification: " + e.getMessage());
            }
        }
    }
    
    private void removeResourceSkill() {
        ResourceSkill selected = skillTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a skill to remove.");
            return;
        }
        
        Alert confirm = DialogUtils.createScreenAwareAlert(Alert.AlertType.CONFIRMATION, stage);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove Skill");
        confirm.setContentText("Are you sure you want to remove this skill from the resource?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                resourceSkillRepo.delete(selected.getId());
                loadResourceSkills();
                showInfo("Skill removed successfully.");
            } catch (Exception e) {
                logger.error("Failed to remove skill", e);
                showError("Failed to remove skill: " + e.getMessage());
            }
        }
    }
    
    private String getProficiencyText(Integer level) {
        if (level == null) return "";
        switch (level) {
            case 1: return "1 - Beginner";
            case 2: return "2 - Basic";
            case 3: return "3 - Intermediate";
            case 4: return "4 - Advanced";
            case 5: return "5 - Expert";
            default: return level.toString();
        }
    }
    
    private void showInfo(String message) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.INFORMATION, stage);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.WARNING, stage);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.ERROR, stage);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void show() {
        stage.show();
    }
}