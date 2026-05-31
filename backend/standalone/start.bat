@echo off
cd /d "%~dp0"
echo Compiling...
javac -cp h2.jar Server.java
if errorlevel 1 (
    echo Compile failed.
    pause
    exit /b 1
)
echo Starting server on http://localhost:8080 ...
java -cp .;h2.jar Server
pause
