# SharePoint Integration Guide for Simple Project Resource Manager
*Updated: August 29, 2025*

## Quick Start Guide

### Step 1: Configure Domain Logins
Before enabling SharePoint sync, map your resources to their Active Directory accounts:

1. **Open Domain Login Configuration**
   - Go to `Tools` → `Configure Domain Logins...`
   - The dialog shows all resources with their current domain login mappings

2. **Enter Domain Logins**
   - For each resource, enter their domain login in one of these formats:
     - `username@domain.com` (preferred for Microsoft 365)
     - `DOMAIN\username` (traditional Active Directory format)
     - `username` (will use default domain)
   
3. **Auto-fill Option**
   - Click "Auto-fill from Email" to automatically extract usernames from email addresses
   - This works well if your email format matches your domain logins

4. **Save Configuration**
   - Click OK to save all domain login mappings
   - These will be used for SharePoint user field mappings and permissions

### Step 2: Configure SharePoint Integration
1. **Open SharePoint Settings**
   - Go to `Tools` → `SharePoint Integration...`
   
2. **Azure AD Configuration**
   - **Tenant ID**: Your Azure AD tenant ID (found in Azure Portal)
   - **Client ID**: Application (client) ID from app registration
   - **Client Secret**: Secret value from Certificates & secrets
   
3. **SharePoint Site Configuration**
   - **Site URL**: `https://yourcompany.sharepoint.com`
   - **Site Name**: `field-operations` (or your preferred site name)
   
4. **Enable Synchronization**
   - Check "Enable automatic synchronization"
   - Set sync interval (default: 30 minutes)
   
5. **Test Connection**
   - Click "Test Connection" to verify credentials
   - Ensure connection is successful before proceeding

### Step 3: Create SharePoint Lists
Create three lists in your SharePoint site with these schemas:

#### Projects List
| Column Name | Type | Required | Indexed |
|------------|------|----------|---------|
| Title | Single line of text | Yes | No |
| ProjectID | Single line of text | Yes | Yes |
| StartDate | Date | Yes | No |
| EndDate | Date | Yes | No |
| Status | Choice (Planning, Active, On Hold, Completed, Cancelled) | Yes | No |
| Location | Single line of text | No | No |
| Description | Multiple lines of text | No | No |
| ProjectManager | Person or Group | No | No |
| Budget | Currency | No | No |

#### Resources List
| Column Name | Type | Required | Indexed |
|------------|------|----------|---------|
| Title | Single line of text (Full Name) | Yes | No |
| ResourceID | Single line of text | Yes | Yes |
| Email | Single line of text | Yes | No |
| Phone | Single line of text | No | No |
| ResourceType | Choice (Employee, Contractor, 3rd Party, Equipment) | Yes | No |
| Department | Single line of text | No | No |
| Skills | Multiple lines of text | No | No |
| DomainLogin | Single line of text | Yes | Yes |

#### Assignments List
| Column Name | Type | Required | Indexed |
|------------|------|----------|---------|
| Title | Single line of text (Auto: Resource - Project) | Yes | No |
| AssignmentID | Single line of text | Yes | Yes |
| ResourceID | Lookup (Resources list) | Yes | No |
| ProjectID | Lookup (Projects list) | Yes | No |
| StartDate | Date | Yes | No |
| EndDate | Date | Yes | No |
| Status | Choice (Scheduled, In Progress, Completed) | Yes | No |
| Notes | Multiple lines of text | No | No |
| TravelDays | Number | No | No |

### Step 4: Configure Permissions
1. **Create Security Groups**
   - `Field Technicians` - Read access to their own assignments
   - `Project Managers` - Read/Write access to their projects
   - `Schedulers` - Full control of all lists

2. **Apply Item-Level Permissions**
   - Resources see only their assignments (filtered by DomainLogin)
   - Project Managers see all assignments for their projects

### Step 5: Configure Mobile Access
1. **SharePoint Mobile App Setup**
   - Install SharePoint app on iOS/Android devices
   - Sign in with corporate credentials
   - Navigate to Field Operations site
   - Pin "My Assignments" view for quick access

