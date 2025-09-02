package com.subliminalsearch.simpleprojectresourcemanager.component;

import javafx.animation.*;
import javafx.animation.Interpolator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.function.Consumer;

public class ToastNotification {
    
    private static final Duration FADE_IN_DURATION = Duration.millis(300);
    private static final Duration FADE_OUT_DURATION = Duration.millis(300);
    private static final Duration DISPLAY_DURATION = Duration.seconds(10);
    
    private final Popup popup;
    private final Label messageLabel;
    private final Label detailsLabel;
    private final Button undoButton;
    private final Label countdownLabel;
    private final StackPane progressContainer;
    private final Rectangle progressBar;
    private Timeline hideTimeline;
    private Timeline countdownTimeline;
    private Timeline progressAnimation;
    
    public ToastNotification() {
        popup = new Popup();
        popup.setAutoHide(false);
        
        // Create the toast container
        VBox container = new VBox(5);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(15, 20, 15, 20));
        container.setMinWidth(400);
        container.setMaxWidth(500);
        container.setStyle(
            "-fx-background-color: #323232;" +
            "-fx-background-radius: 5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 2);"
        );
        
        // Message and details
        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        detailsLabel = new Label();
        detailsLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        detailsLabel.setWrapText(true);
        
        // Custom progress bar for countdown
        progressContainer = new StackPane();
        progressContainer.setPrefHeight(6);
        progressContainer.setMaxWidth(Double.MAX_VALUE);
        progressContainer.setStyle(
            "-fx-background-color: #555555;" +
            "-fx-background-radius: 3;"
        );
        
        progressBar = new Rectangle();
        progressBar.setHeight(6);
        progressBar.setFill(Color.web("#4CAF50"));
        progressBar.setArcWidth(6);
        progressBar.setArcHeight(6);
        
        progressContainer.getChildren().add(progressBar);
        progressContainer.setAlignment(Pos.CENTER_LEFT);
        
        // Bottom row with undo button and countdown
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        
        undoButton = new Button("UNDO");
        String undoButtonBaseStyle = 
            "-fx-background-color: #4CAF50;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 5 15 5 15;" +
            "-fx-background-radius: 3;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 3, 0, 0, 1);";
        undoButton.setStyle(undoButtonBaseStyle);
        
        undoButton.setOnMouseEntered(e -> 
            undoButton.setStyle(
                "-fx-background-color: #5CBF60;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 5 15 5 15;" +
                "-fx-background-radius: 3;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
            )
        );
        undoButton.setOnMouseExited(e -> 
            undoButton.setStyle(undoButtonBaseStyle)
        );
        
        countdownLabel = new Label();
        countdownLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        bottomRow.getChildren().addAll(undoButton, spacer, countdownLabel);
        
        container.getChildren().addAll(messageLabel, detailsLabel, progressContainer, bottomRow);
        popup.getContent().add(container);
    }
    
    /**
     * Show a delete notification with undo option
     */
    public void showDeleteNotification(Window owner, String itemType, String itemName, 
                                      String details, Consumer<Void> onUndo) {
        messageLabel.setText(itemType + " deleted");
        detailsLabel.setText(itemName + (details != null ? "\n" + details : ""));
        
        // Position at top-center of window
        popup.show(owner);
        centerOnWindow(owner);
        
        // Setup undo action
        undoButton.setOnAction(e -> {
            if (onUndo != null) {
                onUndo.accept(null);
            }
            hide();
        });
        
        // Start countdown
        startCountdown();
        
        // Fade in
        FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, container(popup));
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
    
    /**
     * Show a simple info notification (no undo)
     */
    public void showInfo(Window owner, String message, String details) {
        messageLabel.setText(message);
        detailsLabel.setText(details != null ? details : "");
        undoButton.setVisible(false);
        undoButton.setManaged(false);
        
        popup.show(owner);
        centerOnWindow(owner);
        
        // Auto-hide after 3 seconds for info messages
        hideTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> hide()));
        hideTimeline.play();
        
        FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, container(popup));
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
    
    /**
     * Show an error notification
     */
    public void showError(Window owner, String message, String details) {
        messageLabel.setText("Error: " + message);
        messageLabel.setStyle(messageLabel.getStyle() + "-fx-text-fill: #f44336;");
        detailsLabel.setText(details != null ? details : "");
        undoButton.setVisible(false);
        undoButton.setManaged(false);
        progressContainer.setVisible(false);
        progressContainer.setManaged(false);
        
        popup.show(owner);
        centerOnWindow(owner);
        
        // Keep error visible longer
        hideTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> hide()));
        hideTimeline.play();
        
        FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, container(popup));
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
    
    private void startCountdown() {
        final int totalSeconds = 10;
        final long startTime = System.currentTimeMillis();
        
        // Get the container width for progress bar animation
        final double containerWidth = 400; // Default width
        
        // Initial progress bar width
        progressBar.setWidth(containerWidth);
        
        // Animate the progress bar width from full to 0
        progressAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(progressBar.widthProperty(), containerWidth)),
            new KeyFrame(DISPLAY_DURATION, 
                new KeyValue(progressBar.widthProperty(), 0, Interpolator.LINEAR))
        );
        progressAnimation.play();
        
        // Update countdown label and color
        countdownTimeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double remaining = Math.max(0, totalSeconds - (elapsed / 1000.0));
            
            if (remaining <= 0) {
                hide();
            } else {
                countdownLabel.setText(String.format("%.1fs", remaining));
                
                // Smooth color transition as time runs out
                if (remaining < 2) {
                    // Pulse effect when almost out of time
                    double pulse = Math.sin(elapsed / 100.0) * 0.5 + 0.5;
                    progressBar.setFill(Color.web("#f44336")); // Red
                    progressBar.setOpacity(0.7 + pulse * 0.3);
                } else if (remaining < 3) {
                    progressBar.setFill(Color.web("#f44336")); // Red
                    progressBar.setOpacity(1.0);
                } else if (remaining < 5) {
                    progressBar.setFill(Color.web("#FF9800")); // Orange
                    progressBar.setOpacity(1.0);
                } else {
                    progressBar.setFill(Color.web("#4CAF50")); // Green
                    progressBar.setOpacity(1.0);
                }
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        
        // Final hide after exact duration
        hideTimeline = new Timeline(new KeyFrame(DISPLAY_DURATION, e -> hide()));
        hideTimeline.play();
    }
    
    private void hide() {
        if (hideTimeline != null) {
            hideTimeline.stop();
        }
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (progressAnimation != null) {
            progressAnimation.stop();
        }
        
        FadeTransition fadeOut = new FadeTransition(FADE_OUT_DURATION, container(popup));
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());
        fadeOut.play();
    }
    
    private void centerOnWindow(Window owner) {
        // Position at top-center of the window
        double x = owner.getX() + (owner.getWidth() - popup.getWidth()) / 2;
        double y = owner.getY() + 50; // 50 pixels from top
        popup.setX(x);
        popup.setY(y);
    }
    
    private VBox container(Popup popup) {
        return (VBox) popup.getContent().get(0);
    }
    
    // Singleton instance management
    private static ToastNotification instance;
    
    public static ToastNotification getInstance() {
        if (instance == null) {
            instance = new ToastNotification();
        }
        return instance;
    }
}