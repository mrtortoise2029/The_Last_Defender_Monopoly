@echo off
echo Starting The Last Defenders...
java --module-path "D:\openjsx21\javafx-sdk-21.0.8\lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media -cp "target\classes;%USERPROFILE%\.m2\repository\com\google\code\gson\gson\2.11.0\gson-2.11.0.jar" Ui.main
pause
