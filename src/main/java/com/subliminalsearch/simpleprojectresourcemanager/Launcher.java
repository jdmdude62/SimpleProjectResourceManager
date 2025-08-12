package com.subliminalsearch.simpleprojectresourcemanager;

/**
 * Standalone launcher for the Simple Project Resource Manager.
 * This helps avoid JavaFX module issues when launching via Maven.
 */
public class Launcher {
    public static void main(String[] args) {
        // Launch the JavaFX application
        SchedulerApplication.main(args);
    }
}