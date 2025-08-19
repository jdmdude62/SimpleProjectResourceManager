# SharePoint Integration Guide for Simple Project Resource Manager

## Overview
This guide provides step-by-step instructions for integrating the Simple Project Resource Manager with SharePoint to enable technician calendar synchronization using Active Directory groups for authentication and authorization.

## Prerequisites
- SharePoint Online or SharePoint Server 2019/2016
- Active Directory with configured groups:
  - "Project Managers" - Full access to project management
  - "CyberMetal" - Technician group for field service calendars
- Admin rights to SharePoint site
- Microsoft Graph API access (for calendar sync)

## Architecture Overview

```
┌─────────────────────┐
│  JavaFX Desktop App │
│  (Project Managers) │
└──────────┬──────────┘
           │
           ▼
    ┌──────────────┐
    │     LDAP     │◄────── Active Directory
    │Authentication│        Groups:
    └──────┬───────┘        - Project Managers
           │                - CyberMetal
           ▼
    ┌──────────────┐
    │  Export/Sync │
    │    Service   │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │  SharePoint  │
    │   Calendar   │◄────── Technicians view
    │     Lists    │        their schedules
    └──────────────┘
```

## Part 1: SharePoint Site Setup

### Step 1: Create SharePoint Site
1. Navigate to SharePoint Admin Center
2. Click "Create site" → "Team site"
3. Site details:
   - Name: "Project Resource Manager"
   - Description: "Field service scheduling and resource management"
   - Privacy: Private
   - Language: English

### Step 2: Configure Site Permissions
1. Go to Site Settings → Site Permissions
2. Create permission groups:
   ```
   - PRM Managers (Full Control)
   - PRM Technicians (Read/Contribute to calendars only)
   - PRM Viewers (Read only)
   ```

### Step 3: Map AD Groups to SharePoint Groups
1. Click "Grant Permissions"
2. Add Active Directory groups:
   ```
   AD Group: "Project Managers" → SharePoint Group: "PRM Managers"
   AD Group: "CyberMetal" → SharePoint Group: "PRM Technicians"
   ```

## Part 2: Create SharePoint Lists and Calendars

### Step 1: Create Master Assignment List
1. Site Contents → New → List
2. Name: "Field Assignments"
3. Add columns:
   ```
   - Technician (Person or Group)
   - Project ID (Single line text)
   - Project Name (Single line text)
   - Task Description (Multiple lines text)
   - Start Date (Date and Time)
   - End Date (Date and Time)
   - Location (Single line text)
   - Status (Choice: Scheduled, In Progress, Completed, Cancelled)
   - Materials Status (Choice: Pending, Ready, On Site)
   - PM Contact (Person or Group)
   - Notes (Multiple lines text)
   ```

