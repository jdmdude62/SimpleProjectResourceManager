@echo off
echo ========================================
echo SharePoint Sync Test
echo ========================================
echo.

echo Compiling test class...
javac -cp "target/classes;target/lib/*" TestSync.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed
    pause
    exit /b 1
)

echo Running sync test...
echo.

java -cp ".;target/classes;target/lib/*" TestSync

echo.
pause