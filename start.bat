@echo off
REM Start all services (infra -> backend -> frontend)
REM Usage: start.bat [all|infra|backend|frontend]
call "%~dp0scripts\dev.bat" start %*
