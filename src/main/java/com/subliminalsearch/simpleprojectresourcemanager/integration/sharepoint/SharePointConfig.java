package com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class SharePointConfig {
    private static final Logger logger = Logger.getLogger(SharePointConfig.class.getName());
    private static final String CONFIG_FILE = "sharepoint.properties";
    private static SharePointConfig instance;
    
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String siteUrl;
    private String siteName;
    private boolean enabled;
    private int syncIntervalMinutes;
    
    private SharePointConfig() {
        loadConfiguration();
    }
    
    public static synchronized SharePointConfig getInstance() {
        if (instance == null) {
            instance = new SharePointConfig();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        Path configPath = getConfigPath();
        Properties props = new Properties();
        
        if (Files.exists(configPath)) {
            try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
                props.load(fis);
                
                this.tenantId = props.getProperty("sharepoint.tenant.id", "");
                this.clientId = props.getProperty("sharepoint.client.id", "");
                this.clientSecret = props.getProperty("sharepoint.client.secret", "");
                this.siteUrl = props.getProperty("sharepoint.site.url", "");
                this.siteName = props.getProperty("sharepoint.site.name", "field-operations");
                this.enabled = Boolean.parseBoolean(props.getProperty("sharepoint.enabled", "false"));
                this.syncIntervalMinutes = Integer.parseInt(props.getProperty("sharepoint.sync.interval", "30"));
                
                logger.info("SharePoint configuration loaded successfully");
            } catch (IOException e) {
                logger.severe("Failed to load SharePoint configuration: " + e.getMessage());
                setDefaults();
            }
        } else {
            logger.info("SharePoint configuration not found, creating default configuration");
            setDefaults();
            saveConfiguration();
        }
    }
    
    private void setDefaults() {
        this.tenantId = "";
        this.clientId = "";
        this.clientSecret = "";
        this.siteUrl = "";
        this.siteName = "field-operations";
        this.enabled = false;
        this.syncIntervalMinutes = 30;
    }
    
    public void saveConfiguration() {
        Path configPath = getConfigPath();
        Properties props = new Properties();
        
        props.setProperty("sharepoint.tenant.id", tenantId);
        props.setProperty("sharepoint.client.id", clientId);
        props.setProperty("sharepoint.client.secret", clientSecret);
        props.setProperty("sharepoint.site.url", siteUrl);
        props.setProperty("sharepoint.site.name", siteName);
        props.setProperty("sharepoint.enabled", String.valueOf(enabled));
        props.setProperty("sharepoint.sync.interval", String.valueOf(syncIntervalMinutes));
        
        try {
            Files.createDirectories(configPath.getParent());
            try (FileOutputStream fos = new FileOutputStream(configPath.toFile())) {
                props.store(fos, "SharePoint Integration Configuration");
                logger.info("SharePoint configuration saved successfully");
            }
        } catch (IOException e) {
            logger.severe("Failed to save SharePoint configuration: " + e.getMessage());
        }
    }
    
    private Path getConfigPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".SimpleProjectResourceManager", CONFIG_FILE);
    }
    
    public boolean isConfigured() {
        return !tenantId.isEmpty() && !clientId.isEmpty() && !clientSecret.isEmpty() && !siteUrl.isEmpty();
    }
    
    // Getters and setters
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getSiteUrl() {
        return siteUrl;
    }
    
    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }
    
    public String getSiteName() {
        return siteName;
    }
    
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getSyncIntervalMinutes() {
        return syncIntervalMinutes;
    }
    
    public void setSyncIntervalMinutes(int syncIntervalMinutes) {
        this.syncIntervalMinutes = syncIntervalMinutes;
    }
    
    public String getListsApiUrl() {
        return siteUrl + "/sites/" + siteName + "/_api/web/lists";
    }
    
    public String getListItemsApiUrl(String listId) {
        return siteUrl + "/sites/" + siteName + "/_api/web/lists(guid'" + listId + "')/items";
    }
}