@echo off
REM Internal launcher: runs frontend dev server and redirects to log file.
REM Args: %1 = log file path
set "LOG_FILE=%~1"
if "%LOG_FILE%"=="" exit /b 1
cd /d "%~dp0..\frontend"
call pnpm dev > "%LOG_FILE%" 2>&1
