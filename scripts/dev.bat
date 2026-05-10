@echo off
setlocal EnableDelayedExpansion

REM ============================================================
REM  Pilates Local Dev Manager
REM  Usage: dev.bat [start|stop|restart|status] [all|infra|backend|frontend]
REM  Default action: status   Default target: all
REM ============================================================

set "ROOT=%~dp0.."
pushd "%ROOT%"
set "ROOT=%CD%"
popd

set "PID_DIR=%ROOT%\scripts\.pids"
set "LOG_DIR=%ROOT%\scripts\.logs"
if not exist "%PID_DIR%" mkdir "%PID_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

set "BACKEND_PORT=8080"
set "FRONTEND_PORT=3000"
set "BACKEND_PROFILE=local"

set "ACTION=%~1"
set "TARGET=%~2"
if "%ACTION%"=="" set "ACTION=status"
if "%TARGET%"=="" set "TARGET=all"

echo.
echo === Pilates Local Dev Manager ===
echo Action: %ACTION%   Target: %TARGET%
echo Root:   %ROOT%
echo.

if /I "%ACTION%"=="start"   goto :do_start
if /I "%ACTION%"=="stop"    goto :do_stop
if /I "%ACTION%"=="restart" goto :do_restart
if /I "%ACTION%"=="status"  goto :do_status

echo [ERROR] Unknown action: %ACTION%
echo Usage: dev.bat [start^|stop^|restart^|status] [all^|infra^|backend^|frontend]
exit /b 1

REM ============================================================
REM  ACTION DISPATCHERS
REM ============================================================
:do_start
if /I "%TARGET%"=="all"      goto :do_start_all
if /I "%TARGET%"=="infra"    goto :do_start_infra
if /I "%TARGET%"=="backend"  goto :do_start_backend
if /I "%TARGET%"=="frontend" goto :do_start_frontend
echo [ERROR] Unknown target: %TARGET%
exit /b 1

:do_start_all
call :start_infra
call :wait_infra
call :start_backend
call :start_frontend
goto :end

:do_start_infra
call :start_infra
call :wait_infra
goto :end

:do_start_backend
call :start_backend
goto :end

:do_start_frontend
call :start_frontend
goto :end

:do_stop
if /I "%TARGET%"=="all"      goto :do_stop_all
if /I "%TARGET%"=="infra"    goto :do_stop_infra
if /I "%TARGET%"=="backend"  goto :do_stop_backend
if /I "%TARGET%"=="frontend" goto :do_stop_frontend
echo [ERROR] Unknown target: %TARGET%
exit /b 1

:do_stop_all
call :stop_frontend
call :stop_backend
call :stop_infra
goto :end

:do_stop_infra
call :stop_infra
goto :end

:do_stop_backend
call :stop_backend
goto :end

:do_stop_frontend
call :stop_frontend
goto :end

:do_restart
if /I "%TARGET%"=="all"      goto :do_restart_all
if /I "%TARGET%"=="infra"    goto :do_restart_infra
if /I "%TARGET%"=="backend"  goto :do_restart_backend
if /I "%TARGET%"=="frontend" goto :do_restart_frontend
echo [ERROR] Unknown target: %TARGET%
exit /b 1

:do_restart_all
call :stop_frontend
call :stop_backend
call :stop_infra
ping -n 3 127.0.0.1 > nul
call :start_infra
call :wait_infra
call :start_backend
call :start_frontend
goto :end

:do_restart_infra
call :stop_infra
ping -n 3 127.0.0.1 > nul
call :start_infra
call :wait_infra
goto :end

:do_restart_backend
call :stop_backend
ping -n 3 127.0.0.1 > nul
call :start_backend
goto :end

:do_restart_frontend
call :stop_frontend
ping -n 3 127.0.0.1 > nul
call :start_frontend
goto :end

:do_status
call :status_infra
call :status_backend
call :status_frontend
goto :end

REM ============================================================
REM  INFRA (Docker Compose: MySQL + Redis)
REM ============================================================
:start_infra
echo [INFRA] Starting docker compose mysql and redis
where docker > nul 2>&1
if errorlevel 1 goto :start_infra_no_docker
pushd "%ROOT%\infra"
docker compose up -d
set "RC=%ERRORLEVEL%"
popd
if not "%RC%"=="0" goto :start_infra_failed
echo [INFRA] OK
exit /b 0
:start_infra_no_docker
echo [INFRA] [ERROR] docker not found in PATH
exit /b 1
:start_infra_failed
echo [INFRA] [ERROR] docker compose up failed exit=%RC%
exit /b %RC%

:wait_infra
echo [INFRA] Waiting for MySQL healthcheck
set /a TRIES=0
:wait_infra_loop
set /a TRIES+=1
set "HSTATUS="
for /f "delims=" %%H in ('docker inspect -f "{{.State.Health.Status}}" pilates-mysql 2^>nul') do set "HSTATUS=%%H"
if "%HSTATUS%"=="healthy" goto :wait_infra_ok
if %TRIES% GEQ 30 goto :wait_infra_timeout
ping -n 2 127.0.0.1 > nul
goto :wait_infra_loop
:wait_infra_ok
echo [INFRA] MySQL is healthy
exit /b 0
:wait_infra_timeout
echo [INFRA] [WARN] MySQL not healthy after 30 tries. Continuing.
exit /b 0

