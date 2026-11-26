@echo off
rem AMIGEP - inicio del backend y del panel web en modo produccion (Windows)
rem 1) Generar el ejecutable:  cd backend  y  mvnw.cmd clean package
rem 2) Copiar despliegue\amigep.env.ejemplo como despliegue\amigep.env y cambiar las claves
rem 3) Ejecutar este archivo desde la carpeta raiz del repositorio
setlocal
if not exist "%~dp0amigep.env" (
  echo Falta despliegue\amigep.env. Copie amigep.env.ejemplo como amigep.env y complete las claves.
  exit /b 1
)
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%~dp0amigep.env") do set "%%a=%%b"
java -jar "%~dp0..\backend\target\amigep-backend-1.0.0.jar" --spring.profiles.active=prod
