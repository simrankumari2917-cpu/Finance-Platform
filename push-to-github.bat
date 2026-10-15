@echo off
title Push to GitHub - simrankumari2917-cpu/Finance-Platform
echo ===================================================================
echo   Pushing to GitHub: simrankumari2917-cpu/Finance-Platform
echo ===================================================================
echo.

set "GIT_EXE=C:\Program Files\Git\cmd\git.exe"

if not exist "%GIT_EXE%" (
    set "GIT_EXE=git"
)

echo [1/2] Verifying remote repository...
"%GIT_EXE%" remote -v
echo.

echo [2/2] Pushing branch 'main' to origin...
echo (If prompted, please complete the sign-in in your browser window)
echo.

"%GIT_EXE%" push origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===================================================================
    echo   SUCCESS! All project files pushed to:
    echo   https://github.com/simrankumari2917-cpu/Finance-Platform
    echo ===================================================================
) else (
    echo.
    echo Push encountered an error. Please check your internet or GitHub sign-in.
)

echo.
pause
