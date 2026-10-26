# AMIGEP — Aplicación Móvil Inteligente de Gestión y Entrenamiento Personalizado para Gimnasios

Implementación del diseño del documento **APF2** (UTP · Curso Integrador I): app móvil para **clientes y entrenadores**,
panel web para el **administrador**, y un **backend Spring Boot + PostgreSQL** con la API REST que ambos consumen
(sección 8.7: *"ambos consumen la misma API REST y base de datos"*). Piloto: **Angels GYM**.

```
amigep/
├── backend/            API REST Spring Boot 3.5 · Java 17 · PostgreSQL · módulo inteligente
│   └── src/main/resources/
│       ├── db/migration/   V1 = script SQL del documento (sección 11.3) · V2 = catálogo de planes
│       └── static/         Panel web del administrador (HTML/CSS/JS, lo sirve el mismo backend)
├── movil/              App Android nativa (Java) · cliente y entrenador
├── docs/capturas-panel Capturas del panel web con los datos de prueba
└── docker-compose.yml  PostgreSQL 16 para desarrollo (opcional)
```

## Arquitectura

```
 App Android (Java)            Panel web (navegador)
 cliente · entrenador          administrador
        │  Retrofit + JWT              │  fetch + JWT
        └──────────────┬───────────────┘
                       ▼
         API REST  /api/**  (Spring Boot)
   Seguridad: JWT + roles CLIENTE / ENTRENADOR / ADMINISTRADOR
   Servicios ─ Módulo inteligente (rutinas, dieta, abandono)
   JPA / Hibernate ─ Flyway
                       ▼
                  PostgreSQL
```

* **Arquitectura en capas** (sección 14.2): `controlador` → `servicio` → `repositorio` (Spring Data JPA, equivalente al DAO) → PostgreSQL.
* Las **tablas, columnas y restricciones** son exactamente las del modelo físico del documento (14 entidades).
* Contraseñas con **BCrypt**; cada endpoint verifica el rol (`/api/cliente/**`, `/api/entrenador/**`, `/api/admin/**`).
  El entrenador solo accede a los clientes con su `idEntrenador` y el cliente solo a sus propios datos.

## Requisitos

| Componente | Versión |
|---|---|
| JDK | 17 o superior |
| Maven | 3.9+ (o el de IntelliJ / NetBeans) |
| PostgreSQL | 14+ (o Docker) |
| Android Studio | Ladybug o superior · SDK 35 · emulador o celular con Android 7.0+ |

## 1. Base de datos

Con Docker:
```bash
docker compose up -d
```
O en un PostgreSQL instalado (psql / pgAdmin):
```sql
CREATE USER amigep WITH PASSWORD 'amigep';
CREATE DATABASE amigep OWNER amigep;
```
No hace falta ejecutar scripts: al iniciar, **Flyway** crea las tablas con el script del documento.

## 2. Backend + panel web

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run  (o "mvn spring-boot:run" si tienes Maven)
```
* Panel web del administrador: **http://localhost:8080**
* Documentación interactiva de la API (Swagger): **http://localhost:8080/swagger-ui.html**

Variables de entorno opcionales: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`,
`DATOS_DEMO` (`true` por defecto). Con la base vacía y `DATOS_DEMO=true` se cargan datos de prueba con ~6 meses de historial
(asistencias, pagos, rutinas, comidas y progreso), se **entrena el modelo de abandono** y se calculan las predicciones.

Pruebas unitarias del módulo inteligente: `./mvnw test`.

## 3. App móvil (Android)

1. Abrir la carpeta `movil/` en Android Studio y esperar la sincronización de Gradle.
2. Con el backend corriendo, ejecutar en el **emulador**: la app usa `http://10.0.2.2:8080/` (la PC vista desde el emulador).
3. En un **celular real** (misma red Wi‑Fi): en el login toca el ícono de servidor (arriba a la derecha) y escribe
   `http://IP-DE-TU-PC:8080/`.

