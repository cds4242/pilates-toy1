@echo off
REM Stop all services (frontend -> backend -> infra)
REM Usage: stop.bat [all|infra|backend|frontend]
call "%~dp0scripts\dev.bat" stop %*
echo.
pause
