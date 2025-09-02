package com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;

import javax.sql.DataSource;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simple SharePoint synchronization using Microsoft Graph
 * Creates calendar events for assignments (works without Lists)
 */
public class SimpleSharePointSync {
    private static final Logger logger = Logger.getLogger(SimpleSharePointSync.class.getName());
    
    private final SharePointConfig config;
    private final DataSource dataSource;
    private ProjectRepository projectRepo;
    private ResourceRepository resourceRepo;
    private AssignmentRepository assignmentRepo;
    private ClientSecretCredential credential;
    private String accessToken;
    
    public SimpleSharePointSync(DataSource dataSource) {
        this.config = SharePointConfig.getInstance();
        this.dataSource = dataSource;
        this.projectRepo = new ProjectRepository(dataSource);
        this.resourceRepo = new ResourceRepository(dataSource);
        this.assignmentRepo = new AssignmentRepository(dataSource);
    }
    
    /**
     * Initialize Graph client with credentials
     */
    public boolean initialize() {
        try {
            if (!config.isConfigured()) {
                logger.warning("SharePoint is not configured");
                return false;
            }
            
            // Create credential
            credential = new ClientSecretCredentialBuilder()
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .tenantId(config.getTenantId())
                .build();
            
            // Get access token
            try {
                com.azure.core.credential.AccessToken token = credential.getToken(
                    new com.azure.core.credential.TokenRequestContext()
                        .addScopes("https://graph.microsoft.com/.default")
                ).block();
                
                if (token != null && token.getToken() != null) {
                    accessToken = token.getToken();
                    logger.info("Successfully obtained access token");
                } else {
                    logger.severe("Failed to obtain access token");
                    return false;
                }
            } catch (Exception e) {
                logger.severe("Failed to get access token: " + e.getMessage());
                return false;
            }
                
            logger.info("Graph authentication initialized successfully");
            return true;
            
        } catch (Exception e) {
            logger.severe("Failed to initialize Graph client: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Sync all data to SharePoint
     */
    public void syncAll() {
        System.out.println("=== SharePoint Sync Started (System.out) ===");
        logger.info("=== SharePoint Sync Started ===");
        
        if (!config.isEnabled()) {
            System.out.println("SharePoint sync is disabled in configuration");
            logger.info("SharePoint sync is disabled in configuration");
            return;
        }
        
        System.out.println("SharePoint sync is enabled, initializing...");
        logger.info("SharePoint sync is enabled, initializing...");
        
        if (!initialize()) {
            logger.severe("Cannot sync - credentials not initialized");
            return;
        }
        
        logger.info("Credentials initialized, starting sync process...");
        logger.info("Test mode status: " + SharePointTestMode.isTestMode());
        
        try {
            // For now, let's sync to calendars as a proof of concept
            syncAssignmentsToCalendars();
            
            // Show test mode summary if in test mode
            if (SharePointTestMode.isTestMode()) {
                logger.info("TEST MODE ACTIVE - No events sent to users");
                logger.info(SharePointTestMode.getTestSummary());
            } else {
                logger.info("PRODUCTION MODE - Calendar events should be created");
            }
            
            logger.info("=== SharePoint sync completed successfully ===");
            
        } catch (Exception e) {
            logger.severe("Sync failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sync assignments as calendar events
     */
    private void syncAssignmentsToCalendars() {
        logger.info("Syncing assignments to calendars...");
        
        try {
            java.util.List<Assignment> assignments = assignmentRepo.findAll();
            logger.info("Found " + assignments.size() + " total assignments in database");
            
            int processed = 0;
            int skipped = 0;
            int synced = 0;
            
            for (Assignment assignment : assignments) {
                try {
                    // Get project and resource details
                    Optional<Project> projectOpt = projectRepo.findById(assignment.getProjectId());
                    Optional<Resource> resourceOpt = resourceRepo.findById(assignment.getResourceId());
                    
                    if (projectOpt.isPresent() && resourceOpt.isPresent()) {
                        Project project = projectOpt.get();
                        Resource resource = resourceOpt.get();
                        
                        // Only process Mike Miller's assignments for testing
                        if (resource.getName() != null && resource.getName().contains("Mike Miller")) {
                            logger.info("Processing assignment for Mike Miller - Project: " + project.getProjectId());
                            if (resource.getEmail() != null || resource.getLdapUsername() != null) {
                                createOrUpdateCalendarEvent(assignment, project, resource);
                                synced++;
                            } else {
                                logger.warning("No email or LDAP username for resource: " + resource.getName());
                                skipped++;
                            }
                        }
                        processed++;
                    } else {
                        logger.warning("Missing project or resource for assignment " + assignment.getId());
                        skipped++;
                    }
                } catch (Exception e) {
                    logger.warning("Failed to sync assignment " + assignment.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                    skipped++;
                }
            }
            
            logger.info(String.format("Assignment sync summary: Total=%d, Processed=%d, Synced=%d, Skipped=%d",
                assignments.size(), processed, synced, skipped));
            
        } catch (Exception e) {
            logger.severe("Failed to sync assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create or update a calendar event for an assignment
     */
    private void createOrUpdateCalendarEvent(Assignment assignment, Project project, Resource resource) {
        try {
            String userEmail = resource.getEmail();
            
            // Check if we're in test mode
            if (SharePointTestMode.isTestMode()) {
                // Log to test file instead of creating real event
                SharePointTestMode.logTestEvent(
                    resource.getName(),
                    userEmail,
                    project.getProjectId(),
                    project.getDescription(),
                    assignment.getStartDate().toString(),
                    assignment.getEndDate().toString(),
                    project.getContactAddress()
                );
                return;
            }
            
            // Production mode - create actual event using REST API
            String targetUserEmail = userEmail; // Default to email field
            
            // Check if LDAP username is provided
            if (resource.getLdapUsername() != null && !resource.getLdapUsername().isEmpty()) {
                String ldapUser = resource.getLdapUsername().trim();
                
                if (ldapUser.contains("@")) {
                    // Full email format - use as is
                    targetUserEmail = ldapUser;
                } else if (userEmail != null && userEmail.contains("@")) {
                    // Username only - append domain from email
                    String domain = userEmail.substring(userEmail.indexOf("@"));
                    targetUserEmail = ldapUser + domain;
                    logger.info("Constructed email from LDAP username: " + targetUserEmail);
                } else {
                    // Try to use captechno.com as default domain
                    targetUserEmail = ldapUser + "@captechno.com";
                    logger.info("Using default domain for: " + targetUserEmail);
                }
            }
                
            if (targetUserEmail == null || targetUserEmail.isEmpty() || !targetUserEmail.contains("@")) {
                logger.warning("No valid email address for resource: " + resource.getName() + 
                              " (ldap: " + resource.getLdapUsername() + ", email: " + userEmail + ")");
                return;
            }
            
            logger.info("Creating calendar event for: " + targetUserEmail);
            
            // Create event JSON using Jackson
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode eventJson = mapper.createObjectNode();
            
            eventJson.put("subject", project.getProjectId() + " - " + project.getDescription());
            
            // Set body
            ObjectNode body = mapper.createObjectNode();
            body.put("contentType", "HTML");
            body.put("content", String.format(
                "<div>" +
                "<h3>Project Assignment</h3>" +
                "<p><b>Project:</b> %s</p>" +
                "<p><b>Project ID:</b> %s</p>" +
                "<p><b>Location:</b> %s</p>" +
                "<p><b>Start:</b> %s</p>" +
                "<p><b>End:</b> %s</p>" +
                "<p><b>Status:</b> %s</p>" +
                "</div>",
                project.getDescription(),
                project.getProjectId(),
                project.getContactAddress() != null ? project.getContactAddress() : "TBD",
                assignment.getStartDate(),
                assignment.getEndDate(),
                project.getStatus()
            ));
            eventJson.set("body", body);
            
            // Set dates
            ObjectNode start = mapper.createObjectNode();
            start.put("dateTime", assignment.getStartDate().atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            start.put("timeZone", "UTC");
            eventJson.set("start", start);
            
            ObjectNode end = mapper.createObjectNode();
            end.put("dateTime", assignment.getEndDate().atTime(23, 59)
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            end.put("timeZone", "UTC");
            eventJson.set("end", end);
            
            // Set location
            if (project.getContactAddress() != null) {
                ObjectNode location = mapper.createObjectNode();
                location.put("displayName", project.getContactAddress());
                eventJson.set("location", location);
            }
            
            // Set as all-day event
            eventJson.put("isAllDay", true);
            
            // Set categories
            eventJson.putArray("categories").add("Field Work").add("Project");
            
            // Make the HTTP request to create the event
            String graphUrl = String.format("https://graph.microsoft.com/v1.0/users/%s/calendar/events", targetUserEmail);
            
            try {
                URL url = new URL(graphUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                String jsonBody = mapper.writeValueAsString(eventJson);
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    logger.info("✓ Successfully created calendar event for " + resource.getName() + 
                               " (" + targetUserEmail + ") - Project: " + project.getDescription());
                    
                    // Read success response for event ID
                    try (Scanner scanner = new Scanner(conn.getInputStream())) {
                        String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        if (response.contains("\"id\"")) {
                            logger.info("Event created with response: " + response.substring(0, Math.min(200, response.length())));
                        }
                    }
                } else {
                    logger.severe("✗ Failed to create calendar event for " + targetUserEmail + 
                                 ". Response code: " + responseCode);
                    // Try to read error response
                    try (Scanner scanner = new Scanner(conn.getErrorStream())) {
                        String errorResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        logger.severe("Error details: " + errorResponse);
                        
                        // Check for common errors
                        if (errorResponse.contains("InvalidAuthenticationToken")) {
                            logger.severe("Authentication token is invalid or expired. Please reconfigure SharePoint.");
                        } else if (errorResponse.contains("ResourceNotFound")) {
                            logger.severe("User not found: " + targetUserEmail + ". Check if the email is correct.");
                        } else if (errorResponse.contains("Forbidden")) {
                            logger.severe("Permission denied. Check if the app has Calendar.ReadWrite permission.");
                        }
                    }
                }
                
            } catch (Exception httpEx) {
                logger.severe("Failed to create calendar event via REST API: " + httpEx.getMessage());
                httpEx.printStackTrace();
            }
            
        } catch (Exception e) {
            logger.warning("Failed to create calendar event: " + e.getMessage());
        }
    }
    
    /**
     * Test the connection to Microsoft Graph
     */
    public boolean testConnection() {
        try {
            if (!initialize()) {
                return false;
            }
            
            // Test connection with a simpler approach
            // For now, just test the credential
            try {
                com.azure.core.credential.AccessToken token = credential.getToken(
                    new com.azure.core.credential.TokenRequestContext()
                        .addScopes("https://graph.microsoft.com/.default")
                ).block();
                if (token != null && token.getToken() != null) {
                    logger.info("Successfully obtained access token");
                } else {
                    return false;
                }
            } catch (Exception e) {
                logger.severe("Failed to get access token: " + e.getMessage());
                return false;
            }
            return true;
            
        } catch (Exception e) {
            logger.severe("Connection test failed: " + e.getMessage());
            return false;
        }
    }
}