@echo off
javac --add-modules jdk.httpserver -cp ".;lib/*" SQMSServer.java
if errorlevel 1 exit /b 1
java --add-modules jdk.httpserver -cp ".;lib/*" SQMSServer
