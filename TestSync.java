import com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SimpleSharePointSync;
import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import javax.sql.DataSource;

public class TestSync {
    public static void main(String[] args) {
        try {
            System.out.println("===========================================");
            System.out.println("Testing SharePoint Sync");
            System.out.println("===========================================");
            
            // Get data source
            DataSource dataSource = DatabaseConfig.getInstance().getDataSource();
            
            // Create sync client
            SimpleSharePointSync syncClient = new SimpleSharePointSync(dataSource);
            
            // Test connection
            System.out.println("\n1. Testing connection...");
            boolean connected = syncClient.testConnection();
            if (connected) {
                System.out.println("   ✓ Connection successful!");
            } else {
                System.out.println("   ✗ Connection failed!");
                return;
            }
            
            // Run sync
            System.out.println("\n2. Running sync...");
            syncClient.syncAll();
            System.out.println("   ✓ Sync completed!");
            
            System.out.println("\n===========================================");
            System.out.println("Test completed successfully!");
            System.out.println("===========================================");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}