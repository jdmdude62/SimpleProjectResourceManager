@echo off
REM ============================================
REM Quick Report Viewer
REM ============================================

echo.
echo Test Report Locations:
echo ======================
echo.

if exist "target\surefire-reports\" (
    echo [1] Surefire Reports:
    echo     target\surefire-reports\
    dir target\surefire-reports\*.txt /b 2>nul | find /c ".txt" >temp.txt
    set /p count=<temp.txt
    echo     Found %count% test reports
    del temp.txt
    echo.
)

if exist "target\site\surefire-report.html" (
    echo [2] HTML Test Report:
    echo     target\site\surefire-report.html
    echo     Run: start target\site\surefire-report.html
    echo.
)

if exist "target\site\jacoco\index.html" (
    echo [3] Coverage Report:
    echo     target\site\jacoco\index.html
    echo     Run: start target\site\jacoco\index.html
    echo.
)

if exist "test-reports\" (
    echo [4] Custom Reports:
    dir test-reports\ /b /ad /o-d 2>nul | head -1 >temp.txt
    set /p latest=<temp.txt
    if not "%latest%"=="" (
        echo     Latest: test-reports\%latest%\
    )
    del temp.txt
    echo.
)

echo.
echo Commands to generate reports:
echo ==============================
echo mvn test                         - Run tests
echo mvn surefire-report:report       - Generate HTML test report
echo mvn test jacoco:report           - Generate coverage report
echo run-tests.bat                    - Run full test suite with reports
echo.

choice /c VX /n /m "Press [V] to view latest reports, [X] to exit: "
if errorlevel 2 goto :end
if errorlevel 1 goto :view

:view
if exist "target\site\surefire-report.html" (
    start target\site\surefire-report.html
)
if exist "target\site\jacoco\index.html" (
    start target\site\jacoco\index.html
)

:end