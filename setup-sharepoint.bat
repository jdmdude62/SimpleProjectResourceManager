@echo off
echo ============================================
echo SharePoint/Microsoft Lists Setup
echo ============================================
echo.
echo This will set up Microsoft Lists for the
echo Simple Project Resource Manager application.
echo.
echo Prerequisites:
echo - You must be a SharePoint admin
echo - You need a SharePoint site created
echo - Internet connection required
echo.
pause

echo.
echo Starting PowerShell setup script...
echo.

REM Run PowerShell script with execution policy bypass for this session only
powershell.exe -ExecutionPolicy Bypass -File "Setup-MicrosoftLists.ps1"

echo.
echo ============================================
echo Setup process complete!
echo ============================================
echo.
echo If successful, you can now:
echo 1. Configure domain logins in the Java app
echo 2. Set up SharePoint integration settings
echo 3. Test the connection
echo.
pause