## Credenciales de prueba

| Rol | Dónde | Correo | Contraseña |
|---|---|---|---|
| Administrador | Panel web | `admin@angelsgym.pe` | `Admin123*` |
| Entrenador | App (rol Entrenador) | `carlos.bustamante@angelsgym.pe` (también `andrea.quispe@`, `miguel.torres@`) | `Entrenador123*` |
| Cliente | App (rol Cliente) | `luis.atocsa@amigep.pe` (VIP, con rutina y plan nutricional) | `Cliente123*` |
| Cliente | App | `percy.gonzales@amigep.pe` (membresía por vencer) · `julio.kong@amigep.pe` (riesgo alto) | `Cliente123*` |

Recuperar contraseña: sin servidor de correo, el código de 6 dígitos se muestra en la respuesta y en el log del backend
(`amigep.recuperacion.mostrar-codigo=true`, desactivar en producción).

## Pantallas implementadas

| Prototipo (sección 12) | Implementación |
|---|---|
| C1 Inicio de sesión | `LoginActivity` — selector Cliente / Entrenador, recuperar contraseña, configuración del servidor |
| C2 Inicio | `InicioFragment` — QR personal (ZXing), plan vigente, accesos rápidos, sesión de hoy, notificaciones |
| C3 Entrenamiento IA | `EntrenoFragment` + `RutinaActivity` — objetivos, ejercicios recomendados, generar rutina (pide nivel/peso/altura si faltan, BPM Fig. 9), marcar sesión |
| C4 Membresías y pagos | `PagosFragment` — plan actual, planes, historial de pagos, renovar / cambiar plan (pago en recepción) |
| C5 Alimentación IA | `GuiaFragment` + `RegistrarComidaActivity` — comidas recomendadas, macros del día, sugerencia IA, registrar comida con evaluación |
| C6 Perfil y progreso | `PerfilFragment` + `EditarPerfilActivity` — datos, entrenador, estadísticas, mediciones, reportes RP-06/07/08, contraseña |
| E1 Mis clientes | `ClientesFragment` — búsqueda y filtros Vigente / Por finalizar / Pendiente |
| E2 Detalle de cliente | `DetalleClienteActivity` + `EditorRutinaActivity` + `AlimentacionClienteActivity` |
| Rutinas · Agenda (mapa 12.3) | `RutinasFragment`, `AgendaFragment` (asistencias de sus clientes), `PerfilEntrenadorFragment` |
| A1 Panel general | `#/panel` — KPIs, membresías por vencer (RP-02), pagos por método (RP-03), rutinas por estado, ingresos de hoy |
| A2 Clientes y membresías | `#/clientes` — búsqueda por nombre/código/DNI, filtros, alta/edición, ficha con QR, pagos y asistencias |
| A3 Retención | `#/retencion` — predicciones, factores de riesgo, acciones y notificaciones, reentrenar el modelo |
| A4 Reportes | `#/reportes` — RP-01 a RP-05 y RP-08 con filtros, exportar CSV e imprimir |
| Pagos · Entrenadores · Configuración (mapa 12.3) | `#/pagos`, `#/membresias`, `#/entrenadores`, `#/configuracion` (usuarios y roles, planes, cuenta) |

## Trazabilidad con los requerimientos

