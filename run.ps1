# ========================================================
# CampusFin - PowerShell Run Script
# ========================================================

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  CampusFin - Online Personal Finance Platform" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Prepare bin directory
if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

# 2. Compile Java classes
Write-Host "[1/3] Compiling Java classes..." -ForegroundColor Yellow
$jars = (Get-ChildItem -Path "lib\*.jar" | ForEach-Object { $_.FullName }) -join ";"
$sources = (Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })

javac -encoding UTF-8 -cp "$jars" -d "bin" $sources
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

# 3. Copy resources
Write-Host "[2/3] Copying static assets and configuration..." -ForegroundColor Yellow
Copy-Item -Path "src\main\resources\*" -Destination "bin\" -Recurse -Force

# 4. Launch Application
Write-Host "[3/3] Starting server on http://localhost:8080 ..." -ForegroundColor Green
Write-Host "Open your browser to: http://localhost:8080" -ForegroundColor White
Write-Host "Press Ctrl+C to terminate the application." -ForegroundColor Gray
Write-Host ""

java -cp "bin;lib/*" com.finance.web.HttpServerApp
