@echo off
chcp 65001 >nul
title Telesales Assistant Launcher
echo ====================================================
echo        KHOI DONG TELESALES ASSISTANT
echo ====================================================
echo.

echo [1/2] Dang khoi dong Backend (Laravel API port 8000)...
powershell -ExecutionPolicy Bypass -File "%~dp0start-backend.ps1"
echo.

echo [2/2] Kiem tra thiet bi Android / May ao...
set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

if exist %ADB% (
    %ADB% devices | findstr /R "device$" >nul
    if %errorlevel% equ 0 (
        echo Tim thay may ao! Dang khoi chay Telesales Assistant...
        %ADB% shell am start -n com.telesales.assistant/.MainActivity >nul
        echo App da duoc mo tren man hinh may ao!
    ) else (
        echo Chua bat may ao Android. Backend van dang chay ngam tai:
        echo http://127.0.0.1:8000
    )
) else (
    echo Backend da san sang tai port 8000!
)

echo.
echo ====================================================
echo   Chay thanh cong! Ban co the bat dau su dung app.
echo ====================================================
timeout /t 5

