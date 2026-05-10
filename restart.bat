@echo off
REM Restart all services
REM Usage: restart.bat [all|infra|backend|frontend]
call "%~dp0scripts\dev.bat" restart %*
echo.
pause