| RF | Backend (API) | Panel web | App |
|---|---|---|---|
| RF-01 Autenticación | `/api/auth/login`, `/recuperar`, `/restablecer` | Login | C1 |
| RF-02 Usuarios y roles | `/api/admin/trabajadores` | Configuración | — |
| RF-03 Clientes | `/api/admin/clientes` · `/api/cliente/perfil` | A2 | C6 |
| RF-04 Membresías | `/api/admin/membresias`, `/api/cliente/membresia` | Membresías | C2, C4 |
| RF-05 Pagos | `/api/admin/pagos` | Pagos, ficha del cliente | C4 |
| RF-06 Asistencias | `/api/admin/accesos` · `/api/entrenador/agenda` | Control de acceso, A1 | E2, Agenda |
| RF-07 Entrenadores | `/api/admin/trabajadores?rol=ENTRENADOR` | Entrenadores | — |
| RF-08 / RF-09 Rutinas | `/api/entrenador/clientes/{id}/rutinas`, `/api/cliente/rutinas/{id}/sesiones` | RP-05 | C3, E2, Editor |
| RF-10 / RF-11 Personalización IA | `/api/cliente/rutinas/generar` (`GeneradorRutinas`) | — | C3 |
| RF-12 / RF-13 Alimentación IA | `/api/cliente/alimentacion/**` (`GeneradorPlanAlimenticio`) | — | C5 |
| RF-14 Progreso | `/api/cliente/progreso` | — | C6, E2 |
| RF-15 Dashboard | `/api/admin/dashboard`, `/api/cliente/inicio` | A1 | C2 |
| RF-16 Consulta por permisos | control de rol en cada endpoint | todas | todas |
| RF-17 Acceso QR | `/api/admin/accesos/qr`, `/manual` | Control de acceso (cámara, lector USB o DNI) | QR en C2 |
| RF-18 Predicción de abandono | `/api/admin/retencion/**` (`ModeloAbandono`) · tarea diaria 6:00 | A3 | — |
| RF-19 Notificaciones | `NotificacionServicio` · avisos de vencimiento diarios 8:00 | A3 | Notificaciones |
| RF-20 Reportes | `/api/reportes/{RP-01..RP-08}` (+ `?formato=csv`) | A4 | Reportes en perfil |

## Módulo inteligente (sección 14.2)

* **Rutinas** (`GeneradorRutinas`): reglas parametrizadas por objetivo (series, repeticiones y descanso), nivel (n.º de
  ejercicios y series) y ritmo (división semanal de 2 a 6 días); con IMC ≥ 30 o edad ≥ 50 elige cardio de bajo impacto.
* **Alimentación** (`GeneradorPlanAlimenticio`): meta calórica con Mifflin‑St Jeor (promedio, el modelo no guarda sexo)
  × factor de actividad ± ajuste por objetivo; macros por kg de peso; compara lo registrado con la meta acumulada (±15 %)
  y devuelve una recomendación de mantenimiento o una alerta con ajuste (BPM Fig. 10). Son **orientativas**.
* **Abandono** (`ModeloAbandono`): **regresión logística** con 7 variables (días sin asistir, caída de asistencia,
  membresía vencida / por vencer, sin rutina, pagos rechazados, baja frecuencia). Se entrena con el historial
  (cortes quincenales de 12 meses, etiqueta = no volvió en 30 días y quedó sin membresía), con clases balanceadas;
  reporta exactitud y sensibilidad y guarda los pesos en `data/modelo-abandono.json`. Riesgo alto ≥ 0.70, medio ≥ 0.40.

## Ajustes respecto al documento

* **Spring Boot + JPA** en lugar de Servlets/JSP + JDBC/DAO (pedido del equipo). La separación en capas se mantiene;
  los repositorios de Spring Data cumplen el rol del DAO.
* **Panel web** como página que consume la misma API REST (en vez de JSP), servida por el mismo backend.
* **`Plan.descripcion`** (migración V2): texto corto que muestran C4 y el panel junto a cada plan.
* **Menú "Control de acceso"** en el panel: pantalla para que recepción escanee el QR (cámara o lector USB) o registre
  por DNI (BPM Fig. 6). En el mapa de navegación no tenía pantalla propia.
* En C6, la barra "Mejora de rendimiento" se muestra como **Asistencia (30 días)**: es un indicador que sí se puede
  calcular con el modelo de datos.
* Una membresía con pago **rechazado** queda con `activo = false` hasta registrar un pago válido (se clasifica como
  *suspendida* en RP-02, como indica la sección 13.2).
