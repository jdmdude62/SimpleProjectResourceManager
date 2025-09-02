package com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Test mode for SharePoint sync - writes to local file instead of sending to users
 */
public class SharePointTestMode {
    private static final Logger logger = Logger.getLogger(SharePointTestMode.class.getName());
    private static final String TEST_OUTPUT_DIR = "sharepoint-test-output";
    private static final String CONFIG_FILE = "sharepoint-test-mode.properties";
    private static boolean testMode = true; // Default to test mode
    
    static {
        // Load test mode setting from file on startup
        loadTestMode();
    }
    
    private static void loadTestMode() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                Properties props = new Properties();
                try (FileReader reader = new FileReader(configFile)) {
                    props.load(reader);
                    testMode = Boolean.parseBoolean(props.getProperty("testMode", "true"));
                    logger.info("Loaded SharePoint test mode from file: " + testMode);
                }
            } else {
                logger.info("No test mode config file found, defaulting to test mode: true");
            }
        } catch (Exception e) {
            logger.warning("Could not load test mode setting, defaulting to true: " + e.getMessage());
            testMode = true;
        }
    }
    
    public static boolean isTestMode() {
        // Reload from file to ensure we have the latest setting
        loadTestMode();
        return testMode;
    }
    
    public static void setTestMode(boolean enabled) {
        testMode = enabled;
        logger.info("SharePoint sync test mode: " + (enabled ? "ENABLED" : "DISABLED"));
        
        // Save to file for persistence
        try {
            Properties props = new Properties();
            props.setProperty("testMode", String.valueOf(enabled));
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                props.store(writer, "SharePoint Test Mode Configuration");
            }
            logger.info("Saved test mode setting to file: " + enabled);
        } catch (Exception e) {
            logger.warning("Could not save test mode setting: " + e.getMessage());
        }
    }
    
    /**
     * Log a test event instead of creating real calendar event
     */
    public static void logTestEvent(String userName, String userEmail, String projectId, 
                                   String projectName, String startDate, String endDate, 
                                   String location) {
        if (!testMode) return;
        
        try {
            // Create test output directory if it doesn't exist
            File dir = new File(TEST_OUTPUT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = String.format("%s/sync-test-%s.csv", TEST_OUTPUT_DIR, timestamp);
            
            // Check if file exists, create with headers if not
            File file = new File(filename);
            boolean isNew = !file.exists();
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                if (isNew) {
                    writer.println("Timestamp,UserName,UserEmail,ProjectID,ProjectName,StartDate,EndDate,Location");
                }
                
                // Write the test event
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    LocalDateTime.now(),
                    userName,
                    userEmail != null ? userEmail : "NO_EMAIL",
                    projectId,
                    projectName,
                    startDate,
                    endDate,
                    location != null ? location : "TBD"
                );
            }
            
            logger.info(String.format("TEST MODE: Would sync - User: %s, Project: %s (%s), Dates: %s to %s",
                userName, projectName, projectId, startDate, endDate));
            
        } catch (Exception e) {
            logger.severe("Failed to write test event: " + e.getMessage());
        }
    }
    
    /**
     * Get summary of test events
     */
    public static String getTestSummary() {
        File dir = new File(TEST_OUTPUT_DIR);
        if (!dir.exists()) {
            return "No test output found";
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".csv"));
        if (files == null || files.length == 0) {
            return "No test files found";
        }
        
        // Get most recent file
        File latest = files[files.length - 1];
        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(latest))) {
            while (reader.readLine() != null) lineCount++;
        } catch (Exception e) {
            return "Error reading test file: " + e.getMessage();
        }
        
        return String.format("Test mode active. Latest sync: %s with %d events logged to %s", 
            latest.getName(), lineCount - 1, TEST_OUTPUT_DIR);
    }
}