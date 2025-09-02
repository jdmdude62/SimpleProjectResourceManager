# Simple Azure AD App Registration Script
# No special characters, no issues!

Write-Host "======================================"
Write-Host "Azure AD App Registration Setup"
Write-Host "======================================"
Write-Host ""

# Check if Azure AD module is installed
if (!(Get-Module -ListAvailable -Name AzureAD)) {
    Write-Host "Installing AzureAD PowerShell module..."
    Install-Module -Name AzureAD -Force -Scope CurrentUser -AllowClobber
    Write-Host "Module installed!"
}

Import-Module AzureAD

# Connect to Azure AD
Write-Host "Connecting to Azure AD..."
Write-Host "Please sign in with your SharePoint admin account"
Write-Host ""

try {
    Connect-AzureAD
    Write-Host "Connected successfully!"
} catch {
    Write-Host "Connection failed. Error: $_"
    exit 1
}

Write-Host ""
Write-Host "Creating app registration..."

# Create the app
try {
    $appName = "Simple Project Resource Manager"
    
    # Check if app already exists
    $existingApp = Get-AzureADApplication -Filter "DisplayName eq '$appName'"
    
    if ($existingApp) {
        Write-Host "App already exists! Using existing app."
        $app = $existingApp[0]
    } else {
        # Create new app
        $app = New-AzureADApplication `
            -DisplayName $appName `
            -IdentifierUris "https://localhost/projectmanager"
        Write-Host "App created successfully!"
    }
    
    Write-Host ""
    Write-Host "======================================" 
    Write-Host "YOUR APP DETAILS - SAVE THESE!" 
    Write-Host "======================================"
    Write-Host ""
    Write-Host "Application ID: $($app.AppId)"
    Write-Host "Object ID: $($app.ObjectId)"
    
    # Get Tenant ID
    $tenant = Get-AzureADTenantDetail
    Write-Host "Tenant ID: $($tenant.ObjectId)"
    
} catch {
    Write-Host "Error creating app: $_"
    exit 1
}

# Create or get client secret
Write-Host ""
Write-Host "Creating client secret..."

try {
    # Check for existing secrets
    $existingSecrets = Get-AzureADApplicationPasswordCredential -ObjectId $app.ObjectId
    
    if ($existingSecrets) {
        Write-Host "Note: App has existing secrets. Creating additional secret."
    }
    
    $startDate = Get-Date
    $endDate = $startDate.AddYears(2)
    
    $secret = New-AzureADApplicationPasswordCredential `
        -ObjectId $app.ObjectId `
        -CustomKeyIdentifier "JavaApp" `
        -StartDate $startDate `
        -EndDate $endDate
    
    Write-Host ""
    Write-Host "======================================"
    Write-Host "IMPORTANT - COPY THIS SECRET NOW!"
    Write-Host "You will NEVER see it again!"
    Write-Host "======================================"
    Write-Host ""
    Write-Host "CLIENT SECRET: $($secret.Value)"
    Write-Host ""
    Write-Host "Secret expires: $($endDate.ToString('yyyy-MM-dd'))"
    
} catch {
    Write-Host "Error creating secret: $_"
    Write-Host "You may need to create the secret manually in Azure Portal"
}

# Save to file
Write-Host ""
Write-Host "Saving configuration to file..."

$configContent = @"
Azure AD Configuration
======================
Date Created: $(Get-Date)

COPY THESE THREE VALUES:

Tenant ID: $($tenant.ObjectId)
Client ID: $($app.AppId)  
Client Secret: $($secret.Value)

Secret Expires: $($endDate.ToString('yyyy-MM-dd'))

Next Steps:
1. Open your Java application
2. Go to Tools menu -> SharePoint Integration
3. Paste these three values
4. Click Test Connection
"@

$configContent | Out-File -FilePath "azure-config.txt" -Encoding UTF8
Write-Host "Configuration saved to: azure-config.txt"

Write-Host ""
Write-Host "======================================"
Write-Host "SETUP COMPLETE!"
Write-Host "======================================"
Write-Host ""
Write-Host "IMPORTANT - You still need to:"
Write-Host ""
Write-Host "1. Grant admin consent in Azure Portal:"
Write-Host "   https://portal.azure.com"
Write-Host "   -> App registrations"
Write-Host "   -> Simple Project Resource Manager"  
Write-Host "   -> API permissions"
Write-Host "   -> Grant admin consent"
Write-Host ""
Write-Host "2. Configure the Java application with the three values above"
Write-Host ""

# Disconnect
Disconnect-AzureAD -Confirm:$false

Write-Host "Done!"