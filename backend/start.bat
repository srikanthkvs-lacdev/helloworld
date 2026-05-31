@echo off
cd /d "%~dp0"

set MAVEN_ZIP=maven.zip
set MAVEN_DIR=%~dp0maven
set MVN=%MAVEN_DIR%\bin\mvn.cmd

if exist "%MVN%" goto :run

echo Maven not found. Downloading Maven 3.9.6...
curl -L -o "%MAVEN_ZIP%" "https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"

if not exist "%MAVEN_ZIP%" (
    echo ERROR: Download failed. Trying alternate mirror...
    curl -L -o "%MAVEN_ZIP%" "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
)

if not exist "%MAVEN_ZIP%" (
    echo ERROR: Could not download Maven. Check your internet connection.
    pause
    exit /b 1
)

echo Extracting Maven...
powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%~dp0' -Force"

if exist "%~dp0apache-maven-3.9.6" (
    move "%~dp0apache-maven-3.9.6" "%MAVEN_DIR%"
)

del "%MAVEN_ZIP%" 2>nul

if not exist "%MVN%" (
    echo ERROR: Extraction failed.
    pause
    exit /b 1
)

echo Maven ready.

:run
echo Starting Spring Boot backend on http://localhost:8080 ...
"%MVN%" spring-boot:run
pause
