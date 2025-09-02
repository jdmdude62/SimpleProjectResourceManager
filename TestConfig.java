import com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointConfig;

public class TestConfig {
    public static void main(String[] args) {
        System.out.println("Testing SharePoint Configuration...\n");
        
        // Test if we can load/save config without UI
        SharePointConfig config = SharePointConfig.getInstance();
        
        // Set your values
        config.setTenantId("c9e3b7e3-60fd-472d-beb0-dcffda096f4c");
        config.setSiteUrl("https://captechno.sharepoint.com");
        config.setSiteName("Field-Operations");
        
        System.out.println("Current configuration:");
        System.out.println("Tenant ID: " + config.getTenantId());
        System.out.println("Site URL: " + config.getSiteUrl());
        System.out.println("Site Name: " + config.getSiteName());
        System.out.println("\nNote: You still need to set Client ID and Secret");
        
        // Save it
        config.saveConfiguration();
        System.out.println("\nConfiguration saved to: %USERPROFILE%\\.SimpleProjectResourceManager\\sharepoint.properties");
        System.out.println("\nYou can edit this file directly to add:");
        System.out.println("sharepoint.client.id=YOUR_CLIENT_ID");
        System.out.println("sharepoint.client.secret=YOUR_CLIENT_SECRET");
        System.out.println("sharepoint.enabled=true");
    }
}