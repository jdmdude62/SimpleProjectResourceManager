import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.service.SharePointSyncService;

public class TriggerManualSync {
    public static void main(String[] args) {
        try {
            System.out.println("===========================================");
            System.out.println("Triggering Manual SharePoint Sync");
            System.out.println("===========================================");
            
            // Get the sync service instance
            SharePointSyncService syncService = SharePointSyncService.getInstance(
                DatabaseConfig.getInstance().getDataSource()
            );
            
            // Trigger manual sync
            System.out.println("\nTriggering sync now...");
            syncService.syncNow();
            
            // Give it a moment to start
            Thread.sleep(2000);
            
            System.out.println("\nSync has been triggered!");
            System.out.println("Check the application logs for details.");
            System.out.println("\nNote: Since we're using a simplified sync,");
            System.out.println("calendar events are not actually created yet.");
            System.out.println("The sync will log what it would create.");
            System.out.println("===========================================");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}