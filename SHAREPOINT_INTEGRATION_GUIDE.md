# SharePoint Integration Guide - Simple Export

## Overview
This guide explains how to export assignment data from the Simple Project Resource Manager to SharePoint, allowing technicians to view their schedules through SharePoint calendars.

## Quick Start

### Step 1: Export from Application
1. Open the Task List view for your project
2. Click the **📤 Export** button in the toolbar
3. Choose export format:
   - **CSV for SharePoint List Import** - Creates a single CSV file for import
   - **Individual Technician Schedules** - Creates separate CSV files per technician
4. Select save location
5. Click **Export**

### Step 2: Import to SharePoint
1. Open your SharePoint site
2. Navigate to your assignments list
3. Click **Settings** → **Import from spreadsheet**
4. Upload the exported CSV file
5. Map columns as needed

## Detailed SharePoint Setup

### Part 1: Create SharePoint Site

1. **Create Team Site**
   - Go to SharePoint home
   - Click **Create site** → **Team site**
   - Name: "Project Resource Manager"
   - Privacy: Private (adjust as needed)

2. **Set Basic Permissions**
   - Project Managers: Full Control
   - Technicians: Read/Contribute
   - Others: Read Only

### Part 2: Create Assignment List

1. **Create Custom List**
   ```
   Site Contents → New → List
   Name: "Field Assignments"
   ```

2. **Add Required Columns**
   | Column Name | Type | Required |
   |------------|------|----------|
   | Technician | Person or Group | Yes |
   | Project ID | Single line text | Yes |
   | Project Name | Single line text | Yes |
   | Task Description | Multiple lines text | No |
   | Start Date | Date and Time | Yes |
   | End Date | Date and Time | Yes |
   | Location | Single line text | No |
   | Status | Choice | Yes |
   | Materials Status | Choice | No |
   | PM Contact | Single line text | No |
   | Notes | Multiple lines text | No |

3. **Status Choice Values**
   - Scheduled
   - In Progress
   - Completed
   - Cancelled
   - On Hold

4. **Materials Status Values**
   - Pending
   - Ready
   - On Site
   - Not Required

### Part 3: Create Calendar Views

#### A. Master Calendar (All Assignments)
1. In Field Assignments list, click **Create View**
2. Choose **Calendar View**
3. Configure:
   - Name: "Master Schedule"
   - Time interval: Start Date to End Date
   - Month/Week/Day Group: Start Date
   - Title: Project ID

#### B. Individual Technician Views
1. Create View → Calendar View
2. Name: "My Schedule"
3. Filter: `[Me]` in Technician field
4. This shows only the current user's assignments

#### C. Filtered Views by Team
1. Create separate views for each team:
   - "Team Alpha Schedule"
   - "Team Bravo Schedule"
2. Filter by specific technician names

### Part 4: Export/Import Process

#### From Application to SharePoint

1. **Export Data**
   ```
   Application → Task List → Export Button
   Choose: CSV for SharePoint List Import
   Save to: Desktop or Documents
   ```

2. **Prepare CSV (if needed)**
   - Open in Excel
   - Verify date formats (MM/DD/YYYY)
   - Check technician names match SharePoint users
   - Save as CSV

3. **Import to SharePoint**
   
   **Method 1: Quick Edit**
   - Open Field Assignments list
   - Click **Quick edit**
   - Copy/paste from Excel
   - Click **Exit quick edit**

   **Method 2: Import Spreadsheet**
   - Site Contents → New → Import Spreadsheet
   - Browse to CSV file
   - Map columns to list columns
   - Import

### Part 5: Technician Access Options

#### Option 1: Direct SharePoint Access
1. Share site URL with technicians
2. They navigate to Field Assignments → My Schedule
3. Calendar shows only their assignments

#### Option 2: Outlook Integration
1. In SharePoint calendar, click **Connect to Outlook**
2. Calendar syncs to Outlook
3. Available on desktop and mobile

