#!/bin/bash

# Simple Project Resource Manager Launcher Script
# This script provides multiple reliable ways to run the application

echo "=== Simple Project Resource Manager Launcher ==="

show_menu() {
    echo ""
    echo "Choose how to run the application:"
    echo "1) Maven Exec Plugin (Recommended - Most Stable)"
    echo "2) Build and run standalone JAR"  
    echo "3) Maven JavaFX Plugin"
    echo "4) Exit"
    echo ""
    read -p "Enter choice [1-4]: " choice
}

run_with_exec() {
    echo "Compiling project..."
    mvn compile -q
    
    if [ $? -ne 0 ]; then
        echo "Compilation failed!"
        return 1
    fi
    
    echo "Launching with Maven Exec Plugin..."
    echo "Note: This should keep the application running indefinitely."
    echo "Close the application window when done."
    echo ""
    
    mvn exec:java \
        -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.SchedulerApplication" \
        -Dexec.keepAlive=true \
        -Djava.awt.headless=false
}

run_with_jar() {
    echo "Building standalone JAR..."
    mvn package -q
    
    if [ $? -ne 0 ]; then
        echo "Build failed!"
        return 1
    fi
    
    echo "Launching standalone JAR..."
    echo "Note: This should keep the application running indefinitely."
    echo "Close the application window when done."
    echo ""
    
    java --module-path ./target/classes:./target/lib \
         --add-modules javafx.controls,javafx.fxml \
         -jar target/SimpleProjectResourceManager-1.0-SNAPSHOT.jar
}

run_with_javafx() {
    echo "Launching with JavaFX Plugin..."
    echo "Note: May have timeout issues with some Maven versions."
    echo ""
    
    mvn javafx:run
}

# Main menu loop
while true; do
    show_menu
    case $choice in
        1)
            run_with_exec
            break
            ;;
        2)
            run_with_jar
            break
            ;;
        3)
            run_with_javafx
            break
            ;;
        4)
            echo "Exiting..."
            exit 0
            ;;
        *)
            echo "Invalid choice. Please enter 1-4."
            ;;
    esac
done

echo ""
echo "Application has exited."