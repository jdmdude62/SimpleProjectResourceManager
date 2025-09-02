@echo off
cd C:\Users\mmiller\IdeaProjects\SimpleProjectResourceManager
echo Loading Federal Holidays...
java -cp "target\classes;C:\Users\mmiller\.m2\repository\org\xerial\sqlite-jdbc\3.42.0.0\sqlite-jdbc-3.42.0.0.jar;C:\Users\mmiller\.m2\repository\com\zaxxer\HikariCP\5.0.1\HikariCP-5.0.1.jar;C:\Users\mmiller\.m2\repository\org\slf4j\slf4j-api\2.0.7\slf4j-api-2.0.7.jar;C:\Users\mmiller\.m2\repository\org\slf4j\slf4j-simple\2.0.7\slf4j-simple-2.0.7.jar" -Djava.awt.headless=true -Djavafx.headless=true com.subliminalsearch.simpleprojectresourcemanager.util.LoadFederalHolidays 2025
echo.
echo Done!
pause