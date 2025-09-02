@echo off
echo ========================================
echo Testing Calendar Sync
echo ========================================
echo.

echo Compiling test...
javac -cp "target/classes;lib/*" TestCalendarSync.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Compilation failed. Make sure you've run: mvn compile
    pause
    exit /b 1
)

echo Running calendar sync test...
echo.

java -cp ".;target/classes;lib/*" com.subliminalsearch.simpleprojectresourcemanager.TestCalendarSync

echo.
pause