:stop_infra
echo [INFRA] Stopping docker compose
where docker > nul 2>&1
if errorlevel 1 goto :stop_infra_no_docker
pushd "%ROOT%\infra"
docker compose down
popd
echo [INFRA] OK
exit /b 0
:stop_infra_no_docker
echo [INFRA] [WARN] docker not found, skipping
exit /b 0

:status_infra
echo [INFRA] Status:
where docker > nul 2>&1
if errorlevel 1 goto :status_infra_no_docker
docker ps --filter "name=pilates-mysql" --filter "name=pilates-redis" --format "  {{.Names}}  {{.Status}}"
exit /b 0
:status_infra_no_docker
echo   docker not found in PATH
exit /b 0

REM ============================================================
REM  BACKEND (Spring Boot via gradlew bootRun)
REM ============================================================
:start_backend
call :find_pid_by_port %BACKEND_PORT%
if defined FOUND_PID goto :start_backend_already
echo [BACKEND] Starting Spring Boot profile=%BACKEND_PROFILE% port=%BACKEND_PORT%
echo [BACKEND] Logs: %LOG_DIR%\backend.log
start "pilates-backend" /MIN "%ROOT%\scripts\_run_backend.bat" "%LOG_DIR%\backend.log" %BACKEND_PROFILE%
ping -n 3 127.0.0.1 > nul
call :find_pid_by_port %BACKEND_PORT%
if defined FOUND_PID goto :start_backend_bound
echo [BACKEND] Launched. Port %BACKEND_PORT% will bind shortly. Tail %LOG_DIR%\backend.log
exit /b 0
:start_backend_already
echo [BACKEND] Already running PID !FOUND_PID! port %BACKEND_PORT%
exit /b 0
:start_backend_bound
echo !FOUND_PID!> "%PID_DIR%\backend.pid"
echo [BACKEND] Started PID !FOUND_PID! port %BACKEND_PORT%
exit /b 0

:stop_backend
echo [BACKEND] Stopping
call :kill_by_port %BACKEND_PORT% backend
if exist "%PID_DIR%\backend.pid" del "%PID_DIR%\backend.pid"
echo [BACKEND] OK
exit /b 0

:status_backend
echo [BACKEND] Status:
call :find_pid_by_port %BACKEND_PORT%
if defined FOUND_PID goto :status_backend_running
echo   STOPPED  port %BACKEND_PORT% not bound
exit /b 0
:status_backend_running
echo   RUNNING  PID !FOUND_PID!  port %BACKEND_PORT%
exit /b 0

REM ============================================================
REM  FRONTEND (Next.js via pnpm dev)
REM ============================================================
:start_frontend
call :find_pid_by_port %FRONTEND_PORT%
if defined FOUND_PID goto :start_frontend_already
where pnpm > nul 2>&1
if errorlevel 1 goto :start_frontend_no_pnpm
echo [FRONTEND] Starting Next.js port=%FRONTEND_PORT%
echo [FRONTEND] Logs: %LOG_DIR%\frontend.log
start "pilates-frontend" /MIN "%ROOT%\scripts\_run_frontend.bat" "%LOG_DIR%\frontend.log"
ping -n 3 127.0.0.1 > nul
call :find_pid_by_port %FRONTEND_PORT%
if defined FOUND_PID goto :start_frontend_bound
echo [FRONTEND] Launched. Port %FRONTEND_PORT% will bind shortly. Tail %LOG_DIR%\frontend.log
exit /b 0
:start_frontend_already
echo [FRONTEND] Already running PID !FOUND_PID! port %FRONTEND_PORT%
exit /b 0
:start_frontend_no_pnpm
echo [FRONTEND] [ERROR] pnpm not found in PATH
exit /b 1
:start_frontend_bound
echo !FOUND_PID!> "%PID_DIR%\frontend.pid"
echo [FRONTEND] Started PID !FOUND_PID! port %FRONTEND_PORT%
exit /b 0

:stop_frontend
echo [FRONTEND] Stopping
call :kill_by_port %FRONTEND_PORT% frontend
if exist "%PID_DIR%\frontend.pid" del "%PID_DIR%\frontend.pid"
echo [FRONTEND] OK
exit /b 0

:status_frontend
echo [FRONTEND] Status:
call :find_pid_by_port %FRONTEND_PORT%
if defined FOUND_PID goto :status_frontend_running
echo   STOPPED  port %FRONTEND_PORT% not bound
exit /b 0
:status_frontend_running
echo   RUNNING  PID !FOUND_PID!  port %FRONTEND_PORT%
exit /b 0

REM ============================================================
REM  HELPERS
REM ============================================================
:find_pid_by_port
set "FOUND_PID="
for /f "tokens=5" %%P in ('netstat -ano -p tcp ^| findstr ":%~1 " ^| findstr "LISTENING"') do (
    set "FOUND_PID=%%P"
    goto :find_pid_by_port_done
)
:find_pid_by_port_done
exit /b 0

:kill_by_port
set "PORT=%~1"
set "LABEL=%~2"
set "KILLED=0"
for /f "tokens=5" %%P in ('netstat -ano -p tcp ^| findstr ":%PORT% " ^| findstr "LISTENING"') do (
    echo [%LABEL%] Killing PID %%P port %PORT%
    taskkill /PID %%P /T /F > nul 2>&1
    set "KILLED=1"
)
if "%KILLED%"=="0" echo [%LABEL%] No process on port %PORT%
exit /b 0

:end
echo.
endlocal
exit /b 0
