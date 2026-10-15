@echo off
title Push to GitHub - simrankumari2917-cpu/Finance-Platform
echo ===================================================================
echo   Pushing to GitHub: simrankumari2917-cpu/Finance-Platform
echo ===================================================================
echo.

set GIT_PATH="%LOCALAPPDATA%\GitHubDesktop\app-3.6.7\resources\app\git\cmd\git.exe"

if not exist %GIT_PATH% (
    set GIT_PATH=git
)

echo [1/2] Verifying remote repository...
%GIT_PATH% remote -v
echo.

echo [2/2] Pushing branch 'main' to origin...
echo (If prompted, please complete sign-in in your browser or Git Credential Manager)
echo.
%GIT_PATH% push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===================================================================
    echo   SUCCESS! Project pushed to:
    echo   https://github.com/simrankumari2917-cpu/Finance-Platform
    echo ===================================================================
) else (
    echo.
    echo Push encountered an error. Please verify authentication.
)

echo.
pause
