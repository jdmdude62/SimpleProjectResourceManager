@echo off
echo ========================================
echo SharePoint Connection Test
echo ========================================
echo.

REM Compile the test class
echo Compiling test class...
javac -cp "target/classes;lib/*" TestSharePointConnection.java
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to compile test class
    echo Make sure you have run 'mvn compile' first
    pause
    exit /b 1
)

echo.
echo Running SharePoint connection test...
echo.
java -cp ".;target/classes;lib/*" com.subliminalsearch.simpleprojectresourcemanager.TestSharePointConnection

echo.
pause