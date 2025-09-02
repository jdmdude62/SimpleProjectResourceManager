# Azure AD Setup - NO CREDIT CARD REQUIRED

## The Key: Use Your Existing Microsoft 365 Azure AD

You already have Azure AD through your Microsoft 365 subscription. Here's how to use it WITHOUT any credit card or billing setup.

## Step-by-Step Guide (No CC Required)

### 1. Access Azure AD Through Your Work Account

**DO NOT** go to azure.com and create a new account!

**INSTEAD**, use one of these URLs:

```
https://aad.portal.azure.com
```
or
```
https://portal.azure.com/#blade/Microsoft_AAD_IAM/ActiveDirectoryMenuBlade
```

Sign in with:
- Your work email (same as SharePoint admin)
- Your work password
- This uses your EXISTING company Azure AD

### 2. Go Directly to App Registrations

Skip all Azure subscription prompts!

Direct link:
```
https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade
```

Or navigate:
1. Azure Active Directory (in left menu)
2. App registrations
3. New registration

### 3. Create Your App Registration

Fill in:
- **Name**: Simple Project Resource Manager
- **Account types**: Single tenant (your organization only)
- **Redirect URI**: Leave blank

Click Register.

**Note**: No credit card prompt because you're using existing Azure AD!

### 4. Get Your IDs

Copy these immediately:
```
Application (client) ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
Directory (tenant) ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

### 5. Create Client Secret

1. Click "Certificates & secrets" (left menu)
2. Click "New client secret"
3. Description: Java App
4. Expires: 24 months
5. Click Add
6. **COPY THE VALUE IMMEDIATELY**

### 6. Add Permissions (Still Free)

1. Click "API permissions" (left menu)
2. Click "Add a permission"
3. Choose "Microsoft Graph"
4. Choose "Application permissions"
5. Add these FREE permissions:
   - `Calendars.ReadWrite` (for calendar sync)
   - `Mail.Send` (for email schedules)
   - `User.Read.All` (to get user info)
6. Click "Grant admin consent"

## What This Gives You (All Free)

✅ **Calendar Sync**
```java
// Push to Outlook calendars - FREE
graphClient.users(userEmail)
    .calendar()
    .events()
    .post(event);
```

✅ **Email Schedules**
```java
// Send schedule emails - FREE
graphClient.users("scheduler@company.com")
    .sendMail(message)
    .post();
```

✅ **Read User Info**
```java
// Get user details - FREE
User user = graphClient.users(userEmail)
    .buildRequest()
    .get();
```

## What to AVOID (These Require Credit Card)

❌ **Don't Create:**
- Azure Subscription
- Azure Free Account
- Pay-as-you-go account
- Azure resources (VMs, Storage, etc.)

❌ **Don't Click:**
- "Start free trial"
- "Upgrade account"
- "Add payment method"
- Any billing-related prompts

## If You See Credit Card Prompts

**You're in the wrong place!** This means:
1. You went to azure.com instead of portal.azure.com
2. You're trying to create a new account instead of using existing
3. You clicked on Azure services instead of just Azure AD

**Solution**: 
- Close that window
- Go to https://aad.portal.azure.com
- Sign in with work account
- You'll be in the right place (no CC required)

## The Simplest Integration (Calendar Only)

```java
public class SimpleCalendarSync {
    
    // No SharePoint, no Lists, just calendars
    public void addToUserCalendar(String userEmail, 
                                  String projectName, 
                                  LocalDate startDate, 
                                  LocalDate endDate, 
                                  String location) {
        
        Event event = new Event();
        event.subject = projectName;
        event.location = new Location();
        event.location.displayName = location;
        event.start = convertToDateTime(startDate);
        event.end = convertToDateTime(endDate);
        event.categories = Arrays.asList("Field Work");
        
        // This uses Exchange Online (part of M365)
        // No additional Azure services
        // No possibility of charges
        graphClient.users(userEmail)
            .events()
            .buildRequest()
            .post(event);
    }
}
```

## Cost Analysis

| What You're Using | Cost | Why |
|-------------------|------|-----|
| Azure AD (existing) | $0 | Included with M365 |
| App Registration | $0 | Free feature |
| Graph API | $0 | Included with M365 |
| Calendar Access | $0 | Part of Exchange |
| Email Sending | $0 | Part of Exchange |
| **Total Forever** | **$0** | **No CC needed!** |

## Verification Checklist

After setup, verify:
- [ ] You signed in with work account (not personal)
- [ ] You're in your company's Azure AD tenant
- [ ] No credit card was requested
- [ ] No subscription was created
- [ ] You only created an app registration
- [ ] You only added Graph permissions

## The Bottom Line

You're using Azure AD that comes FREE with Microsoft 365. This is like:
- Creating a service account in Active Directory
- Giving it permission to create calendar events
- No cloud resources, no billing, no charges possible

This is NOT:
- Creating Azure cloud services
- Using compute or storage
- Anything that could generate a bill

---

**Remember**: If it asks for a credit card, you're in the wrong place. Close and use the direct links above!