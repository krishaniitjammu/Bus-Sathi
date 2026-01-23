# Security Verification Script
# Run this before uploading to GitHub to ensure no sensitive data remains

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Bus Tracker App - Security Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$hasIssues = $false

# Check for sensitive files
Write-Host "[1/6] Checking for sensitive files..." -ForegroundColor Yellow

$sensitiveFiles = @(
    "app_dev_key",
    "local.properties",
    "app\google-services.json",
    ".env"
)

foreach ($file in $sensitiveFiles) {
    if (Test-Path $file) {
        Write-Host "   ❌ FOUND: $file (MUST BE REMOVED)" -ForegroundColor Red
        $hasIssues = $true
    }
    else {
        Write-Host "   ✅ OK: $file not found" -ForegroundColor Green
    }
}

# Check for GPS data files
Write-Host ""
Write-Host "[2/6] Checking for GPS tracking data..." -ForegroundColor Yellow

$csvFiles = Get-ChildItem -Recurse -Filter "*.csv" -ErrorAction SilentlyContinue
$gpxFiles = Get-ChildItem -Recurse -Filter "*.gpx" -ErrorAction SilentlyContinue

if ($csvFiles.Count -gt 0) {
    Write-Host "   ❌ FOUND $($csvFiles.Count) CSV file(s):" -ForegroundColor Red
    $csvFiles | ForEach-Object { Write-Host "      - $($_.FullName)" -ForegroundColor Red }
    $hasIssues = $true
}
else {
    Write-Host "   ✅ OK: No CSV files found" -ForegroundColor Green
}

if ($gpxFiles.Count -gt 0) {
    Write-Host "   ❌ FOUND $($gpxFiles.Count) GPX file(s):" -ForegroundColor Red
    $gpxFiles | ForEach-Object { Write-Host "      - $($_.FullName)" -ForegroundColor Red }
    $hasIssues = $true
}
else {
    Write-Host "   ✅ OK: No GPX files found" -ForegroundColor Green
}

# Check for build directories
Write-Host ""
Write-Host "[3/6] Checking for build artifacts..." -ForegroundColor Yellow

$buildDirs = @("build", ".gradle", "app\build", "app\release")
foreach ($dir in $buildDirs) {
    if (Test-Path $dir) {
        Write-Host "   ⚠️  WARNING: $dir exists (will be ignored by .gitignore)" -ForegroundColor Yellow
    }
    else {
        Write-Host "   ✅ OK: $dir not found" -ForegroundColor Green
    }
}

# Verify template files exist
Write-Host ""
Write-Host "[4/6] Verifying template files exist..." -ForegroundColor Yellow

$templateFiles = @(
    "local.properties.template",
    "app\google-services.json.template"
)

foreach ($file in $templateFiles) {
    if (Test-Path $file) {
        Write-Host "   ✅ OK: $file exists" -ForegroundColor Green
    }
    else {
        Write-Host "   ❌ MISSING: $file (should exist)" -ForegroundColor Red
        $hasIssues = $true
    }
}

# Verify .gitignore exists and has required patterns
Write-Host ""
Write-Host "[5/6] Verifying .gitignore configuration..." -ForegroundColor Yellow

if (Test-Path ".gitignore") {
    $gitignoreContent = Get-Content ".gitignore" -Raw
    $requiredPatterns = @(
        "google-services.json",
        "local.properties",
        "app_dev_key",
        "*.csv",
        "*.gpx"
    )
    
    $allPatternsFound = $true
    foreach ($pattern in $requiredPatterns) {
        if ($gitignoreContent -match [regex]::Escape($pattern)) {
            Write-Host "   ✅ OK: .gitignore includes '$pattern'" -ForegroundColor Green
        }
        else {
            Write-Host "   ❌ MISSING: .gitignore doesn't include '$pattern'" -ForegroundColor Red
            $allPatternsFound = $false
            $hasIssues = $true
        }
    }
}
else {
    Write-Host "   ❌ MISSING: .gitignore file not found!" -ForegroundColor Red
    $hasIssues = $true
}

# Check documentation
Write-Host ""
Write-Host "[6/6] Verifying documentation files..." -ForegroundColor Yellow

$docFiles = @("README.md", "SETUP.md", "CONTRIBUTING.md", "LICENSE")
foreach ($file in $docFiles) {
    if (Test-Path $file) {
        Write-Host "   ✅ OK: $file exists" -ForegroundColor Green
    }
    else {
        Write-Host "   ⚠️  WARNING: $file not found (recommended)" -ForegroundColor Yellow
    }
}

# Final summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Verification Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($hasIssues) {
    Write-Host "❌ SECURITY ISSUES FOUND!" -ForegroundColor Red
    Write-Host "Please fix the issues above before uploading to GitHub." -ForegroundColor Red
    Write-Host ""
    exit 1
}
else {
    Write-Host "✅ All checks passed!" -ForegroundColor Green
    Write-Host "Your repository is ready for GitHub upload." -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. git init" -ForegroundColor White
    Write-Host "2. git add ." -ForegroundColor White
    Write-Host "3. git commit -m 'Initial commit'" -ForegroundColor White
    Write-Host "4. Create repository on GitHub" -ForegroundColor White
    Write-Host "5. git remote add origin <your-repo-url>" -ForegroundColor White
    Write-Host "6. git push -u origin main" -ForegroundColor White
    Write-Host ""
    exit 0
}
