#!/usr/bin/env sh
# AMIGEP - inicio del backend y del panel web en modo producción (Linux / macOS)
# 1) Generar el ejecutable:  cd backend && ./mvnw clean package
# 2) Copiar despliegue/amigep.env.ejemplo como despliegue/amigep.env y cambiar las claves
# 3) Ejecutar:  sh despliegue/iniciar-produccion.sh
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ ! -f "$DIR/amigep.env" ]; then
  echo "Falta despliegue/amigep.env. Copie amigep.env.ejemplo como amigep.env y complete las claves."
  exit 1
fi
set -a
. "$DIR/amigep.env"
set +a
exec java -jar "$DIR/../backend/target/amigep-backend-1.0.0.jar" --spring.profiles.active=prod
