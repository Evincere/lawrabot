param (
    [Parameter(Mandatory=$false, HelpMessage="Número de teléfono (con código de país, ej. 5492634515362)")]
    [string]$Phone,

    [Parameter(Mandatory=$false, HelpMessage="Limpiar TODOS los registros en base de datos y archivos de sesión (incluso credenciales si es severo)")]
    [switch]$All
)

if (-not $Phone -and -not $All) {
    Write-Host "DEBES proveer un número de teléfono con '-Phone' o usar el flag '-All' para limpieza total de la base de datos." -ForegroundColor Red
    exit 1
}

$dbHost = "localhost"
$dbPort = "5433"
$dbUser = "postgres"
$dbName = "lawrabot_db"
$env:PGPASSWORD = "postgres_password"

if ($All) {
    Write-Host "Iniciando TRUNCAMIENTO TOTAL de la base de datos y eliminación de TODOS los archivos de sesión..." -ForegroundColor Cyan
    $sqlCommand = @"
    TRUNCATE TABLE case_participants, spouses, children, regulatory_agreements, socioeconomic_profiles, expedientes RESTART IDENTITY CASCADE;
    TRUNCATE TABLE correction_feedback, citizens RESTART IDENTITY CASCADE;
"@
} else {
    Write-Host "Iniciando LIMPIEZA TOTAL de sesión y registros solo para el número: $Phone" -ForegroundColor Cyan

    # Extraemos los últimos 10 dígitos para búsquedas flexibles
    $shortPhone = if ($Phone.Length -gt 10) { $Phone.Substring($Phone.Length - 10) } else { $Phone }

    $sqlCommand = @"
    DO $$ 
    DECLARE
        v_exp_ids UUID[];
        v_spouse_ids UUID[];
    BEGIN
        -- 1. Identificar IDs de expedientes
        SELECT array_agg(id) INTO v_exp_ids FROM expedientes WHERE contact_phone_number LIKE '%$shortPhone';
        
        IF v_exp_ids IS NOT NULL THEN
            -- Identificar spouses vinculados antes de borrar el expediente
            SELECT array_agg(petitioner_id) INTO v_spouse_ids FROM expedientes WHERE id = ANY(v_exp_ids);
            SELECT array_agg(respondent_id) INTO v_spouse_ids FROM expedientes WHERE id = ANY(v_exp_ids) AND respondent_id IS NOT NULL;

            -- Borrar dependencias directas
            DELETE FROM children WHERE expediente_id = ANY(v_exp_ids);
            DELETE FROM case_participants WHERE expediente_id = ANY(v_exp_ids);
            DELETE FROM digital_evidences WHERE expediente_id = ANY(v_exp_ids);
            DELETE FROM correction_feedback WHERE case_id = ANY(v_exp_ids);
            
            -- Borrar expedientes
            DELETE FROM expedientes WHERE id = ANY(v_exp_ids);
            
            -- Borrar spouses (si no están referenciados por otros expedientes, simplificado a borrar los encontrados)
            DELETE FROM spouses WHERE id = ANY(v_spouse_ids);
        END IF;

        -- 2. Limpieza de Ciudadano (MCI) y sus Feedbacks/Participaciones
        -- Primero participaciones ligadas al ciudadano
        DELETE FROM case_participants WHERE citizen_id IN (SELECT id FROM citizens WHERE phone_number LIKE '%$shortPhone');
        
        -- Luego feedbacks
        DELETE FROM correction_feedback WHERE citizen_id IN (SELECT id FROM citizens WHERE phone_number LIKE '%$shortPhone');
        
        -- Finalmente el ciudadano
        DELETE FROM citizens WHERE phone_number LIKE '%$shortPhone';

        RAISE NOTICE 'Limpieza de base de datos completada para el patron %', '$shortPhone';
    END $$;
"@
}

Write-Host "-> Ejecutando limpieza profunda en DB ($dbName)..."
$dockerCmd = "docker exec lawrabot-db psql -U $dbUser -d $dbName -c `"$sqlCommand`""
$output = Invoke-Expression $dockerCmd 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "   Base de datos: Limpieza finalizada satisfactoriamente." -ForegroundColor Green
} else {
    Write-Host "   Error en limpieza de DB: $output" -ForegroundColor Red
}

# Archivos de persistencia del Agente
if ($All) {
    # Eliminar todos los json que empiecen con session- (excluyendo credenciales de whatsapp por defecto)
    $targetJsonFiles = Get-ChildItem -Path (Join-Path $PWD "agent\.data") -Filter "session-*.json" -Recurse
    $sessionJsonlFiles = Get-ChildItem -Path (Join-Path $PWD "agent\.data") -Filter "*.jsonl" -Recurse

    if ($sessionJsonlFiles) {
        $sessionJsonlFiles | Remove-Item -Force
        Write-Host "   -> $($sessionJsonlFiles.Count) historiales .jsonl eliminados." -ForegroundColor Green
    }

    if ($targetJsonFiles) {
        $targetJsonFiles | Remove-Item -Force
        Write-Host "   -> $($targetJsonFiles.Count) archivos temporales JSON de sesiones eliminados." -ForegroundColor Green
    }
} else {
    $conversationId = "${Phone}@s.whatsapp.net"
    $safeConvo = $conversationId -replace "[^a-zA-Z0-9_-]", "_"

    # a) Historial TemplateClaw (.jsonl)
    $sessionJsonl = Join-Path $PWD "agent\.data\sessions-divorce\whatsapp\${safeConvo}.jsonl"
    if (Test-Path $sessionJsonl) {
        Remove-Item $sessionJsonl -Force
        Write-Host "   -> Historial .jsonl eliminado." -ForegroundColor Green
    }

    # b) Persistencia de Chat Ollama/Persistent (.json)
    $targetJsonFiles = Get-ChildItem -Path (Join-Path $PWD "agent\.data") -Filter "session-${Phone}*.json" -Recurse

    if ($targetJsonFiles) {
        $targetJsonFiles | Remove-Item -Force
        Write-Host "   -> $($targetJsonFiles.Count) archivos de sesión .json eliminados para $Phone." -ForegroundColor Green
    } else {
        Write-Host "   No se encontraron archivos .json de persistencia." -ForegroundColor Yellow
    }
}

Write-Host "==========================="
Write-Host "¡Fresh Start listo! Limpieza exitosa." -ForegroundColor Cyan
