param (
    [Parameter(Mandatory=$true, HelpMessage="Número de teléfono (con código de país, ej. 5492634515362)")]
    [string]$Phone
)

Write-Host "Iniciando limpieza de sesión y registros para el número: $Phone" -ForegroundColor Cyan

# 1. Base de Datos (PostgreSQL)
$dbHost = "localhost"
$dbPort = "5433"
$dbUser = "postgres"
$dbName = "lawrabot_db"
$env:PGPASSWORD = "postgres_password"

# Extraemos solo los últimos 10 dígitos o limpiamos el teléfono para asegurar coincidencia
# O usar el teléfono tal cual que debería estar guardado como ej: 2634515362
# Nota: Si contact_phone_number guarda sin código de país (ej. 2634515362), ajustamos la variable:
$shortPhone = if ($Phone.Length -gt 10) { $Phone.Substring($Phone.Length - 10) } else { $Phone }

$sqlCommand = "DELETE FROM expedientes WHERE contact_phone_number = '$shortPhone';"

Write-Host "-> Eliminando registros en DB ($dbName en ${dbHost}:${dbPort}) para el número $shortPhone..."
try {
    # Usamos docker exec porque la base de datos corre en docker, garantizando que psql esté disponible
    $dockerCmd = "docker exec lawrabot_postgres psql -U $dbUser -d $dbName -c `"$sqlCommand`""
    
    $output = Invoke-Expression $dockerCmd 2>&1
    if ($LASTEXITCODE -ne 0 -and $null -ne $LASTEXITCODE) {
        Write-Host "   Fallo al ejecutar psql vía Docker." -ForegroundColor Red
        Write-Host "   (Ejecuta manualmente en tu base de datos: $sqlCommand)" -ForegroundColor Yellow
    } else {
        Write-Host "   Base de datos: Comando ejecutado correctamente en el contenedor Docker." -ForegroundColor Green
    }
} catch {
    Write-Host "   Error al ejecutar psql (quizás no esté instalado localmente): $_" -ForegroundColor Red
    Write-Host "   Ejecuta el siguiente comando SQL manualmente en tu cliente de base de datos:" -ForegroundColor Yellow
    Write-Host "   $sqlCommand" -ForegroundColor Yellow
}

# 2. Archivo de historial de TemplateClaw (Agent)
# El ID de conversación suele ser 'Numero@s.whatsapp.net'
$conversationId = "${Phone}@s.whatsapp.net"
# sanitización segura como manager.ts:
$safeConvo = $conversationId -replace "[^a-zA-Z0-9_-]", "_"

$sessionFilePath = Join-Path $PWD "agent\.data\sessions-divorce\whatsapp\${safeConvo}.jsonl"

Write-Host "-> Verificando archivo de sesión: $sessionFilePath"
if (Test-Path $sessionFilePath) {
    Remove-Item $sessionFilePath -Force
    Write-Host "   Historial de WhatsApp eliminado satisfactoriamente." -ForegroundColor Green
} else {
    Write-Host "   No se encontró historial en disco para este número." -ForegroundColor Yellow
}

Write-Host "==========================="
Write-Host "Limpieza finalizada con éxito." -ForegroundColor Cyan