#### Option 3: Microsoft Teams
1. In Teams channel, click **+** tab
2. Add **SharePoint** → **Field Assignments**
3. Select calendar view
4. Team members see schedule in Teams

#### Option 4: SharePoint Mobile App
1. Install SharePoint mobile app
2. Navigate to site
3. Open Field Assignments list
4. Switch to calendar view

### Part 6: Maintaining Data

#### Regular Updates
1. **Daily/Weekly Export**
   - Export updated assignments
   - Import to SharePoint (overwrites existing)

2. **Incremental Updates**
   - Export only changed assignments
   - Update specific items in SharePoint

3. **Bulk Operations**
   - Use Quick Edit for multiple changes
   - Export all → Clear list → Import fresh

### Part 7: CSV File Format

The exported CSV contains these columns:
```csv
Technician,Project ID,Project Name,Task Description,Start Date,End Date,Location,Status,Materials Status,PM Contact,Notes
"John Smith","PRJ-2025-001","Oak Street Installation","Install doghouse","01/15/2025","01/17/2025","123 Oak St","Scheduled","Ready","PM-1","2-story deluxe model"
```

### Part 8: Troubleshooting

| Issue | Solution |
|-------|----------|
| Dates import incorrectly | Ensure MM/DD/YYYY format in CSV |
| Technician names don't match | Use exact SharePoint display names |
| Calendar view is empty | Check view filters and date range |
| Can't import CSV | Check file encoding (use UTF-8) |
| Permissions error | Ensure users have Contribute rights |

### Part 9: Best Practices

1. **Naming Conventions**
   - Use consistent Project IDs
   - Standardize technician names
   - Clear task descriptions

2. **Update Frequency**
   - Export daily for active projects
   - Weekly for planning purposes
   - After any major schedule changes

3. **Data Validation**
   - Verify dates before export
   - Check for assignment conflicts
   - Ensure all required fields populated

4. **Backup**
   - Keep copy of exported CSVs
   - Document import dates
   - Maintain change log

### Part 10: PowerShell Automation (Optional)

#### Automated Import Script
```powershell
# Connect to SharePoint
Connect-PnPOnline -Url "https://yoursite.sharepoint.com/sites/ProjectRM"

# Read CSV
$assignments = Import-Csv "C:\Exports\assignments.csv"

# Import to list
foreach ($item in $assignments) {
    Add-PnPListItem -List "Field Assignments" -Values @{
        "Title" = $item.'Project ID'
        "Technician" = $item.Technician
        "ProjectName" = $item.'Project Name'
        "StartDate" = [DateTime]$item.'Start Date'
        "EndDate" = [DateTime]$item.'End Date'
        "Status" = $item.Status
    }
}
```

#### Scheduled Export Task
Create Windows Task Scheduler job:
1. Trigger: Daily at 6 AM
2. Action: Run PowerShell script
3. Script exports and emails CSV

### Part 11: Email Notifications (Optional)

Configure SharePoint alerts:
1. Field Assignments → Alert Me
2. Choose:
   - When: Items are added/changed
   - Filter: Assigned to me
   - Frequency: Daily summary
3. Technicians receive email updates

### Appendix A: Sample Files

Sample CSV and import templates available in:
- `/docs/sharepoint-templates/`
- Includes sample data
- Column mapping guide
- Import instructions

### Appendix B: Quick Reference Card

**For Project Managers:**
1. Export: Task List → 📤 Export → CSV
2. Import: SharePoint → Quick Edit → Paste
3. Verify: Check calendar views

**For Technicians:**
1. Access: SharePoint site → Field Assignments
2. View: My Schedule (calendar)
3. Sync: Connect to Outlook (optional)

### Support

For issues or questions:
- Application Support: Check application logs
- SharePoint Support: Contact SharePoint admin
- Import/Export Help: See troubleshooting section

---

**Version:** 2.0 (Simplified - No LDAP Required)  
**Last Updated:** Today  
**Next Review:** As needed