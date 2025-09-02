# SharePoint Setup Checklist

*Check off each item as you complete it*

## 📋 Quick Setup Checklist

### Part 1: Azure Setup (15 minutes)

- [ ] Go to https://portal.azure.com
- [ ] Create new App Registration named "Simple Project Resource Manager"
- [ ] Copy and save these three values:
  - [ ] Tenant ID: _______________________________
  - [ ] Client ID: _______________________________
  - [ ] Client Secret: ___________________________
- [ ] Add permission: `Sites.ReadWrite.All`
- [ ] Click "Grant admin consent"

### Part 2: SharePoint/Lists Setup (10 minutes)

#### Option A: Automatic (Recommended)

- [ ] Open Command Prompt as Administrator
- [ ] Navigate to project folder: `cd C:\Users\mmiller\IdeaProjects\SimpleProjectResourceManager`
- [ ] Run: `setup-sharepoint.bat`
- [ ] Enter your SharePoint site URL when prompted
- [ ] Sign in when the login window appears

#### Option B: Manual

- [ ] Go to https://lists.live.com
- [ ] Create list: "Field Projects"
- [ ] Create list: "Field Resources" 
- [ ] Create list: "Field Assignments"
- [ ] Add columns as specified in the guide

### Part 3: Java Application Setup (5 minutes)

- [ ] Run the application: `mvn javafx:run`
- [ ] Go to Tools → Configure Domain Logins
  - [ ] Enter login names for each resource
  - [ ] Click OK to save
- [ ] Go to Tools → SharePoint Integration
  - [ ] Enter Tenant ID
  - [ ] Enter Client ID
  - [ ] Enter Client Secret
  - [ ] Enter Site URL (https://yourcompany.sharepoint.com)
  - [ ] Enter Site Name (Field-Operations)
  - [ ] Check "Enable automatic synchronization"
  - [ ] Click "Test Connection"
  - [ ] See "Connection successful!" message

### Part 4: Verification (2 minutes)

- [ ] Run: `verify-sharepoint-setup.bat`
- [ ] Confirm test passes
- [ ] Create a test project in Java app
- [ ] Check it appears in Microsoft Lists within 1 minute

## ✅ You're Done!

### Test Mobile Access

1. Download "Microsoft Lists" app on your phone
2. Sign in with work account
3. Open "Field Assignments" list
4. You should see assignments!

## 🆘 Troubleshooting

### If Test Connection Fails:

1. Wait 5 minutes (Azure needs time)
2. Double-check all IDs are correct
3. Verify "Grant admin consent" was clicked
4. Check firewall/proxy settings

### If Lists Don't Sync:

1. Ensure "Enable automatic synchronization" is checked
2. Check Java console for errors
3. Verify lists exist in Microsoft Lists
4. Try manual sync (coming in next update)

## 📞 Getting Help

### Documentation Files:

- **For Beginners**: `AZURE_SHAREPOINT_SETUP_FOR_BEGINNERS.md`
- **Technical Details**: `SHAREPOINT_MODERN_INTEGRATION_GUIDE.md`
- **Original Plan**: `SHAREPOINT_INTEGRATION_PLAN.md`

### Scripts:

- **Setup**: `setup-sharepoint.bat`
- **Verify**: `verify-sharepoint-setup.bat`
- **PowerShell**: `Setup-MicrosoftLists.ps1`

### Where to Get Your IDs:

- **Tenant ID**: Azure Portal → Azure Active Directory → Overview
- **Client ID**: Azure Portal → App registrations → Your app → Overview
- **Client Secret**: You saved this when you created it (can't view again)

---

*Remember: This is a one-time setup. Once it's working, it runs automatically!*