### Step 2: Create Technician Calendar View
1. Open "Field Assignments" list
2. Create View → Calendar View
3. Configure:
   - Name: "Technician Schedule"
   - Time interval: Start Date to End Date
   - Title: Project ID + Task Description
   - Filter: [Me] in Technician field (shows only current user's assignments)

### Step 3: Create Individual Technician Calendars
For each technician, create a filtered calendar:
1. Create View → Calendar View
2. Name: "[Technician Name] Schedule"
3. Filter: Technician equals specific person
4. Set permissions so only that technician and managers can view

## Part 3: Configure Microsoft Graph API

### Step 1: Register Application in Azure AD
1. Go to Azure Portal → Azure Active Directory
2. App registrations → New registration
3. Configure:
   ```
   Name: Simple Project Resource Manager
   Supported account types: Single tenant
   Redirect URI: http://localhost:8080/callback (for desktop app)
   ```

### Step 2: Configure API Permissions
1. API permissions → Add permission
2. Microsoft Graph → Delegated permissions:
   ```
   - Calendars.ReadWrite
   - Sites.ReadWrite.All
   - User.Read
   - Group.Read.All
   ```

### Step 3: Create Client Secret
1. Certificates & secrets → New client secret
2. Store securely (will be used in application config)

### Step 4: Grant Admin Consent
1. Click "Grant admin consent for [Organization]"
2. Confirm permissions

## Part 4: Application Configuration

### Step 1: Update LDAP Configuration
Edit `LDAPService.java` configuration:
```java
private static final String LDAP_URL = "ldap://your-dc.company.com:389";
private static final String DOMAIN = "company.com";
private static final String BASE_DN = "DC=company,DC=com";
```

### Step 2: Configure SharePoint Connection
Create `sharepoint.properties`:
```properties
sharepoint.site.url=https://yourcompany.sharepoint.com/sites/ProjectResourceManager
sharepoint.tenant.id=your-tenant-id
sharepoint.client.id=your-client-id
sharepoint.client.secret=your-client-secret
sharepoint.list.assignments=Field Assignments
```

### Step 3: Enable Authentication Mode
Run application with LDAP authentication:
```bash
java -Dauth.mode=ldap -jar SimpleProjectResourceManager.jar
```

For development/testing without LDAP:
```bash
java -Dauth.mode=bypass -Ddev.mode=true -jar SimpleProjectResourceManager.jar
```

## Part 5: Data Synchronization

### Manual Export Process
1. In the application, select project(s) to export
2. Right-click → "Export to SharePoint"
3. Choose export options:
   - All assignments
   - Specific date range
   - Specific technicians

### Automatic Sync (Optional)
Configure scheduled sync in application settings:
```java
// In application configuration
scheduler.sharepoint.sync.enabled=true
scheduler.sharepoint.sync.interval=30 // minutes
scheduler.sharepoint.sync.mode=incremental
```

## Part 6: Technician Access

### For Technicians to View Their Calendars:

#### Option 1: SharePoint Site
1. Navigate to: https://yourcompany.sharepoint.com/sites/ProjectResourceManager
2. Click "Field Assignments" → "Technician Schedule"
3. Calendar shows only their assignments

#### Option 2: Outlook Integration
1. In SharePoint calendar, click "Connect to Outlook"
2. Calendar syncs to Outlook
3. View on desktop and mobile Outlook apps

#### Option 3: Teams Integration
1. In Microsoft Teams, add SharePoint tab
2. Select "Field Assignments" list
3. Choose "Technician Schedule" view

## Part 7: Security Considerations

### Data Access Control
- Project Managers: Full read/write access to all data
- Technicians: Read-only access to their own assignments
- No direct database access from SharePoint
- All authentication through Active Directory

### Audit Trail
SharePoint automatically maintains:
- Version history for all list items
- Who made changes and when
- Ability to restore previous versions

## Part 8: Troubleshooting

### Common Issues and Solutions

#### 1. LDAP Connection Failed
```
Error: Cannot connect to authentication server
Solution:
- Verify LDAP URL and port (usually 389 or 636 for SSL)
- Check firewall rules
- Ensure service account has read permissions
```

#### 2. SharePoint List Not Found
```
Error: List 'Field Assignments' not found
Solution:
- Verify list name matches configuration
- Check user has permissions to the list
- Ensure site URL is correct
```

#### 3. Calendar Not Syncing
```
Error: Failed to sync calendar events
Solution:
- Check Microsoft Graph API permissions
- Verify API credentials are valid
- Check SharePoint site permissions
```

#### 4. Technicians Can't See Calendar
```
Issue: Technician logged in but calendar is empty
Solution:
- Verify technician is in "CyberMetal" AD group
- Check SharePoint group membership
- Ensure calendar view filter is correct
```

## Part 9: Testing Checklist

Before going live, test:

- [ ] LDAP authentication with Project Manager account
- [ ] LDAP authentication with Technician account
- [ ] Export single project to SharePoint
- [ ] Export multiple projects to SharePoint
- [ ] Technician can view their calendar in SharePoint
- [ ] Technician cannot view other technicians' calendars
- [ ] Project Manager can view all calendars
- [ ] Calendar syncs to Outlook correctly
- [ ] Session timeout works (30 minutes)
- [ ] Audit trail captures changes

## Part 10: Maintenance

### Regular Tasks
1. **Weekly**: Review sync logs for errors
2. **Monthly**: Check SharePoint storage usage
3. **Quarterly**: Review and update AD group memberships
4. **Annually**: Review and rotate API credentials

### Backup Strategy
1. SharePoint lists are automatically backed up by Microsoft
2. Export critical data weekly to CSV
3. Maintain local database backups

## Support Contacts

For issues with:
- **Application**: IT Help Desk (ext. 1234)
- **SharePoint**: SharePoint Admin (sharepoint@company.com)
- **Active Directory**: AD Admin (adadmin@company.com)
- **Developer Support**: dev-team@company.com

## Appendix A: PowerShell Scripts

### Create SharePoint List via PowerShell
```powershell
Connect-PnPOnline -Url "https://yourcompany.sharepoint.com/sites/ProjectResourceManager"

$listName = "Field Assignments"
New-PnPList -Title $listName -Template GenericList

# Add columns
Add-PnPField -List $listName -DisplayName "Technician" -InternalName "Technician" -Type User
Add-PnPField -List $listName -DisplayName "Project ID" -InternalName "ProjectID" -Type Text
Add-PnPField -List $listName -DisplayName "Start Date" -InternalName "StartDate" -Type DateTime
# ... add remaining columns
```

### Export Assignments to SharePoint
```powershell
# Run from application server
$csvPath = "C:\Exports\assignments.csv"
$assignments = Import-Csv $csvPath

foreach ($assignment in $assignments) {
    Add-PnPListItem -List "Field Assignments" -Values @{
        "Technician" = $assignment.Technician
        "ProjectID" = $assignment.ProjectID
        "StartDate" = $assignment.StartDate
        # ... map remaining fields
    }
}
```

## Appendix B: Sample Configuration Files

### LDAP Test Script (ldap-test.ps1)
```powershell
$username = Read-Host "Enter username"
$password = Read-Host "Enter password" -AsSecureString
$domain = "company.com"

$cred = New-Object System.Management.Automation.PSCredential ("$username@$domain", $password)

try {
    $searcher = New-Object DirectoryServices.DirectorySearcher
    $searcher.Filter = "(samaccountname=$username)"
    $result = $searcher.FindOne()
    
    if ($result) {
        Write-Host "Authentication successful!"
        Write-Host "Groups:"
        $result.Properties.memberof | ForEach-Object { Write-Host " - $_" }
    }
} catch {
    Write-Host "Authentication failed: $_"
}
```

---

**Document Version**: 1.0
**Last Updated**: Today
**Next Review**: Quarterly