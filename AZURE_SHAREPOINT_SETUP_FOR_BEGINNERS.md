# Complete Azure & SharePoint Setup Guide for Beginners
*Step-by-step instructions with screenshots references*

## Overview
We're going to connect your Java application to Microsoft 365 so field technicians can see their schedules on their phones. Think of it like giving your app permission to talk to SharePoint.

## Part 1: Azure AD App Registration (15 minutes)

Think of this like creating a "service account" that your Java app will use to access SharePoint.

### Step 1: Access Azure Portal
1. Open your browser and go to: https://portal.azure.com
2. Sign in with your work account (same one you use for SharePoint admin)
3. You should see the Azure Portal dashboard

### Step 2: Navigate to App Registrations
1. In the search bar at the top, type: **"App registrations"**
2. Click on **"App registrations"** under Services
3. Click the **"+ New registration"** button at the top

### Step 3: Register Your Application
Fill in these fields exactly:

1. **Name**: `Simple Project Resource Manager`
   - This is just a friendly name, you can change it later

2. **Supported account types**: 
   - Select: `Accounts in this organizational directory only (Your Company only - Single tenant)`
   - This means only your company can use this app

3. **Redirect URI**: 
   - Leave this blank for now (we don't need it)

4. Click **"Register"** button at the bottom

### Step 4: Save Your IDs (IMPORTANT!)
After registration, you'll see an overview page. Write these down:

1. **Application (client) ID**: Looks like `12345678-1234-1234-1234-123456789abc`
2. **Directory (tenant) ID**: Looks like `87654321-4321-4321-4321-123456789xyz`

Save these in Notepad - you'll need them soon!

### Step 5: Create a Client Secret (Password)
This is like creating a password for your app:

1. In the left menu, click **"Certificates & secrets"**
2. Click **"+ New client secret"**
3. Description: `Java App Secret`
4. Expires: Select **"24 months"** (you'll need to renew it in 2 years)
5. Click **"Add"**

⚠️ **CRITICAL**: Copy the **Value** immediately! It looks like: `8Q8~Vt.fakevalue.1234~real`
- You can ONLY see this once
- If you lose it, you'll have to create a new one
- Save it in your Notepad with the other IDs

### Step 6: Grant SharePoint Permissions
Now we tell Azure what your app is allowed to do:

1. In the left menu, click **"API permissions"**
2. Click **"+ Add a permission"**
3. Choose **"Microsoft Graph"** (it has a graph icon)
4. Choose **"Application permissions"** (not Delegated)
5. Search for and check these permissions:
   - ✅ `Sites.ReadWrite.All` (under Sites)
   - ✅ `User.Read.All` (under User)
6. Click **"Add permissions"** at the bottom

### Step 7: Grant Admin Consent
This approves the permissions:

1. Still on the API permissions page
2. Click **"✅ Grant admin consent for [Your Company]"** button
3. Click **"Yes"** to confirm
4. You should see green checkmarks ✅ appear next to all permissions

**Congratulations!** Azure AD setup is complete! You should have these three values saved:
- Tenant ID
- Client ID  
- Client Secret

---

## Part 2: Create Microsoft Lists (10 minutes)

Now let's create the lists that will store your data in the cloud.

### Step 1: Access Microsoft Lists
1. Go to: https://www.office.com
2. Sign in with your work account
3. Click the app launcher (9 dots ⋮⋮⋮ in top left)
4. Click **"Lists"** (if you don't see it, click "All apps" first)

### Step 2: Create Your First List - Projects
1. Click **"+ New list"**
2. Choose **"Blank list"**
3. Name: `Field Projects`
4. Description: `Project scheduling and tracking`
5. Color: Choose any color you like
6. Icon: Choose the calendar or clipboard icon
7. Click **"Create"**

### Step 3: Add Columns to Projects List
After the list opens, we need to add columns:

1. Click **"+ Add column"** button
2. For each column below, choose the type and settings:

**ProjectID** 
- Type: Single line of text
- Required: Yes
- Click Save

**StartDate**
- Type: Date and time
- Include time: No
- Required: Yes
- Click Save

**EndDate**
- Type: Date and time
- Include time: No
- Required: Yes
- Click Save

**Status**
- Type: Choice
- Choices (one per line):
  - Planning
  - Active
  - On Hold
  - Complete
  - Cancelled
- Default: Planning
- Click Save

**Location**
- Type: Single line of text
- Required: No
- Click Save

**Budget**
- Type: Currency
- Currency: USD (or your currency)
- Required: No
- Click Save

### Step 4: Create Resources List
1. Go back to Lists home (click "Lists" in top navigation)
2. Click **"+ New list"** → **"Blank list"**
3. Name: `Field Resources`
4. Create and add these columns:

**ResourceID**
- Type: Single line of text
- Required: Yes

**Email**
- Type: Single line of text
- Required: Yes

**Phone**
- Type: Single line of text
- Required: No

**ResourceType**
- Type: Choice
- Choices:
  - Employee
  - Contractor
  - 3rd Party
  - Equipment

**DomainLogin**
- Type: Single line of text
- Required: Yes
- Description: "User's login name (e.g., jsmith)"

### Step 5: Create Assignments List
1. Create another new list named `Field Assignments`
2. Add these columns:

**AssignmentID**
- Type: Single line of text
- Required: Yes

**ResourceName**
- Type: Single line of text
- Required: Yes

**ProjectName**
- Type: Single line of text
- Required: Yes

**StartDate**
- Type: Date and time
- Include time: No
- Required: Yes

**EndDate**
- Type: Date and time
- Include time: No
- Required: Yes

**Status**
- Type: Choice
- Choices:
  - Scheduled
  - In Progress
  - Complete

**TravelDays**
- Type: Number
- Default value: 0

---

## Part 3: Create SharePoint Site (5 minutes)

Let's create a nice home for your lists.

### Step 1: Create Team Site
1. Go to: https://[yourcompany].sharepoint.com
2. Click **"+ Create site"** (usually in top navigation)
3. Choose **"Team site"**
4. Site name: `Field Operations`
5. Description: `Field service scheduling and resource management`
6. Privacy: **Private** (only specific people can access)
7. Click **"Next"**
8. Add yourself as owner for now
9. Click **"Finish"**

### Step 2: Add Lists to Your Site
1. In your new Field Operations site
2. Click **"New"** → **"App"**
3. Search for "Lists"
4. Click **"Lists"** to add the app
5. Your Microsoft Lists will now be accessible from this site

---

## Part 4: Configure Your Java Application (5 minutes)

Now let's tell your Java app about all this setup.

### Step 1: Run Your Application
```cmd
cd C:\Users\mmiller\IdeaProjects\SimpleProjectResourceManager
mvn javafx:run
```

### Step 2: Configure Domain Logins
1. In the app menu: **Tools** → **Configure Domain Logins...**
2. For each resource/person:
   - Enter their login username (e.g., `jsmith` or `john.smith`)
   - You can click "Auto-fill from Email" to extract from email addresses
3. Click **OK** to save

### Step 3: Configure SharePoint Integration
1. In the app menu: **Tools** → **SharePoint Integration...**
2. Fill in the values you saved earlier:

**Azure AD Configuration:**
- Tenant ID: `[paste your tenant ID]`
- Client ID: `[paste your client ID]`
- Client Secret: `[paste your client secret]`

**SharePoint Configuration:**
- Site URL: `https://[yourcompany].sharepoint.com`
- Site Name: `Field-Operations` (or whatever you named it)

3. Check ✅ **"Enable automatic synchronization"**
4. Leave sync interval at 30 minutes
5. Click **"Test Connection"**

If you see "Connection successful!" - everything is working!

---

## Part 5: Test Everything (5 minutes)

### Step 1: Create Test Data
1. In your Java app, create a test project:
   - Name: "Test SharePoint Sync"
   - Start: Today
   - End: Next week
   - Status: Active

2. Assign a resource to it

### Step 2: Check Microsoft Lists
1. Go back to https://lists.live.com
2. Open "Field Projects" list
3. You should see your test project appear within a minute!

### Step 3: Test Mobile Access
1. On your phone, download **"Microsoft Lists"** app (not SharePoint)
2. Sign in with your work account
3. You'll see all three lists
4. Open "Field Assignments" 
5. You can filter to see only your assignments!

---

## Troubleshooting Common Issues

### "Test Connection" Fails
**Problem**: Authentication error
**Solution**: 
1. Double-check your Tenant ID, Client ID, and Secret
2. Make sure you clicked "Grant admin consent" in Azure
3. Wait 5 minutes (Azure sometimes needs time to propagate)

### Lists Don't Appear
**Problem**: Can't see lists in SharePoint site
**Solution**:
1. Make sure you created them in Microsoft Lists first
2. They automatically sync to SharePoint
3. Try refreshing the page

### No Data Syncing
**Problem**: Created project but don't see it in Lists
**Solution**:
1. Check that "Enable automatic synchronization" is checked
2. Click "Test Connection" again
3. Check the Java console for error messages

### Permission Denied
**Problem**: Getting 403 or permission errors
**Solution**:
1. Go back to Azure Portal → App registrations
2. Make sure you have `Sites.ReadWrite.All` permission
3. Click "Grant admin consent" again

---

## What Happens Next?

Once everything is working:

1. **Automatic Sync**: Every 30 minutes, your Java app will push updates to SharePoint
2. **Mobile Access**: Field technicians can see their schedules on their phones
3. **Notifications**: You can set up Power Automate to send alerts for new assignments
4. **Reports**: Managers can view everything in SharePoint/Teams

---

## Quick Reference Card

Save this information for later:

**Azure Portal**: https://portal.azure.com
**Microsoft Lists**: https://lists.live.com
**Your SharePoint Site**: https://[yourcompany].sharepoint.com/sites/Field-Operations

**Your App IDs**:
```
Tenant ID: ________________________________
Client ID: ________________________________
Secret: ___________________________________
Secret Expires: ___________________________
```

**Support Contacts**:
- Azure/SharePoint Admin: (your IT department)
- Microsoft Support: 1-800-865-9408

---

## Next Steps

1. ✅ Test with one or two real projects
2. ✅ Have a field technician test mobile access
3. ✅ Set up daily sync schedule
4. ✅ Create Power Automate flow for notifications (optional)

**Need Help?** 
- This guide: AZURE_SHAREPOINT_SETUP_FOR_BEGINNERS.md
- Technical details: SHAREPOINT_MODERN_INTEGRATION_GUIDE.md
- Integration plan: SHAREPOINT_INTEGRATION_PLAN.md

---

*You did it! Your Java app is now connected to Microsoft 365!*