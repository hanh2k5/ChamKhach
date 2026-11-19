param([switch]$Stop)

$root        = "c:\Users\Acer\Documents\ChamKhach"
$nginxExe    = "$root\nginx\nginx.exe"
$nginxDir    = "$root\nginx"
$phpCgiExe   = "$root\php\php-cgi.exe"
$backendDir  = "$root\backend"

function Stop-Backend {
    Write-Host "Dung Nginx..." -ForegroundColor Yellow
    if (Test-Path $nginxExe) {
        & $nginxExe -s stop 2>$null
    }
    Get-Process -Name "nginx" -ErrorAction SilentlyContinue | Stop-Process -Force
    Write-Host "Dung php-cgi..." -ForegroundColor Yellow
    Get-Process -Name "php-cgi" -ErrorAction SilentlyContinue | Stop-Process -Force
    # Stop any old php artisan serve
    Get-Process -Name "php" -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "*ChamKhach*" } | Stop-Process -Force
    Write-Host "Backend da dung." -ForegroundColor Green
}

if ($Stop) {
    Stop-Backend
    exit 0
}

Stop-Backend

Write-Host "Khoi dong Backend (Nginx + php-cgi 4 workers)..." -ForegroundColor Cyan

$env:PHP_FCGI_CHILDREN = "4"
$env:PHP_FCGI_MAX_REQUESTS = "500"

$phpCgiProc = Start-Process -FilePath $phpCgiExe `
    -ArgumentList "-b", "127.0.0.1:9000" `
    -WorkingDirectory $backendDir `
    -WindowStyle Hidden `
    -PassThru

Write-Host "php-cgi PID: $($phpCgiProc.Id)" -ForegroundColor Green
Start-Sleep -Milliseconds 500

Push-Location $nginxDir
$nginxProc = Start-Process -FilePath $nginxExe `
    -WorkingDirectory $nginxDir `
    -WindowStyle Hidden `
    -PassThru
Pop-Location

Write-Host "Nginx PID: $($nginxProc.Id)" -ForegroundColor Green
Start-Sleep -Seconds 1

Write-Host "Backend dang chay tai http://127.0.0.1:8000" -ForegroundColor Cyan
Write-Host "Emulator: http://10.0.2.2:8000" -ForegroundColor Cyan
