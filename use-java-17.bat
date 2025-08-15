@echo off
REM ============================================
REM Set Java 17 for this project
REM ============================================
REM 
REM Update the path below to match your Java 17 installation:
REM Common locations:
REM   C:\Program Files\Java\jdk-17
REM   C:\Program Files\Java\jdk-17.0.x
REM   C:\Program Files\Microsoft\jdk-17.x.x.x
REM   C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x

set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo ========================================
echo Java Environment Set
echo ========================================
echo JAVA_HOME: %JAVA_HOME%
echo.

java -version
echo.
javac -version
echo.

echo You can now run Maven commands with Java 17
echo Example: mvn clean test
echo.