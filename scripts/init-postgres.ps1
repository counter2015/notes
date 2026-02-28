param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 5432,
    [string]$AdminUser = "postgres",
    [string]$AppDb = "notes",
    [string]$AppUser = "notes",
    [string]$AppPassword = "notes"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw "psql not found. Please install PostgreSQL client tools first."
}

$scriptPath = Join-Path $PSScriptRoot "init-postgres.sql"

Write-Host "Initializing PostgreSQL (idempotent) ..."
Write-Host "  host=$DbHost port=$Port admin=$AdminUser app_db=$AppDb app_user=$AppUser"

psql `
  -v ON_ERROR_STOP=1 `
  -h $DbHost `
  -p $Port `
  -U $AdminUser `
  -d postgres `
  -v app_user="$AppUser" `
  -v app_password="$AppPassword" `
  -v app_db="$AppDb" `
  -f $scriptPath

Write-Host "Done."
