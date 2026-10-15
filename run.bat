@echo off
title CampusFin - Personal Finance Platform
echo ========================================================
echo   CampusFin - Online Personal Finance Platform
echo ========================================================
echo.

REM Create bin output directory
if not exist "bin" mkdir bin

echo [1/3] Compiling Java source files...
javac -encoding UTF-8 -cp "lib/*" -d "bin" src/main/java/com/finance/model/*.java src/main/java/com/finance/util/*.java src/main/java/com/finance/dao/*.java src/main/java/com/finance/service/*.java src/main/java/com/finance/web/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please check Java installation.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Copying resources and static assets...
xcopy /E /I /Y "src\main\resources" "bin" >nul

echo [3/3] Launching application on http://localhost:8080 ...
echo.
echo Press Ctrl+C in this terminal window to stop the server.
echo.

java -cp "bin;lib/*" com.finance.web.HttpServerApp

pause
