package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointConfig;
import com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SimpleSharePointSync;

import javax.sql.DataSource;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Service to manage SharePoint synchronization
 */
public class SharePointSyncService {
    private static final Logger logger = Logger.getLogger(SharePointSyncService.class.getName());
    private static SharePointSyncService instance;
    
    private final DataSource dataSource;
    private final SharePointConfig config;
    private Timer syncTimer;
    private SimpleSharePointSync syncClient;
    
    private SharePointSyncService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.config = SharePointConfig.getInstance();
        this.syncClient = new SimpleSharePointSync(dataSource);
    }
    
    public static synchronized SharePointSyncService getInstance(DataSource dataSource) {
        if (instance == null) {
            instance = new SharePointSyncService(dataSource);
        }
        return instance;
    }
    
    /**
     * Start automatic synchronization
     */
    public void startSync() {
        if (!config.isEnabled() || !config.isConfigured()) {
            logger.info("SharePoint sync is not enabled or configured");
            return;
        }
        
        stopSync(); // Stop any existing timer
        
        syncTimer = new Timer("SharePoint-Sync", true);
        
        TimerTask syncTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    logger.info("Running scheduled SharePoint sync...");
                    syncClient.syncAll();
                } catch (Exception e) {
                    logger.severe("Scheduled sync failed: " + e.getMessage());
                }
            }
        };
        
        // Schedule to run every X minutes
        long intervalMillis = config.getSyncIntervalMinutes() * 60L * 1000L;
        
        // Run first sync immediately, then repeat at intervals
        logger.info("Running initial SharePoint sync immediately...");
        syncTask.run(); // Run once immediately
        
        // Then schedule regular runs
        syncTimer.scheduleAtFixedRate(syncTask, intervalMillis, intervalMillis);
        
        logger.info("SharePoint sync scheduled to run every " + config.getSyncIntervalMinutes() + " minutes");
    }
    
    /**
     * Stop automatic synchronization
     */
    public void stopSync() {
        if (syncTimer != null) {
            syncTimer.cancel();
            syncTimer = null;
            logger.info("SharePoint sync stopped");
        }
    }
    
    /**
     * Run sync immediately
     */
    public void syncNow() {
        if (!config.isEnabled() || !config.isConfigured()) {
            logger.warning("SharePoint is not configured or enabled");
            return;
        }
        
        logger.info("Running manual SharePoint sync...");
        
        // Run in a separate thread to avoid blocking UI
        new Thread(() -> {
            try {
                syncClient.syncAll();
                logger.info("Manual sync completed");
            } catch (Exception e) {
                logger.severe("Manual sync failed: " + e.getMessage());
            }
        }, "SharePoint-Manual-Sync").start();
    }
    
    /**
     * Test SharePoint connection
     */
    public boolean testConnection() {
        return syncClient.testConnection();
    }
}