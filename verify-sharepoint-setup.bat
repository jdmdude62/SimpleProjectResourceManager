@echo off
echo ============================================
echo SharePoint Setup Verification
echo ============================================
echo.
echo This will verify your SharePoint setup is
echo working correctly with the Java application.
echo.
echo Make sure you have:
echo [X] Created Azure AD app registration
echo [X] Set up Microsoft Lists
echo [X] Configured the Java application
echo.
pause

echo.
echo Step 1: Checking Java configuration files...
echo.

if exist "%USERPROFILE%\.SimpleProjectResourceManager\sharepoint.properties" (
    echo [OK] SharePoint configuration file found
    echo.
    echo Configuration details:
    type "%USERPROFILE%\.SimpleProjectResourceManager\sharepoint.properties" | findstr "enabled"
    type "%USERPROFILE%\.SimpleProjectResourceManager\sharepoint.properties" | findstr "site"
    echo.
) else (
    echo [MISSING] SharePoint configuration not found!
    echo.
    echo Please run the Java application and configure:
    echo   Tools -^> SharePoint Integration...
    echo.
)

echo ============================================
echo Step 2: Testing with Java
echo ============================================
echo.
echo Compiling test class...

javac -cp "target/classes;lib/*" TestSharePointConnection.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed!
    echo.
    echo Make sure you have run: mvn compile
    echo.
    pause
    exit /b 1
)

echo.
echo Running connection test...
echo.

java -cp ".;target/classes;lib/*" com.subliminalsearch.simpleprojectresourcemanager.TestSharePointConnection

echo.
echo ============================================
echo Verification Complete
echo ============================================
echo.
echo If the test passed, you're ready to:
echo 1. Start syncing data
echo 2. Test mobile access
echo 3. Set up notifications
echo.
pause