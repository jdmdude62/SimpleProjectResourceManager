package com.subliminalsearch.simpleprojectresourcemanager;

import com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Standalone test to verify SharePoint connection without full implementation
 * Run this after configuring SharePoint settings in the application
 */
public class TestSharePointConnection {
    
    public static void main(String[] args) {
        System.out.println("SharePoint Connection Test");
        System.out.println("==========================\n");
        
        SharePointConfig config = SharePointConfig.getInstance();
        
        // Check configuration
        if (!config.isConfigured()) {
            System.err.println("ERROR: SharePoint is not configured!");
            System.err.println("Please run the application and configure SharePoint first:");
            System.err.println("  Tools → SharePoint Integration...");
            System.exit(1);
        }
        
        System.out.println("Configuration loaded:");
        System.out.println("  Tenant ID: " + maskString(config.getTenantId()));
        System.out.println("  Client ID: " + maskString(config.getClientId()));
        System.out.println("  Site URL: " + config.getSiteUrl());
        System.out.println("  Site Name: " + config.getSiteName());
        System.out.println();
        
        // Test authentication
        System.out.println("Testing authentication...");
        String accessToken = getAccessToken(config);
        
        if (accessToken != null) {
            System.out.println("✓ Authentication successful!");
            System.out.println("  Token (first 20 chars): " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
            System.out.println();
            
            // Test site access
            System.out.println("Testing site access...");
            if (testSiteAccess(config, accessToken)) {
                System.out.println("✓ Site access successful!");
                System.out.println();
                
                // List existing lists (if any)
                System.out.println("Checking for existing lists...");
                checkExistingLists(config, accessToken);
                
                System.out.println("\n=== TEST SUCCESSFUL ===");
                System.out.println("SharePoint integration is properly configured!");
                System.out.println("\nNext steps:");
                System.out.println("1. Create the required lists in SharePoint");
                System.out.println("2. Run the full sync implementation");
            } else {
                System.err.println("✗ Failed to access site");
                System.err.println("Check that the site URL and name are correct");
            }
        } else {
            System.err.println("✗ Authentication failed");
            System.err.println("Check your Azure AD credentials");
        }
    }
    
    private static String getAccessToken(SharePointConfig config) {
        try {
            String tokenUrl = "https://login.microsoftonline.com/" + config.getTenantId() + "/oauth2/v2.0/token";
            
            String params = "client_id=" + URLEncoder.encode(config.getClientId(), "UTF-8") +
                          "&scope=" + URLEncoder.encode("https://graph.microsoft.com/.default", "UTF-8") +
                          "&client_secret=" + URLEncoder.encode(config.getClientSecret(), "UTF-8") +
                          "&grant_type=client_credentials";
            
            URL url = new URL(tokenUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            
            conn.getOutputStream().write(params.getBytes(StandardCharsets.UTF_8));
            
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Parse access token from JSON response (simple parsing)
                String json = response.toString();
                int tokenStart = json.indexOf("\"access_token\":\"") + 16;
                int tokenEnd = json.indexOf("\"", tokenStart);
                return json.substring(tokenStart, tokenEnd);
            }
        } catch (Exception e) {
            System.err.println("Error getting access token: " + e.getMessage());
        }
        return null;
    }
    
    private static boolean testSiteAccess(SharePointConfig config, String accessToken) {
        try {
            String siteUrl = config.getSiteUrl() + "/sites/" + config.getSiteName() + "/_api/web";
            
            URL url = new URL(siteUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Accept", "application/json");
            
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            System.err.println("Error accessing site: " + e.getMessage());
            return false;
        }
    }
    
    private static void checkExistingLists(SharePointConfig config, String accessToken) {
        try {
            String listsUrl = config.getSiteUrl() + "/sites/" + config.getSiteName() + "/_api/web/lists";
            
            URL url = new URL(listsUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Accept", "application/json");
            
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String json = response.toString();
                
                // Check for our specific lists
                boolean hasProjects = json.contains("\"Title\":\"Projects\"");
                boolean hasResources = json.contains("\"Title\":\"Resources\"");
                boolean hasAssignments = json.contains("\"Title\":\"Assignments\"");
                
                System.out.println("  Projects list: " + (hasProjects ? "✓ Found" : "✗ Not found"));
                System.out.println("  Resources list: " + (hasResources ? "✓ Found" : "✗ Not found"));
                System.out.println("  Assignments list: " + (hasAssignments ? "✓ Found" : "✗ Not found"));
                
                if (!hasProjects || !hasResources || !hasAssignments) {
                    System.out.println("\nLists need to be created in SharePoint.");
                    System.out.println("See SHAREPOINT_INTEGRATION_GUIDE.md for list schemas.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking lists: " + e.getMessage());
        }
    }
    
    private static String maskString(String str) {
        if (str == null || str.length() < 8) {
            return "****";
        }
        return str.substring(0, 4) + "****" + str.substring(str.length() - 4);
    }
}