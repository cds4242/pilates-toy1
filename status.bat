@echo off
REM Check service status (infra + backend + frontend)
REM Usage: status.bat
call "%~dp0scripts\dev.bat" status
echo.
pause
