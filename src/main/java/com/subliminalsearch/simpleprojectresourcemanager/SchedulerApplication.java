package com.subliminalsearch.simpleprojectresourcemanager;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.controller.MainController;
import com.subliminalsearch.simpleprojectresourcemanager.data.SampleDataGenerator;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectManagerRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.service.SessionManager;
import com.subliminalsearch.simpleprojectresourcemanager.view.LoginView;
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
    private SessionManager sessionManager;
    private Stage primaryStage;
    private boolean useAuthentication = true; // Set to false to bypass login during development

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
        this.primaryStage = stage;
        
        // Initialize session manager
        sessionManager = SessionManager.getInstance();
        sessionManager.setOnSessionExpired(() -> {
            Platform.runLater(() -> {
                primaryStage.close();
                showLoginScreen();
            });
        });
        
        // Check if we should use authentication
        String authMode = System.getProperty("auth.mode", "ldap");
        useAuthentication = !"bypass".equalsIgnoreCase(authMode);
        
        if (useAuthentication) {
            // Show login screen first
            showLoginScreen();
        } else {
            // Bypass login for development
            logger.info("Authentication bypassed for development");
            showMainApplication();
        }
    }
    
    private void showLoginScreen() {
        LoginView loginView = new LoginView(new Stage());
        
        // Set callback for successful login
        loginView.setOnSuccessfulLogin(() -> {
            Platform.runLater(() -> {
                try {
                    showMainApplication();
                } catch (IOException e) {
                    logger.error("Error loading main application", e);
                }
            });
        });
        
        // Enable test mode if in development
        if (System.getProperty("dev.mode", "false").equals("true")) {
            loginView.enableTestMode();
        }
        
        loginView.show();
    }
    
    private void showMainApplication() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        
        // Create and inject the main controller
        mainController = new MainController(schedulingService);
        fxmlLoader.setController(mainController);
        
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        
        // Add BootstrapFX styling
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        
        // Add custom CSS
        scene.getStylesheets().add(getClass().getResource("/css/scheduler.css").toExternalForm());
        
        // Update title with user info if authenticated
        String title = "Simple Project Resource Manager";
        if (sessionManager.getCurrentUser() != null) {
            title += " - " + sessionManager.getCurrentUser().getFullName() + 
                    " (" + sessionManager.getCurrentUserRole() + ")";
        }
        primaryStage.setTitle(title);
        
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        
        // Set minimum window size
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // Track user activity for session management
        scene.setOnMouseMoved(e -> sessionManager.updateActivity());
        scene.setOnKeyPressed(e -> sessionManager.updateActivity());
        
        // Handle window close properly - ensure clean shutdown only on user request
        primaryStage.setOnCloseRequest(event -> {
            logger.info("User requested application closure");
            
            // End session
            if (sessionManager != null) {
                sessionManager.endSession();
            }
            
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
        
        primaryStage.show();
        
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