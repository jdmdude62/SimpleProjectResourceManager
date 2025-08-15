@echo off
echo Setting up Java environment...

rem Try to find Java installation
for /d %%i in ("C:\Program Files\Java\jdk*") do set JAVA_HOME=%%i
if not defined JAVA_HOME (
    for /d %%i in ("C:\Program Files\OpenJDK\jdk*") do set JAVA_HOME=%%i
)
if not defined JAVA_HOME (
    for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk*") do set JAVA_HOME=%%i
)

if not defined JAVA_HOME (
    echo Java JDK not found in standard locations.
    echo Please install Java 17 JDK from: https://adoptium.net/
    pause
    exit /b 1
)

echo Found Java at: %JAVA_HOME%
echo.
echo Running application...
mvnw.cmd javafx:run
pause