@echo off
REM ============================================
REM Quick Test Runner - Fast feedback loop
REM ============================================

echo.
echo Quick Test Execution
echo ====================
echo.

if "%1"=="" (
    echo Running all tests...
    mvn test
) else if "%1"=="financial" (
    echo Running financial tests...
    mvn test -Dtest="*Financial*Test"
) else if "%1"=="regression" (
    echo Running regression tests...
    mvn test -Dtest="*Regression*Test"
) else if "%1"=="repository" (
    echo Running repository tests...
    mvn test -Dtest="*Repository*Test"
) else if "%1"=="coverage" (
    echo Running tests with coverage...
    mvn clean test jacoco:report
    start target\site\jacoco\index.html
) else (
    echo Running specific test: %1
    mvn test -Dtest="%1"
)

echo.
echo Test execution complete!
echo.
pause