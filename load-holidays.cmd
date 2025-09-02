@echo off
echo Loading Federal Holidays into Database...
echo.

mvn -q compile exec:java -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.util.LoadFederalHolidays" -Dexec.args="%1"

echo.
echo Done! You can now open the application and view/edit the holidays in the Holiday Calendar.
pause