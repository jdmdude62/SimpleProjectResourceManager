@echo off
echo Running database migration to remove UNIQUE constraint from project_id...
cd C:\Users\mmiller\IdeaProjects\SimpleProjectResourceManager
java -cp "target\classes;C:\Users\mmiller\.m2\repository\org\xerial\sqlite-jdbc\3.42.0.0\sqlite-jdbc-3.42.0.0.jar;C:\Users\mmiller\.m2\repository\com\zaxxer\HikariCP\5.0.1\HikariCP-5.0.1.jar;C:\Users\mmiller\.m2\repository\org\slf4j\slf4j-api\2.0.7\slf4j-api-2.0.7.jar;C:\Users\mmiller\.m2\repository\org\slf4j\slf4j-simple\2.0.7\slf4j-simple-2.0.7.jar" com.subliminalsearch.simpleprojectresourcemanager.util.RunDatabaseMigration
echo.
echo Done!
pause