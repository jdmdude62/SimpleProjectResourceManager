@echo off
echo ========================================
echo SharePoint Sync Test Output Viewer
echo ========================================
echo.

if exist sharepoint-test-output (
    echo Test output directory found.
    echo.
    echo Latest test files:
    dir /b /o-d sharepoint-test-output\*.csv | head -5
    echo.
    
    echo ========================================
    echo Opening test output folder...
    echo ========================================
    start sharepoint-test-output
    
    echo.
    echo The CSV files contain:
    echo - Timestamp of sync
    echo - User name and email
    echo - Project details
    echo - Assignment dates
    echo - Location
    echo.
    echo You can open these in Excel to review.
    echo.
    echo TEST MODE IS ACTIVE - No events are being sent to users!
) else (
    echo No test output found yet.
    echo Run a sync first to generate test data.
)

echo.
pause