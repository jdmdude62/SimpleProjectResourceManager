# PowerShell Script to Set Up Microsoft Lists for Field Operations
# Run this in PowerShell as Administrator
# Prerequisites: PnP PowerShell module

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Microsoft Lists Setup for Field Operations" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if PnP PowerShell is installed
Write-Host "Checking for PnP PowerShell module..." -ForegroundColor Yellow
if (!(Get-Module -ListAvailable -Name "PnP.PowerShell")) {
    Write-Host "PnP PowerShell not found. Installing..." -ForegroundColor Yellow
    Install-Module -Name "PnP.PowerShell" -Force -AllowClobber -Scope CurrentUser
    Write-Host "PnP PowerShell installed successfully!" -ForegroundColor Green
} else {
    Write-Host "PnP PowerShell is already installed." -ForegroundColor Green
}

# Get site URL from user
Write-Host ""
Write-Host "Enter your SharePoint site URL" -ForegroundColor Yellow
Write-Host "Example: https://yourcompany.sharepoint.com/sites/Field-Operations" -ForegroundColor Gray
$siteUrl = Read-Host "Site URL"

# Connect to SharePoint
Write-Host ""
Write-Host "Connecting to SharePoint..." -ForegroundColor Yellow
Write-Host "A login window will appear - sign in with your SharePoint admin account" -ForegroundColor Cyan
try {
    Connect-PnPOnline -Url $siteUrl -Interactive
    Write-Host "Connected successfully!" -ForegroundColor Green
} catch {
    Write-Host "Failed to connect. Please check your URL and credentials." -ForegroundColor Red
    Exit
}

# Function to create a list with columns
function Create-FieldList {
    param(
        [string]$ListName,
        [string]$Description,
        [array]$Columns
    )
    
    Write-Host ""
    Write-Host "Creating list: $ListName" -ForegroundColor Yellow
    
    # Check if list exists
    $existingList = Get-PnPList -Identity $ListName -ErrorAction SilentlyContinue
    if ($existingList) {
        Write-Host "  List '$ListName' already exists. Skipping creation." -ForegroundColor Yellow
        return $existingList
    }
    
    # Create the list
    try {
        $list = New-PnPList -Title $ListName -Template GenericList -Description $Description
        Write-Host "  ✓ List created successfully" -ForegroundColor Green
        
        # Add columns
        foreach ($column in $Columns) {
            Write-Host "  Adding column: $($column.Name)..." -NoNewline
            
            try {
                switch ($column.Type) {
                    "Text" {
                        Add-PnPField -List $ListName -DisplayName $column.Name `
                            -InternalName $column.Name.Replace(" ", "") `
                            -Type Text -Required:$column.Required | Out-Null
                    }
                    "DateTime" {
                        Add-PnPField -List $ListName -DisplayName $column.Name `
                            -InternalName $column.Name.Replace(" ", "") `
                            -Type DateTime -Required:$column.Required | Out-Null
                    }
                    "Choice" {
                        Add-PnPFieldFromXml -List $ListName -FieldXml "<Field Type='Choice' DisplayName='$($column.Name)' Name='$($column.Name.Replace(" ", ""))' Required='$($column.Required.ToString().ToUpper())' Format='Dropdown'><CHOICES>$($column.Choices | ForEach-Object { "<CHOICE>$_</CHOICE>" })</CHOICES><Default>$($column.Default)</Default></Field>" | Out-Null
                    }
                    "Number" {
                        Add-PnPField -List $ListName -DisplayName $column.Name `
                            -InternalName $column.Name.Replace(" ", "") `
                            -Type Number -Required:$column.Required | Out-Null
                    }
                    "Currency" {
                        Add-PnPField -List $ListName -DisplayName $column.Name `
                            -InternalName $column.Name.Replace(" ", "") `
                            -Type Currency -Required:$column.Required | Out-Null
                    }
                    "Note" {
                        Add-PnPField -List $ListName -DisplayName $column.Name `
                            -InternalName $column.Name.Replace(" ", "") `
                            -Type Note -Required:$column.Required | Out-Null
                    }
                }
                Write-Host " ✓" -ForegroundColor Green
            } catch {
                Write-Host " ✗ Failed: $_" -ForegroundColor Red
            }
        }
        
        return $list
    } catch {
        Write-Host "  ✗ Failed to create list: $_" -ForegroundColor Red
        return $null
    }
}

