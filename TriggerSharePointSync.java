import com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointConfig;

public class TriggerSharePointSync {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Manual SharePoint Sync Trigger");
        System.out.println("========================================\n");
        
        // Load configuration
        SharePointConfig config = SharePointConfig.getInstance();
        
        if (!config.isConfigured()) {
            System.err.println("SharePoint is not configured!");
            System.exit(1);
        }
        
        if (!config.isEnabled()) {
            System.err.println("SharePoint sync is not enabled!");
            System.err.println("Enable it in the SharePoint Integration dialog.");
            System.exit(1);
        }
        
        System.out.println("Configuration loaded:");
        System.out.println("  Site URL: " + config.getSiteUrl());
        System.out.println("  Site Name: " + config.getSiteName());
        System.out.println("  Sync Interval: " + config.getSyncIntervalMinutes() + " minutes");
        System.out.println("  Enabled: " + config.isEnabled());
        System.out.println();
        
        // Since we don't have the full sync implementation yet,
        // this would be where the sync would trigger
        System.out.println("NOTE: Full sync implementation requires Microsoft Graph SDK.");
        System.out.println("The automatic sync will run based on your configured interval.");
        System.out.println();
        System.out.println("For now, your options are:");
        System.out.println("1. Wait for the next automatic sync (every " + config.getSyncIntervalMinutes() + " minutes)");
        System.out.println("2. Restart the application to potentially trigger sync");
        System.out.println("3. Check SharePoint manually after the sync interval");
        System.out.println();
        System.out.println("Your sync should create these lists in SharePoint:");
        System.out.println("  - Field Projects");
        System.out.println("  - Field Resources");  
        System.out.println("  - Field Assignments");
    }
}