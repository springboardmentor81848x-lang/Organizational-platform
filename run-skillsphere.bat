@echo off
setlocal

title SkillSphere Launcher

set "PROJECT=C:\Users\gjaya\OneDrive\Desktop\SkillSphere\skillsphere"
set "BACKEND=%PROJECT%\backend"
set "FRONTEND=%PROJECT%\frontend"

echo ==========================================
echo       SkillSphere Application Launcher
echo ==========================================
echo.

REM Verify the project directories
if not exist "%BACKEND%\pom.xml" (
    echo ERROR: Backend pom.xml was not found.
    echo Check the PROJECT path in this BAT file.
    pause
    exit /b 1
)

if not exist "%FRONTEND%\package.json" (
    echo ERROR: Frontend package.json was not found.
    echo Check the PROJECT path in this BAT file.
    pause
    exit /b 1
)

REM Start PostgreSQL using the existing Docker Compose configuration
echo [1/4] Starting PostgreSQL / Docker services...
cd /d "%PROJECT%"
docker compose up -d

if errorlevel 1 (
    echo.
    echo ERROR: Docker Compose failed.
    echo Make sure Docker Desktop is running.
    pause
    exit /b 1
)

REM Give PostgreSQL time to initialize
echo.
echo Waiting 5 seconds for database initialization...
timeout /t 5 /nobreak >nul

REM Start the Spring Boot backend
echo.
echo [2/4] Starting Spring Boot backend...
start "SkillSphere Backend" /D "%BACKEND%" cmd /k "mvn spring-boot:run -Dspring-boot.run.jvmArguments=-Duser.timezone=Asia/Kolkata"

REM Start the React frontend
echo.
echo [3/4] Starting React frontend...
start "SkillSphere Frontend" /D "%FRONTEND%" cmd /k "npm run dev -- --host 127.0.0.1"

REM Open the application in the browser
echo.
echo [4/4] Opening SkillSphere in your browser...
timeout /t 5 /nobreak >nul
start "" "http://localhost:5173/"

echo.
echo ==========================================
echo SkillSphere startup commands have been sent.
echo.
echo Frontend: http://localhost:5173/
echo Backend:  http://localhost:8080/
echo.
echo Keep the backend and frontend windows open.
echo Close them when you want to stop the application.
echo ==========================================
echo.
pause

endlocal