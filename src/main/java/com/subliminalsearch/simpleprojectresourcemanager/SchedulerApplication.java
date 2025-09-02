package com.subliminalsearch.simpleprojectresourcemanager;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.controller.MainController;
import com.subliminalsearch.simpleprojectresourcemanager.data.SampleDataGenerator;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectManagerRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SchedulerApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerApplication.class);
    
    private DatabaseConfig databaseConfig;
    private SchedulingService schedulingService;
    private MainController mainController;

    @Override
    public void init() throws Exception {
        super.init();
        
        // Initialize database and services
        logger.info("Initializing Simple Project Resource Manager...");
        
        databaseConfig = new DatabaseConfig();
        
        // Initialize repositories
        ProjectRepository projectRepository = new ProjectRepository(databaseConfig.getDataSource());
        ResourceRepository resourceRepository = new ResourceRepository(databaseConfig.getDataSource());
        AssignmentRepository assignmentRepository = new AssignmentRepository(databaseConfig.getDataSource());
        ProjectManagerRepository projectManagerRepository = new ProjectManagerRepository(databaseConfig.getDataSource());
        
        // Initialize service
        schedulingService = new SchedulingService(projectRepository, resourceRepository, assignmentRepository, projectManagerRepository, databaseConfig.getDataSource());
        
        // Generate sample data if database is empty
        // DISABLED: Auto-generation for template creation
        // if (schedulingService.getProjectCount() == 0) {
        //     SampleDataGenerator.generateSampleData(schedulingService);
        // }
        
        logger.info("Application initialization complete");
    }

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting JavaFX application...");
        
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        
        // Create and inject the main controller with shared database config
        mainController = new MainController(schedulingService, databaseConfig);
        fxmlLoader.setController(mainController);
        
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        
        // Add BootstrapFX styling
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        
        // Add custom CSS
        scene.getStylesheets().add(getClass().getResource("/css/scheduler.css").toExternalForm());
        
        stage.setTitle("Simple Project Resource Manager");
        stage.setScene(scene);
        stage.setMaximized(true);
        
        // Set minimum window size
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        
        // Handle window close properly - ensure clean shutdown only on user request
        stage.setOnCloseRequest(event -> {
            logger.info("User requested application closure");
            try {
                if (databaseConfig != null) {
                    databaseConfig.shutdown();
                }
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
            Platform.exit();
            System.exit(0);
        });
        
        // Prevent implicit exit - keep application running
        Platform.setImplicitExit(false);
        
        stage.show();
        
        logger.info("JavaFX application started successfully");
    }

    @Override
    public void stop() throws Exception {
        logger.info("Shutting down application...");
        
        try {
            if (databaseConfig != null) {
                databaseConfig.shutdown();
            }
        } catch (Exception e) {
            logger.error("Error during database shutdown", e);
        }
        
        super.stop();
        logger.info("Application shutdown complete");
    }

    public static void main(String[] args) {
        launch();
    }
    
    // For testing purposes
    public SchedulingService getSchedulingService() {
        return schedulingService;
    }
    
    public MainController getMainController() {
        return mainController;
    }
}