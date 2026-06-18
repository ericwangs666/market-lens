@echo off
setlocal EnableDelayedExpansion

set WRAPPER_DIR=%~dp0.mvn\wrapper
set PROPERTIES_FILE=%WRAPPER_DIR%\maven-wrapper.properties
set MAVEN_VERSION=3.9.9
set MAVEN_DIR=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%
set MAVEN_CMD=%MAVEN_DIR%\bin\mvn.cmd
set MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip
set DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

where java >NUL 2>NUL
if errorlevel 1 (
  for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-17*-hotspot") do (
    if exist "%%J\bin\java.exe" (
      set "JAVA_HOME=%%J"
      set "PATH=%%J\bin;!PATH!"
    )
  )
)

if exist "%PROPERTIES_FILE%" (
  for /f "tokens=1,* delims==" %%A in (%PROPERTIES_FILE%) do (
    if "%%A"=="distributionUrl" set DOWNLOAD_URL=%%B
  )
)

if not exist "%MAVEN_CMD%" (
  echo Maven %MAVEN_VERSION% was not found locally. Downloading...
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  if exist "%MAVEN_ZIP%" del "%MAVEN_ZIP%"
  curl.exe --ssl-no-revoke -fL "%DOWNLOAD_URL%" -o "%MAVEN_ZIP%"
  if errorlevel 1 (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%MAVEN_ZIP%'"
  )
  if not exist "%MAVEN_ZIP%" (
    echo Failed to download Maven wrapper distribution.
    exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%WRAPPER_DIR%' -Force"
  if errorlevel 1 (
    echo Failed to download Maven wrapper distribution.
    exit /b 1
  )
)

call "%MAVEN_CMD%" %*
endlocal