2. **Create Personal Views**
   - "My Schedule" - Filtered by [Me] in ResourceID lookup
   - "This Week" - Filtered by date range
   - "Upcoming Projects" - Next 30 days

## How It Works

### Data Flow
1. **Java Application → SharePoint**
   - Creates/updates occur in real-time when changes are made
   - Batch sync runs every 30 minutes (configurable)
   - Domain logins map resources to SharePoint users

2. **Mobile Access**
   - Field technicians use SharePoint mobile app
   - Views are filtered based on logged-in user's domain login
   - Push notifications for new assignments

3. **Permissions**
   - Domain login mapping enables automatic filtering
   - SharePoint's [Me] filter shows only user's assignments
   - Project Managers see team assignments

## Administrator Setup Checklist

- [ ] Map all resources to domain logins
- [ ] Register app in Azure AD
- [ ] Grant SharePoint API permissions
- [ ] Create client secret
- [ ] Configure integration in application
- [ ] Create SharePoint site and lists
- [ ] Set up security groups
- [ ] Configure item-level permissions
- [ ] Create filtered views
- [ ] Test with sample data
- [ ] Train field technicians on mobile app
- [ ] Enable production sync

## Troubleshooting

### Common Issues

**Issue: Resources not appearing in SharePoint**
- Solution: Ensure domain login is configured for the resource
- Check: Tools → Configure Domain Logins

**Issue: Authentication fails**
- Solution: Verify Azure AD app permissions include Sites.ReadWrite.All
- Check client secret hasn't expired

**Issue: Mobile app doesn't show assignments**
- Solution: Verify DomainLogin field matches logged-in user
- Check view filters are configured correctly

**Issue: Sync appears slow**
- Solution: Adjust sync interval in settings
- Consider batch size for large datasets

## Security Best Practices

1. **Credential Management**
   - Store client secrets securely
   - Rotate secrets regularly
   - Use certificate authentication for production

2. **Data Protection**
   - Enable audit logging on SharePoint lists
   - Implement data retention policies
   - Use encryption for sensitive project data

3. **Access Control**
   - Regular review of permissions
   - Remove access for terminated employees
   - Use security groups, not individual permissions

## Advanced Configuration

### Power Automate Integration
Create flows for:
- New assignment notifications
- Daily schedule emails
- Project status updates
- Resource availability alerts

### Power Apps Custom Forms
Build mobile-optimized forms for:
- Time entry
- Status updates
- Issue reporting
- Document uploads

### Teams Integration
- Embed schedule views in Teams channels
- Enable Teams notifications
- Create project-specific Teams channels
- Sync project documents

## API Reference

### Authentication
```http
POST https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token
Content-Type: application/x-www-form-urlencoded

client_id={client-id}
&scope=https://graph.microsoft.com/.default
&client_secret={client-secret}
&grant_type=client_credentials
```

### Create List Item
```http
POST https://{site}.sharepoint.com/sites/{site-name}/_api/web/lists/getbytitle('Projects')/items
Authorization: Bearer {token}
Content-Type: application/json

{
  "Title": "Project Name",
  "ProjectID": "PROJ-001",
  "StartDate": "2025-08-29",
  "EndDate": "2025-09-15",
  "Status": "Active"
}
```

### Update with Domain User
```http
POST https://{site}.sharepoint.com/sites/{site-name}/_api/web/lists/getbytitle('Resources')/items
Authorization: Bearer {token}
Content-Type: application/json

{
  "Title": "John Smith",
  "ResourceID": "RES-001",
  "Email": "john.smith@company.com",
  "DomainLogin": "jsmith",
  "ResourceType": "Employee"
}
```

## Support and Resources

- **Documentation**: [SharePoint REST API Reference](https://docs.microsoft.com/en-us/sharepoint/dev/sp-add-ins/get-to-know-the-sharepoint-rest-service)
- **Microsoft Graph**: [Graph API Documentation](https://docs.microsoft.com/en-us/graph/)
- **Power Platform**: [Power Apps and Power Automate](https://docs.microsoft.com/en-us/power-platform/)
- **Support**: Contact your SharePoint administrator

---

*This guide provides comprehensive instructions for integrating the Simple Project Resource Manager with SharePoint for mobile field access.*