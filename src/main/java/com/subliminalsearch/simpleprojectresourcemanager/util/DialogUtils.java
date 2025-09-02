package com.subliminalsearch.simpleprojectresourcemanager.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/**
 * Utility class for managing dialog positioning across multiple displays.
 * Ensures dialogs open on the same screen as their parent window.
 */
public class DialogUtils {
    
    /**
     * Initializes a dialog with owner and positions it on the same screen.
     * This is the recommended method for all Dialog instances.
     * 
     * @param dialog The dialog to initialize
     * @param owner The owner window (can be null)
     */
    public static void initializeDialog(Dialog<?> dialog, Window owner) {
        if (owner != null) {
            dialog.initOwner(owner);
            centerOnOwnerScreen(dialog, owner);
        }
    }
    
    /**
     * Centers a dialog on the same screen as its owner window.
     * If the owner has been moved to a different display, the dialog follows.
     * 
     * @param dialog The dialog to position
     * @param owner The owner window (usually the main application window)
     */
    public static void centerOnOwnerScreen(Dialog<?> dialog, Window owner) {
        if (owner == null) {
            return;
        }
        
        // Get the screen where the owner window is currently displayed
        Screen ownerScreen = getScreenForWindow(owner);
        if (ownerScreen == null) {
            ownerScreen = Screen.getPrimary();
        }
        
        // Get screen bounds
        Rectangle2D screenBounds = ownerScreen.getVisualBounds();
        
        // Calculate center position
        double centerX = screenBounds.getMinX() + (screenBounds.getWidth() / 2);
        double centerY = screenBounds.getMinY() + (screenBounds.getHeight() / 2);
        
        // Position dialog at center of the screen
        dialog.setOnShown(event -> {
            dialog.setX(centerX - (dialog.getWidth() / 2));
            dialog.setY(centerY - (dialog.getHeight() / 2));
        });
    }
    
    /**
     * Centers an Alert on the same screen as its owner window.
     * 
     * @param alert The alert to position
     * @param owner The owner window
     */
    public static void centerOnOwnerScreen(Alert alert, Window owner) {
        if (owner == null) {
            return;
        }
        
        Screen ownerScreen = getScreenForWindow(owner);
        if (ownerScreen == null) {
            ownerScreen = Screen.getPrimary();
        }
        
        Rectangle2D screenBounds = ownerScreen.getVisualBounds();
        double centerX = screenBounds.getMinX() + (screenBounds.getWidth() / 2);
        double centerY = screenBounds.getMinY() + (screenBounds.getHeight() / 2);
        
        alert.setOnShown(event -> {
            alert.setX(centerX - (alert.getWidth() / 2));
            alert.setY(centerY - (alert.getHeight() / 2));
        });
    }
    
    /**
     * Positions a stage on the same screen as its owner window.
     * 
     * @param stage The stage to position
     * @param owner The owner window
     * @param widthRatio Width as a ratio of screen width (0.0 to 1.0)
     * @param heightRatio Height as a ratio of screen height (0.0 to 1.0)
     */
    public static void positionStageOnOwnerScreen(Stage stage, Window owner, double widthRatio, double heightRatio) {
        if (owner == null) {
            return;
        }
        
        Screen ownerScreen = getScreenForWindow(owner);
        if (ownerScreen == null) {
            ownerScreen = Screen.getPrimary();
        }
        
        Rectangle2D screenBounds = ownerScreen.getVisualBounds();
        
        // Calculate dimensions based on screen size
        double width = screenBounds.getWidth() * widthRatio;
        double height = screenBounds.getHeight() * heightRatio;
        
        // Calculate center position
        double centerX = screenBounds.getMinX() + (screenBounds.getWidth() / 2);
        double centerY = screenBounds.getMinY() + (screenBounds.getHeight() / 2);
        
        // Set size
        stage.setWidth(width);
        stage.setHeight(height);
        
        // Set position after the stage is shown to ensure proper positioning
        if (stage.isShowing()) {
            stage.setX(centerX - (width / 2));
            stage.setY(centerY - (height / 2));
        } else {
            stage.setOnShown(event -> {
                stage.setX(centerX - (width / 2));
                stage.setY(centerY - (height / 2));
            });
        }
    }
    
    /**
     * Ensures a dialog stays within screen bounds after being shown.
     * Useful for dialogs that might be partially off-screen.
     * 
     * @param dialog The dialog to constrain
     */
    public static void constrainToScreen(Dialog<?> dialog) {
        dialog.setOnShown(event -> {
            Screen screen = getScreenForWindow(dialog.getOwner());
            if (screen == null) {
                screen = Screen.getPrimary();
            }
            
            Rectangle2D bounds = screen.getVisualBounds();
            
            // Check if dialog is outside screen bounds and adjust
            double x = dialog.getX();
            double y = dialog.getY();
            double width = dialog.getWidth();
            double height = dialog.getHeight();
            
            // Adjust X position
            if (x < bounds.getMinX()) {
                dialog.setX(bounds.getMinX());
            } else if (x + width > bounds.getMaxX()) {
                dialog.setX(bounds.getMaxX() - width);
            }
            
            // Adjust Y position
            if (y < bounds.getMinY()) {
                dialog.setY(bounds.getMinY());
            } else if (y + height > bounds.getMaxY()) {
                dialog.setY(bounds.getMaxY() - height);
            }
        });
    }
    
    /**
     * Gets the screen that contains the majority of the given window.
     * 
     * @param window The window to check
     * @return The screen containing the window, or null if not found
     */
    private static Screen getScreenForWindow(Window window) {
        List<Screen> screens = Screen.getScreensForRectangle(
            window.getX(), 
            window.getY(), 
            window.getWidth(), 
            window.getHeight()
        );
        
        if (!screens.isEmpty()) {
            // Return the screen that contains the most area of the window
            return screens.get(0);
        }
        
        // Fallback: check which screen contains the window's center point
        double centerX = window.getX() + (window.getWidth() / 2);
        double centerY = window.getY() + (window.getHeight() / 2);
        
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            if (bounds.contains(centerX, centerY)) {
                return screen;
            }
        }
        
        return null;
    }
    
    /**
     * Creates a properly positioned dialog with owner screen awareness.
     * 
     * @param owner The owner window
     * @return A new dialog positioned on the owner's screen
     */
    public static <T> Dialog<T> createScreenAwareDialog(Window owner) {
        Dialog<T> dialog = new Dialog<>();
        dialog.initOwner(owner);
        centerOnOwnerScreen(dialog, owner);
        return dialog;
    }
    
    /**
     * Creates a properly positioned alert with owner screen awareness.
     * 
     * @param alertType The type of alert
     * @param owner The owner window
     * @return A new alert positioned on the owner's screen
     */
    public static Alert createScreenAwareAlert(Alert.AlertType alertType, Window owner) {
        Alert alert = new Alert(alertType);
        alert.initOwner(owner);
        centerOnOwnerScreen(alert, owner);
        return alert;
    }
}