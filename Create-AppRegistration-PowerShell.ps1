# Create Azure AD App Registration via PowerShell
# No Azure Portal needed, no credit card risk!

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Azure AD App Registration via PowerShell" -ForegroundColor Cyan
Write-Host "No Portal, No Credit Card Required!" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Install Azure AD module if needed
if (!(Get-Module -ListAvailable -Name AzureAD)) {
    Write-Host "Installing AzureAD PowerShell module..." -ForegroundColor Yellow
    Install-Module -Name AzureAD -Force -Scope CurrentUser
}

# Connect to Azure AD
Write-Host "Connecting to Azure AD..." -ForegroundColor Yellow
Write-Host "Sign in with your SharePoint admin account" -ForegroundColor Cyan
Connect-AzureAD

# Create the app registration
Write-Host ""
Write-Host "Creating app registration..." -ForegroundColor Yellow

$app = New-AzureADApplication `
    -DisplayName "Simple Project Resource Manager" `
    -IdentifierUris "https://localhost/projectmanager" `
    -ReplyUrls @()

Write-Host "✓ App created successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Your App Details:" -ForegroundColor Cyan
Write-Host "=================" -ForegroundColor Cyan
Write-Host "Application ID: $($app.AppId)" -ForegroundColor Yellow
Write-Host "Object ID: $($app.ObjectId)" -ForegroundColor Yellow

# Get Tenant ID
$tenant = Get-AzureADTenantDetail
Write-Host "Tenant ID: $($tenant.ObjectId)" -ForegroundColor Yellow

# Create client secret
Write-Host ""
Write-Host "Creating client secret..." -ForegroundColor Yellow

$startDate = Get-Date
$endDate = $startDate.AddYears(2)
$secret = New-AzureADApplicationPasswordCredential `
    -ObjectId $app.ObjectId `
    -CustomKeyIdentifier "JavaAppSecret" `
    -StartDate $startDate `
    -EndDate $endDate

Write-Host "✓ Secret created successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "IMPORTANT - SAVE THIS SECRET NOW:" -ForegroundColor Red
Write-Host "=================================" -ForegroundColor Red
Write-Host $secret.Value -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Red
Write-Host ""

# Add Microsoft Graph permissions
Write-Host "Adding Microsoft Graph permissions..." -ForegroundColor Yellow

# Get Microsoft Graph Service Principal
$graphSP = Get-AzureADServicePrincipal -Filter "DisplayName eq 'Microsoft Graph'"

# Define required permissions
$permissions = @(
    @{
        Id = "798ee544-9d2d-430c-a058-570e29e34338"  # Calendars.ReadWrite
        Type = "Role"
    },
    @{
        Id = "b633e1c5-b582-4048-a93e-9f11b44c7e96"  # Mail.Send
        Type = "Role"
    },
    @{
        Id = "df021288-bdef-4463-88db-98f22de89214"  # User.Read.All
        Type = "Role"
    }
)

# Create resource access object
$resourceAccess = @()
foreach ($permission in $permissions) {
    $resourceAccess += [Microsoft.Open.AzureAD.Model.ResourceAccess]@{
        Id = $permission.Id
        Type = $permission.Type
    }
}

$requiredResourceAccess = [Microsoft.Open.AzureAD.Model.RequiredResourceAccess]@{
    ResourceAppId = $graphSP.AppId
    ResourceAccess = $resourceAccess
}

# Update app with permissions
Set-AzureADApplication `
    -ObjectId $app.ObjectId `
    -RequiredResourceAccess $requiredResourceAccess

Write-Host "✓ Permissions added!" -ForegroundColor Green

# Create service principal
Write-Host ""
Write-Host "Creating service principal..." -ForegroundColor Yellow
$sp = New-AzureADServicePrincipal -AppId $app.AppId
Write-Host "✓ Service principal created!" -ForegroundColor Green

# Save configuration to file
Write-Host ""
Write-Host "Saving configuration..." -ForegroundColor Yellow

$config = @"
# Azure AD Configuration for Simple Project Resource Manager
# Created: $(Get-Date)

TENANT_ID=$($tenant.ObjectId)
CLIENT_ID=$($app.AppId)
CLIENT_SECRET=$($secret.Value)

# These values expire on: $($endDate.ToString('yyyy-MM-dd'))
"@

$config | Out-File -FilePath "azure-config.txt" -Encoding UTF8
Write-Host "✓ Configuration saved to azure-config.txt" -ForegroundColor Green

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "Setup Complete - NO CREDIT CARD USED!" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Copy the values from azure-config.txt" -ForegroundColor White
Write-Host "2. Open your Java application" -ForegroundColor White
Write-Host "3. Go to Tools -> SharePoint Integration" -ForegroundColor White
Write-Host "4. Paste the three values" -ForegroundColor White
Write-Host "5. Test the connection" -ForegroundColor White
Write-Host ""
Write-Host "Note: Admin consent still needs to be granted in the portal" -ForegroundColor Yellow
Write-Host "Direct link: https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade" -ForegroundColor Yellow

# Disconnect
Disconnect-AzureAD