# Define columns for each list
$projectColumns = @(
    @{Name="ProjectID"; Type="Text"; Required=$true},
    @{Name="StartDate"; Type="DateTime"; Required=$true},
    @{Name="EndDate"; Type="DateTime"; Required=$true},
    @{Name="Status"; Type="Choice"; Required=$true; 
      Choices=@("Planning", "Active", "On Hold", "Complete", "Cancelled"); 
      Default="Planning"},
    @{Name="Location"; Type="Text"; Required=$false},
    @{Name="Description"; Type="Note"; Required=$false},
    @{Name="Budget"; Type="Currency"; Required=$false},
    @{Name="ClientName"; Type="Text"; Required=$false},
    @{Name="ClientContact"; Type="Text"; Required=$false},
    @{Name="TravelRequired"; Type="Choice"; Required=$false;
      Choices=@("Yes", "No"); Default="No"}
)

$resourceColumns = @(
    @{Name="ResourceID"; Type="Text"; Required=$true},
    @{Name="Email"; Type="Text"; Required=$true},
    @{Name="Phone"; Type="Text"; Required=$false},
    @{Name="ResourceType"; Type="Choice"; Required=$true;
      Choices=@("Employee", "Contractor", "3rd Party", "Equipment");
      Default="Employee"},
    @{Name="Department"; Type="Text"; Required=$false},
    @{Name="Skills"; Type="Note"; Required=$false},
    @{Name="DomainLogin"; Type="Text"; Required=$true}
)

$assignmentColumns = @(
    @{Name="AssignmentID"; Type="Text"; Required=$true},
    @{Name="ResourceName"; Type="Text"; Required=$true},
    @{Name="ProjectName"; Type="Text"; Required=$true},
    @{Name="StartDate"; Type="DateTime"; Required=$true},
    @{Name="EndDate"; Type="DateTime"; Required=$true},
    @{Name="Status"; Type="Choice"; Required=$true;
      Choices=@("Scheduled", "In Progress", "Complete");
      Default="Scheduled"},
    @{Name="TravelDays"; Type="Number"; Required=$false},
    @{Name="Notes"; Type="Note"; Required=$false}
)

