@echo off
echo Installing Apache Maven...

:: Check if Maven is already installed
where mvn >nul 2>&1
if %ERRORLEVEL% == 0 (
    echo Maven is already installed.
    goto :test_build
)

:: Download Maven
echo Downloading Apache Maven...
powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.8/binaries/apache-maven-3.9.8-bin.zip' -OutFile 'apache-maven-3.9.8-bin.zip'"

:: Check if download succeeded
if %ERRORLEVEL% NEQ 0 (
    echo Failed to download Maven
    pause
    exit /B 1
)

:: Extract Maven
echo Extracting Maven...
powershell -Command "Expand-Archive -Path 'apache-maven-3.9.8-bin.zip' -DestinationPath '.'"

:: Set environment variables
echo Setting environment variables...
set MAVEN_HOME=%~dp0apache-maven-3.9.8
set PATH=%PATH%;%MAVEN_HOME%\bin

:: Add to system PATH temporarily for this session
setx M2_HOME "%MAVEN_HOME%"
setx PATH "%PATH%;%MAVEN_HOME%\bin"

:: Clean up downloaded archive
del apache-maven-3.9.8-bin.zip

echo Maven installation completed successfully!

:test_build
echo Testing Maven installation...
mvn -version

echo Attempting to build the project...
cd /d "%~dp0"
mvn clean compile

pause