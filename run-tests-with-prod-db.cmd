@echo off
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Running tests with production database copy...
echo Using Java from: %JAVA_HOME%

set PROD_DB_PATH=%USERPROFILE%\.SimpleProjectResourceManager\scheduler.db
set TEST_DB_PATH=%CD%\test_production_copy.db

echo.
echo Production DB: %PROD_DB_PATH%
echo Test DB Copy: %TEST_DB_PATH%

if exist "%PROD_DB_PATH%" (
    echo Copying production database to test location...
    copy "%PROD_DB_PATH%" "%TEST_DB_PATH%" >nul
    if exist "%TEST_DB_PATH%" (
        echo Production database copied successfully
    ) else (
        echo ERROR: Failed to copy production database
        exit /b 1
    )
) else (
    echo WARNING: Production database not found at %PROD_DB_PATH%
    echo Creating empty test database...
    echo. > "%TEST_DB_PATH%"
)

echo.
echo Running tests with production data...
mvn test -Djacoco.skip=true -Dtest.db.url=jdbc:sqlite:%TEST_DB_PATH% %*

echo.
echo Cleaning up test database copy...
if exist "%TEST_DB_PATH%" del "%TEST_DB_PATH%"

echo Test run complete.