param([switch]$Stop)

$root        = "c:\Users\Acer\Documents\ChamKhach"
$phpExe      = "$root\php\php.exe"
$backendDir  = "$root\backend"

function Stop-Backend {
    Write-Host "Dung backend..." -ForegroundColor Yellow
    Get-Process -Name "php" -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "*ChamKhach*" } | Stop-Process -Force
    Write-Host "Backend da dung." -ForegroundColor Green
}

if ($Stop) {
    Stop-Backend
    exit 0
}

Stop-Backend

Write-Host "Khoi dong Backend (artisan serve port 8000)..." -ForegroundColor Cyan

$serveProc = Start-Process -FilePath $phpExe `
    -ArgumentList "artisan", "serve", "--host=0.0.0.0", "--port=8000" `
    -WorkingDirectory $backendDir `
    -WindowStyle Hidden `
    -PassThru

Write-Host "Backend dang chay tai PID: $($serveProc.Id)" -ForegroundColor Green
Write-Host "Local URL: http://127.0.0.1:8000" -ForegroundColor Cyan
Write-Host "Emulator URL: http://10.0.2.2:8000" -ForegroundColor Cyan

