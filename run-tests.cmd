@echo off
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
echo Running tests with Java from: %JAVA_HOME%
java -version
mvn test -Djacoco.skip=true %*