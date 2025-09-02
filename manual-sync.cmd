@echo off
echo ========================================
echo Manual SharePoint Sync Trigger
echo ========================================
echo.

echo Compiling trigger class...
javac -cp "target/classes" TriggerManualSync.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed
    pause
    exit /b 1
)

echo Running manual sync trigger...
echo.

java -cp ".;target/classes" TriggerManualSync

echo.
echo ========================================
echo Check the main application window for sync activity
echo ========================================
pause