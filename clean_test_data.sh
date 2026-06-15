#!/bin/bash

# clean_test_data.sh
# Script para limpiar la base de datos, las conversaciones de chat y la documentación cargada.
# Manteniendo la sesión de WhatsApp activa.

# Colores para la consola
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}===================================================================${NC}"
echo -e "${YELLOW}           LAWRABOT - SCRIPT DE LIMPIEZA DE DATOS DE PRUEBA        ${NC}"
echo -e "${YELLOW}===================================================================${NC}"
echo -e "${RED}⚠️  ¡ADVERTENCIA! Este script eliminará de forma permanente:${NC}"
echo -e " - Todos los expedientes, ciudadanos, cónyuges e hijos en la base de datos."
echo -e " - La documentación digital descargada (actas, certificados, etc.)."
echo -e " - El historial conversacional activo de todos los chats de WhatsApp (Redis)."
echo -e "${YELLOW}* La sesión vinculada de WhatsApp (QR) NO se cerrará. *${NC}"
echo -e "${YELLOW}===================================================================${NC}"

# Confirmación de seguridad
read -p "¿Estás seguro de que deseas continuar? (escribe 'si' para confirmar): " CONFIRM
if [ "$CONFIRM" != "si" ]; then
    echo -e "${RED}Operación cancelada.${NC}"
    exit 1
fi

echo -e "\n${YELLOW}[1/4] Limpiando base de datos PostgreSQL...${NC}"
# Cargar variables del .env de forma segura si existe
if [ -f .env ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        # Ignorar comentarios y líneas vacías
        if [[ ! "$line" =~ ^[[:space:]]*# ]] && [[ "$line" =~ = ]]; then
            key=$(echo "$line" | cut -d'=' -f1 | xargs)
            val=$(echo "$line" | cut -d'=' -f2- | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
            export "$key=$val" 2>/dev/null
        fi
    done < .env
fi

DB_USER=${POSTGRES_USER:-postgres}
DB_NAME=${POSTGRES_DB:-lawrabot_db}

docker exec -i lawrabot-db psql -U "$DB_USER" -d "$DB_NAME" -c "TRUNCATE TABLE tasks, spouses, socioeconomic_profiles, signature_appointments, regulatory_agreements, observations, expedientes, digital_evidences, correction_feedback, citizens, children, case_participants RESTART IDENTITY CASCADE;"
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Base de datos limpiada correctamente.${NC}"
else
    echo -e "${RED}✗ Error al limpiar la base de datos.${NC}"
fi

echo -e "\n${YELLOW}[2/4] Limpiando caché y conversaciones en Redis...${NC}"
docker exec -i lawrabot-redis redis-cli FLUSHALL
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Caché y conversaciones de Redis vaciadas.${NC}"
else
    echo -e "${RED}✗ Error al vaciar Redis.${NC}"
fi

echo -e "\n${YELLOW}[3/4] Limpiando archivos de documentación y evidencias...${NC}"
# Borrar media descargada por el agente (dentro del contenedor o volumen)
docker exec -i lawrabot-agent rm -rf /app/.data/media/* 2>/dev/null
# Borrar historial conversacional persistido del agente
docker exec -i lawrabot-agent sh -c "rm -rf /app/.data/sessions/* 2>/dev/null"
# Borrar evidencias guardadas en el mcp server
docker exec -i lawrabot-mcp rm -rf /app/storage/certificates/* /app/storage/evidences/* ./storage/evidences/* 2>/dev/null

echo -e "${GREEN}✓ Archivos temporales de evidencias y certificados eliminados.${NC}"

echo -e "\n${YELLOW}[4/4] Reiniciando servicios para aplicar los cambios...${NC}"
# Reiniciar agente y mcp server para vaciar cualquier caché en memoria
docker restart lawrabot-agent lawrabot-mcp
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Servicios de LawraBot reiniciados con éxito.${NC}"
else
    echo -e "${RED}✗ Error al reiniciar los servicios.${NC}"
fi

echo -e "\n${GREEN}===================================================================${NC}"
echo -e "${GREEN}        ¡Limpieza completada! El sistema está listo para pruebas.   ${NC}"
echo -e "${GREEN}===================================================================${NC}"