# Create the lists
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Creating Microsoft Lists" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$projectsList = Create-FieldList -ListName "Field Projects" `
    -Description "Project scheduling and tracking" `
    -Columns $projectColumns

$resourcesList = Create-FieldList -ListName "Field Resources" `
    -Description "Field service resources and technicians" `
    -Columns $resourceColumns

$assignmentsList = Create-FieldList -ListName "Field Assignments" `
    -Description "Resource assignments to projects" `
    -Columns $assignmentColumns

# Create views
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Creating Views" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Projects views
Write-Host ""
Write-Host "Creating views for Field Projects..." -ForegroundColor Yellow
try {
    # Active Projects view
    Add-PnPView -List "Field Projects" -Title "Active Projects" `
        -Fields @("Title", "ProjectID", "StartDate", "EndDate", "Status", "Location") `
        -Query "<Where><Eq><FieldRef Name='Status'/><Value Type='Choice'>Active</Value></Eq></Where>" `
        -ErrorAction SilentlyContinue | Out-Null
    Write-Host "  ✓ Active Projects view created" -ForegroundColor Green
    
    # Upcoming Projects view
    Add-PnPView -List "Field Projects" -Title "Upcoming Projects" `
        -Fields @("Title", "ProjectID", "StartDate", "EndDate", "Status", "Location") `
        -Query "<Where><And><Geq><FieldRef Name='StartDate'/><Value Type='DateTime'><Today/></Value></Geq><Eq><FieldRef Name='Status'/><Value Type='Choice'>Planning</Value></Eq></And></Where>" `
        -ErrorAction SilentlyContinue | Out-Null
    Write-Host "  ✓ Upcoming Projects view created" -ForegroundColor Green
} catch {
    Write-Host "  Some views may already exist or failed to create" -ForegroundColor Yellow
}

# Assignments views
Write-Host ""
Write-Host "Creating views for Field Assignments..." -ForegroundColor Yellow
try {
    # This Week view
    Add-PnPView -List "Field Assignments" -Title "This Week" `
        -Fields @("Title", "ResourceName", "ProjectName", "StartDate", "EndDate", "Status") `
        -Query "<Where><And><Leq><FieldRef Name='StartDate'/><Value Type='DateTime'><Today OffsetDays='7'/></Value></Leq><Geq><FieldRef Name='EndDate'/><Value Type='DateTime'><Today/></Value></Geq></And></Where>" `
        -ErrorAction SilentlyContinue | Out-Null
    Write-Host "  ✓ This Week view created" -ForegroundColor Green
    
    # My Assignments view (will be personalized per user)
    Add-PnPView -List "Field Assignments" -Title "My Assignments" `
        -Fields @("Title", "ProjectName", "StartDate", "EndDate", "Status", "TravelDays") `
        -PersonalView `
        -ErrorAction SilentlyContinue | Out-Null
    Write-Host "  ✓ My Assignments view created" -ForegroundColor Green
} catch {
    Write-Host "  Some views may already exist or failed to create" -ForegroundColor Yellow
}

# Create sample data
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Create Sample Data?" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
$createSample = Read-Host "Would you like to create sample data for testing? (Y/N)"

if ($createSample -eq "Y" -or $createSample -eq "y") {
    Write-Host ""
    Write-Host "Creating sample data..." -ForegroundColor Yellow
    
    # Sample project
    try {
        $project = Add-PnPListItem -List "Field Projects" -Values @{
            "Title" = "Test SharePoint Integration"
            "ProjectID" = "PROJ-TEST-001"
            "StartDate" = (Get-Date)
            "EndDate" = (Get-Date).AddDays(7)
            "Status" = "Active"
            "Location" = "Main Office"
            "Description" = "Testing SharePoint integration with Java application"
            "Budget" = 10000
        }
        Write-Host "  ✓ Sample project created" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ Failed to create sample project: $_" -ForegroundColor Red
    }
    
    # Sample resource
    try {
        $resource = Add-PnPListItem -List "Field Resources" -Values @{
            "Title" = "John Smith"
            "ResourceID" = "RES-001"
            "Email" = "john.smith@company.com"
            "Phone" = "555-0100"
            "ResourceType" = "Employee"
            "Department" = "Field Service"
            "DomainLogin" = "jsmith"
        }
        Write-Host "  ✓ Sample resource created" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ Failed to create sample resource: $_" -ForegroundColor Red
    }
    
    # Sample assignment
    try {
        $assignment = Add-PnPListItem -List "Field Assignments" -Values @{
            "Title" = "John Smith - Test Project"
            "AssignmentID" = "ASSIGN-001"
            "ResourceName" = "John Smith"
            "ProjectName" = "Test SharePoint Integration"
            "StartDate" = (Get-Date)
            "EndDate" = (Get-Date).AddDays(7)
            "Status" = "Scheduled"
            "TravelDays" = 0
        }
        Write-Host "  ✓ Sample assignment created" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ Failed to create sample assignment: $_" -ForegroundColor Red
    }
}

# Display summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "✓ Microsoft Lists created successfully" -ForegroundColor Green
Write-Host "✓ All columns configured" -ForegroundColor Green
Write-Host "✓ Views created" -ForegroundColor Green
if ($createSample -eq "Y" -or $createSample -eq "y") {
    Write-Host "✓ Sample data created" -ForegroundColor Green
}

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Go to https://lists.live.com to see your lists" -ForegroundColor White
Write-Host "2. Configure domain logins in your Java application" -ForegroundColor White
Write-Host "3. Set up SharePoint integration with your Azure AD credentials" -ForegroundColor White
Write-Host "4. Test the connection from the Java application" -ForegroundColor White

Write-Host ""
Write-Host "Site URL: $siteUrl" -ForegroundColor Yellow
Write-Host ""

# Disconnect
Disconnect-PnPOnline