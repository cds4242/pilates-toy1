@echo off
REM Internal launcher: runs backend bootRun and redirects to log file.
REM Args: %1 = log file path, %2 = spring profile
set "LOG_FILE=%~1"
set "PROFILE=%~2"
if "%LOG_FILE%"=="" exit /b 1
if "%PROFILE%"=="" set "PROFILE=local"
cd /d "%~dp0..\backend"
call gradlew.bat bootRun --args=--spring.profiles.active=%PROFILE% > "%LOG_FILE%" 2>&1
