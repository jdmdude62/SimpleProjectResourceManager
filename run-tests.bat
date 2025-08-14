@echo off
REM ============================================
REM Automated Test Suite Runner
REM Generates developer and coverage reports
REM ============================================

echo.
echo ========================================
echo Simple Project Resource Manager
echo Automated Test Suite Execution
echo ========================================
echo.

REM Set timestamp for reports
set TIMESTAMP=%date:~-4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM Create reports directory
if not exist "test-reports" mkdir test-reports
if not exist "test-reports\%TIMESTAMP%" mkdir "test-reports\%TIMESTAMP%"

echo [1/4] Cleaning previous build...
call mvn clean

echo.
echo [2/4] Running unit tests with coverage...
call mvn test jacoco:report

echo.
echo [3/4] Generating HTML test report...
call mvn surefire-report:report

echo.
echo [4/4] Collecting all reports...
if exist target\surefire-reports xcopy /s /y target\surefire-reports\* "test-reports\%TIMESTAMP%\surefire\"
if exist target\site\jacoco xcopy /s /y target\site\jacoco\* "test-reports\%TIMESTAMP%\coverage\"

echo.
echo ========================================
echo Test Execution Complete!
echo ========================================
echo.
echo Reports available at:
echo   Test Results: test-reports\%TIMESTAMP%\surefire\
echo   Coverage:     test-reports\%TIMESTAMP%\coverage\index.html
echo.

REM Open coverage report in browser if it exists
if exist "test-reports\%TIMESTAMP%\coverage\index.html" (
    start "test-reports\%TIMESTAMP%\coverage\index.html"
)

echo Press any key to exit...
pause >nul