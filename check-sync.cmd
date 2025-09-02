@echo off
echo ========================================
echo SharePoint Sync Status Check
echo ========================================
echo.

echo Compiling sync checker...
javac -cp "target/classes" TriggerSharePointSync.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed
    pause
    exit /b 1
)

echo Running sync status check...
echo.

java -cp ".;target/classes" TriggerSharePointSync

echo.
echo ========================================
echo Next Steps:
echo ========================================
echo.
echo 1. Wait for automatic sync (check time above)
echo 2. Then check SharePoint at:
echo    https://captechno.sharepoint.com/sites/field-operations
echo    OR
echo    https://lists.live.com
echo.
echo 3. Look for these three lists:
echo    - Field Projects
echo    - Field Resources
echo    - Field Assignments
echo.
pause