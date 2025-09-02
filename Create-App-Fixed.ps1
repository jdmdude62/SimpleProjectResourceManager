# Fixed Azure AD App Registration Script
Write-Host "======================================"
Write-Host "Azure AD App Registration - Fixed"
Write-Host "======================================"
Write-Host ""

# Connect to Azure AD (you're already connected)
$connected = $false
try {
    Get-AzureADTenantDetail -ErrorAction Stop | Out-Null
    Write-Host "Already connected to Azure AD"
    $connected = $true
} catch {
    Write-Host "Connecting to Azure AD..."
    Connect-AzureAD
    $connected = $true
}

if ($connected) {
    Write-Host ""
    Write-Host "Creating app registration..."
    
    # Create app WITHOUT the problematic IdentifierUris
    try {
        $appName = "Simple Project Resource Manager"
        
        # Check if app already exists
        $existingApp = Get-AzureADApplication -Filter "DisplayName eq '$appName'"
        
        if ($existingApp) {
            Write-Host "App already exists! Using existing app."
            $app = $existingApp[0]
        } else {
            # Create new app without IdentifierUris
            $app = New-AzureADApplication -DisplayName $appName
            Write-Host "App created successfully!"
        }
        
        Write-Host ""
        Write-Host "======================================"
        Write-Host "SAVE THESE VALUES!"
        Write-Host "======================================"
        Write-Host ""
        
        # Get Tenant ID
        $tenant = Get-AzureADTenantDetail
        
        Write-Host "Tenant ID:     $($tenant.ObjectId)"
        Write-Host "Client ID:     $($app.AppId)"
        Write-Host ""
        
        # Create client secret
        Write-Host "Creating client secret..."
        $startDate = Get-Date
        $endDate = $startDate.AddYears(2)
        
        # Check if we need a new secret
        $createNewSecret = $true
        if ($existingApp) {
            $response = Read-Host "App already exists. Create new secret? (Y/N)"
            if ($response -ne "Y" -and $response -ne "y") {
                $createNewSecret = $false
            }
        }
        
        if ($createNewSecret) {
            try {
                $secret = New-AzureADApplicationPasswordCredential `
                    -ObjectId $app.ObjectId `
                    -CustomKeyIdentifier "JavaApp$(Get-Date -Format 'yyyyMMdd')" `
                    -StartDate $startDate `
                    -EndDate $endDate
                
                Write-Host ""
                Write-Host "======================================"
                Write-Host "CLIENT SECRET - COPY NOW!"
                Write-Host "======================================"
                Write-Host ""
                Write-Host "$($secret.Value)"
                Write-Host ""
                Write-Host "Expires: $($endDate.ToString('yyyy-MM-dd'))"
                
                # Save to file
                $config = @"
==========================================
Azure AD Configuration
Created: $(Get-Date)
==========================================

COPY THESE THREE VALUES TO YOUR JAVA APP:

Tenant ID:     $($tenant.ObjectId)
Client ID:     $($app.AppId)
Client Secret: $($secret.Value)

Secret Expires: $($endDate.ToString('yyyy-MM-dd'))

==========================================
NEXT STEPS:
1. Copy these values to the Java app
2. Grant admin consent in Azure Portal
==========================================
"@
                
                $config | Out-File -FilePath "azure-config-fixed.txt" -Encoding UTF8
                Write-Host "Configuration saved to: azure-config-fixed.txt"
                
            } catch {
                Write-Host "Error creating secret: $_"
            }
        } else {
            Write-Host "Skipped secret creation. Use existing secret or create one in portal."
            
            # Still save what we have
            $config = @"
==========================================
Azure AD Configuration
==========================================

Tenant ID:     $($tenant.ObjectId)
Client ID:     $($app.AppId)
Client Secret: [Use existing or create in portal]

==========================================
"@
            $config | Out-File -FilePath "azure-config-partial.txt" -Encoding UTF8
            Write-Host "Partial configuration saved to: azure-config-partial.txt"
        }
        
    } catch {
        Write-Host "Error: $_"
    }
    
    Write-Host ""
    Write-Host "======================================"
    Write-Host "IMPORTANT FINAL STEP!"
    Write-Host "======================================"
    Write-Host ""
    Write-Host "You MUST grant admin consent:"
    Write-Host "1. Go to: https://portal.azure.com"
    Write-Host "2. Search for 'App registrations'"
    Write-Host "3. Click on 'Simple Project Resource Manager'"
    Write-Host "4. Click 'API permissions' in left menu"
    Write-Host "5. Add permission -> Microsoft Graph -> Application permissions"
    Write-Host "6. Add: Calendars.ReadWrite, Mail.Send, User.Read.All"
    Write-Host "7. Click 'Grant admin consent for captechno.com'"
    Write-Host ""
}

Write-Host "Done!"