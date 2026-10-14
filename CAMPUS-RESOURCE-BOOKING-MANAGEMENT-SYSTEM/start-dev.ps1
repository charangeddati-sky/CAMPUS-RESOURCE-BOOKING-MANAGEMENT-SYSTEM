$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$mvn = "C:\Users\ganta\tools\apache-maven-3.9.9\bin\mvn.cmd"
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

if (Test-Path $mysql) {
  & $mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS college_resource_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
}

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root\backend'; & '$mvn' spring-boot:run"
Start-Sleep -Seconds 2
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$root\frontend'; if (-not (Test-Path node_modules)) { npm install }; npm run dev"
Write-Host "CampusOS starting: backend :8080  frontend :5173"
