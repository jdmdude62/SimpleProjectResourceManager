package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ResourceSkillFilterDialog {
    private static final Logger logger = LoggerFactory.getLogger(ResourceSkillFilterDialog.class);
    
    private final Stage stage;
    private final List<Resource> allResources;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceCertificationRepository resourceCertificationRepository;
    
    private TableView<ResourceMatch> matchTable;
    private ObservableList<ResourceMatch> resourceMatches;
    
    private ListView<SkillRequirement> requiredSkillsList;
    private ListView<CertificationRequirement> requiredCertsList;
    private ObservableList<SkillRequirement> requiredSkills;
    private ObservableList<CertificationRequirement> requiredCerts;
    
    private ComboBox<Skill> skillCombo;
    private ComboBox<Certification> certCombo;
    private Spinner<Integer> minSkillLevel;
    private Spinner<Integer> minCertLevel;
    
    private List<Resource> selectedResources = new ArrayList<>();
    
    public ResourceSkillFilterDialog(Window owner, List<Resource> resources) {
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.initOwner(owner);
        this.stage.setTitle("Filter Resources by Skills & Certifications");
        this.allResources = resources;
        
        // Initialize repositories
        DatabaseConfig dbConfig = new DatabaseConfig();
        this.skillRepository = new SkillRepository(dbConfig.getDataSource());
        this.certificationRepository = new CertificationRepository(dbConfig.getDataSource());
        this.resourceSkillRepository = new ResourceSkillRepository(dbConfig.getDataSource());
        this.resourceCertificationRepository = new ResourceCertificationRepository(dbConfig.getDataSource());
        
        initializeUI();
        
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
        Label titleLabel = new Label("Select Resources Based on Required Skills and Certifications");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Main content split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.4);
        
        // Left side - Requirements
        VBox requirementsBox = createRequirementsPane();
        
        // Right side - Matching resources
        VBox resourcesBox = createResourcesPane();
        
        splitPane.getItems().addAll(requirementsBox, resourcesBox);
        
        // Bottom buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));
        
        Button selectButton = new Button("Select Checked Resources");
        selectButton.setOnAction(e -> selectResources());
        
        Button selectAllButton = new Button("Check All Matches");
        selectAllButton.setOnAction(e -> selectAllMatches());
        
        Button clearButton = new Button("Clear Requirements");
        clearButton.setOnAction(e -> clearRequirements());
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> stage.close());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        buttonBar.getChildren().addAll(selectAllButton, clearButton, spacer, selectButton, cancelButton);
        
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        root.getChildren().addAll(titleLabel, splitPane, buttonBar);
        
        stage.setScene(new javafx.scene.Scene(root));
    }
    
    private VBox createRequirementsPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label requirementsLabel = new Label("Requirements");
        requirementsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Skills section
        Label skillsLabel = new Label("Required Skills:");
        skillsLabel.setStyle("-fx-font-weight: bold;");
        
        HBox skillAddBox = new HBox(5);
        skillCombo = new ComboBox<>();
        skillCombo.setPrefWidth(200);
        skillCombo.setPromptText("Select skill...");
        
        minSkillLevel = new Spinner<>(1, 5, 3);
        minSkillLevel.setPrefWidth(80);
        
        Button addSkillBtn = new Button("Add");
        addSkillBtn.setOnAction(e -> addSkillRequirement());
        
        skillAddBox.getChildren().addAll(skillCombo, new Label("Min Level:"), minSkillLevel, addSkillBtn);
        
        requiredSkills = FXCollections.observableArrayList();
        requiredSkillsList = new ListView<>(requiredSkills);
        requiredSkillsList.setPrefHeight(150);
        requiredSkillsList.setCellFactory(lv -> new ListCell<SkillRequirement>() {
            @Override
            protected void updateItem(SkillRequirement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5);
                    Label label = new Label(item.skill.getName() + " (Level " + item.minLevel + "+)");
                    Button removeBtn = new Button("Remove");
                    removeBtn.setOnAction(e -> {
                        requiredSkills.remove(item);
                        updateMatches();
                    });
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    box.getChildren().addAll(label, spacer, removeBtn);
                    setGraphic(box);
                }
            }
        });
        
        // Certifications section
        Label certsLabel = new Label("Required Certifications:");
        certsLabel.setStyle("-fx-font-weight: bold;");
        
        HBox certAddBox = new HBox(5);
        certCombo = new ComboBox<>();
        certCombo.setPrefWidth(200);
        certCombo.setPromptText("Select certification...");
        
        minCertLevel = new Spinner<>(1, 5, 3);
        minCertLevel.setPrefWidth(80);
        
        Button addCertBtn = new Button("Add");
        addCertBtn.setOnAction(e -> addCertRequirement());
        
        certAddBox.getChildren().addAll(certCombo, new Label("Min Score:"), minCertLevel, addCertBtn);
        
        requiredCerts = FXCollections.observableArrayList();
        requiredCertsList = new ListView<>(requiredCerts);
        requiredCertsList.setPrefHeight(150);
        requiredCertsList.setCellFactory(lv -> new ListCell<CertificationRequirement>() {
            @Override
            protected void updateItem(CertificationRequirement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5);
                    Label label = new Label(item.certification.getName() + " (Score " + item.minScore + "+)");
                    Button removeBtn = new Button("Remove");
                    removeBtn.setOnAction(e -> {
                        requiredCerts.remove(item);
                        updateMatches();
                    });
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    box.getChildren().addAll(label, spacer, removeBtn);
                    setGraphic(box);
                }
            }
        });
        
        // Load skills and certifications
        loadSkillsAndCertifications();
        
        VBox.setVgrow(requiredSkillsList, Priority.ALWAYS);
        VBox.setVgrow(requiredCertsList, Priority.ALWAYS);
        
        box.getChildren().addAll(
            requirementsLabel,
            new Separator(),
            skillsLabel, skillAddBox, requiredSkillsList,
            new Separator(),
            certsLabel, certAddBox, requiredCertsList
        );
        
        return box;
    }
    
    private VBox createResourcesPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label matchLabel = new Label("Matching Resources");
        matchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Resource matches table
        matchTable = new TableView<>();
        resourceMatches = FXCollections.observableArrayList();
        matchTable.setItems(resourceMatches);
        
        // Select column
        TableColumn<ResourceMatch, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setPrefWidth(60);
        selectCol.setEditable(true);
        
        // Resource name column
        TableColumn<ResourceMatch, String> nameCol = new TableColumn<>("Resource");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(150);
        
        // Match percentage column
        TableColumn<ResourceMatch, Integer> matchCol = new TableColumn<>("Match %");
        matchCol.setCellValueFactory(cellData -> cellData.getValue().matchPercentageProperty().asObject());
        matchCol.setPrefWidth(80);
        matchCol.setCellFactory(column -> new TableCell<ResourceMatch, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item + "%");
                    if (item >= 100) {
                        setStyle("-fx-background-color: #c8e6c9; -fx-font-weight: bold;");
                    } else if (item >= 75) {
                        setStyle("-fx-background-color: #fff9c4;");
                    } else if (item >= 50) {
                        setStyle("-fx-background-color: #ffe0b2;");
                    } else {
                        setStyle("-fx-background-color: #ffccbc;");
                    }
                }
            }
        });
        
        // Skills matched column
        TableColumn<ResourceMatch, String> skillsCol = new TableColumn<>("Skills Matched");
        skillsCol.setCellValueFactory(cellData -> cellData.getValue().skillsMatchedProperty());
        skillsCol.setPrefWidth(200);
        
        // Certifications matched column
        TableColumn<ResourceMatch, String> certsCol = new TableColumn<>("Certifications Matched");
        certsCol.setCellValueFactory(cellData -> cellData.getValue().certsMatchedProperty());
        certsCol.setPrefWidth(200);
        
        matchTable.getColumns().addAll(selectCol, nameCol, matchCol, skillsCol, certsCol);
        matchTable.setEditable(true);
        
        // Initially show all resources
        updateMatches();
        
        VBox.setVgrow(matchTable, Priority.ALWAYS);
        box.getChildren().addAll(matchLabel, new Separator(), matchTable);
        
        return box;
    }
    
    private void loadSkillsAndCertifications() {
        try {
            List<Skill> skills = skillRepository.findAll();
            skillCombo.setItems(FXCollections.observableArrayList(skills));
            skillCombo.setConverter(new StringConverter<Skill>() {
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
            
            List<Certification> certs = certificationRepository.findAll();
            certCombo.setItems(FXCollections.observableArrayList(certs));
            certCombo.setConverter(new StringConverter<Certification>() {
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
        } catch (Exception e) {
            logger.error("Failed to load skills and certifications", e);
        }
    }
    
    private void addSkillRequirement() {
        Skill skill = skillCombo.getValue();
        if (skill != null) {
            // Check if already added
            boolean exists = requiredSkills.stream()
                .anyMatch(req -> req.skill.getId().equals(skill.getId()));
            if (!exists) {
                requiredSkills.add(new SkillRequirement(skill, minSkillLevel.getValue()));
                updateMatches();
                skillCombo.setValue(null);
            }
        }
    }
    
    private void addCertRequirement() {
        Certification cert = certCombo.getValue();
        if (cert != null) {
            // Check if already added
            boolean exists = requiredCerts.stream()
                .anyMatch(req -> req.certification.getId().equals(cert.getId()));
            if (!exists) {
                requiredCerts.add(new CertificationRequirement(cert, minCertLevel.getValue()));
                updateMatches();
                certCombo.setValue(null);
            }
        }
    }
    
    private void updateMatches() {
        resourceMatches.clear();
        
        for (Resource resource : allResources) {
            ResourceMatch match = calculateMatch(resource);
            resourceMatches.add(match);
        }
        
        // Sort by match percentage descending
        resourceMatches.sort((a, b) -> Integer.compare(b.matchPercentage.get(), a.matchPercentage.get()));
    }
    
    private ResourceMatch calculateMatch(Resource resource) {
        ResourceMatch match = new ResourceMatch(resource);
        
        if (requiredSkills.isEmpty() && requiredCerts.isEmpty()) {
            match.matchPercentage.set(100);
            return match;
        }
        
        int totalRequirements = requiredSkills.size() + requiredCerts.size();
        int metRequirements = 0;
        List<String> matchedSkills = new ArrayList<>();
        List<String> matchedCerts = new ArrayList<>();
        
        // Check skills
        List<ResourceSkill> resourceSkills = resourceSkillRepository.findByResourceId(resource.getId());
        for (SkillRequirement req : requiredSkills) {
            ResourceSkill resourceSkill = resourceSkills.stream()
                .filter(rs -> rs.getSkillId().equals(req.skill.getId()))
                .findFirst()
                .orElse(null);
            
            if (resourceSkill != null && 
                resourceSkill.getProficiencyLevel() != null && 
                resourceSkill.getProficiencyLevel() >= req.minLevel) {
                metRequirements++;
                matchedSkills.add(req.skill.getName() + " (L" + resourceSkill.getProficiencyLevel() + ")");
            }
        }
        
        // Check certifications
        List<ResourceCertification> resourceCerts = resourceCertificationRepository.findByResourceId(resource.getId());
        for (CertificationRequirement req : requiredCerts) {
            ResourceCertification resourceCert = resourceCerts.stream()
                .filter(rc -> rc.getCertificationId().equals(req.certification.getId()))
                .findFirst()
                .orElse(null);
            
            if (resourceCert != null && 
                resourceCert.getProficiencyScore() != null && 
                resourceCert.getProficiencyScore() >= req.minScore) {
                metRequirements++;
                matchedCerts.add(req.certification.getName() + " (S" + resourceCert.getProficiencyScore() + ")");
            }
        }
        
        int percentage = (metRequirements * 100) / totalRequirements;
        match.matchPercentage.set(percentage);
        match.skillsMatched.set(String.join(", ", matchedSkills));
        match.certsMatched.set(String.join(", ", matchedCerts));
        
        // Auto-select if 100% match
        if (percentage == 100) {
            match.selected.set(true);
        }
        
        return match;
    }
    
    private void selectAllMatches() {
        for (ResourceMatch match : resourceMatches) {
            if (match.matchPercentage.get() > 0) {
                match.selected.set(true);
            }
        }
    }
    
    private void clearRequirements() {
        requiredSkills.clear();
        requiredCerts.clear();
        updateMatches();
    }
    
    private void selectResources() {
        selectedResources = resourceMatches.stream()
            .filter(rm -> rm.selected.get())
            .map(rm -> rm.resource)
            .collect(Collectors.toList());
        stage.close();
    }
    
    public List<Resource> showAndWait() {
        stage.showAndWait();
        return selectedResources;
    }
    
    // Helper classes
    private static class SkillRequirement {
        final Skill skill;
        final int minLevel;
        
        SkillRequirement(Skill skill, int minLevel) {
            this.skill = skill;
            this.minLevel = minLevel;
        }
    }
    
    private static class CertificationRequirement {
        final Certification certification;
        final int minScore;
        
        CertificationRequirement(Certification certification, int minScore) {
            this.certification = certification;
            this.minScore = minScore;
        }
    }
    
    private static class ResourceMatch {
        final Resource resource;
        final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
        final SimpleStringProperty name;
        final SimpleIntegerProperty matchPercentage = new SimpleIntegerProperty(0);
        final SimpleStringProperty skillsMatched = new SimpleStringProperty("");
        final SimpleStringProperty certsMatched = new SimpleStringProperty("");
        
        ResourceMatch(Resource resource) {
            this.resource = resource;
            this.name = new SimpleStringProperty(resource.getName());
        }
        
        public SimpleBooleanProperty selectedProperty() { return selected; }
        public SimpleStringProperty nameProperty() { return name; }
        public SimpleIntegerProperty matchPercentageProperty() { return matchPercentage; }
        public SimpleStringProperty skillsMatchedProperty() { return skillsMatched; }
        public SimpleStringProperty certsMatchedProperty() { return certsMatched; }
    }
}