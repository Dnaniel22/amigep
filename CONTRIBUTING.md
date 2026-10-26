# Guía de trabajo colaborativo — AMIGEP

Equipo del Curso Integrador I: Sistemas Software (UTP · ciclo VI · 2026).

## Roles del equipo

| Integrante | Siglas | Rol | Frente principal en el repositorio |
|---|---|---|---|
| Daniel Piero Franco Herrera | DPFH | Project Manager | Planificación, revisión de pull requests, documentación y releases |
| Luis Fernando Atocsa Tapia | LFAT | Desarrollador backend e integración | `backend/` — controladores, servicios, seguridad e integración |
| Fabricio Ignacio Hernández Alvarado | FIHA | Desarrollador de la app móvil y del panel web | `movil/` y `backend/src/main/resources/static/` |
| Julio Germán Kong San Inciso | JGKSI | Desarrollador del módulo inteligente (IA) | `backend/.../servicio/ia/` y sus pruebas |
| César Siomet Huatangari De La Cruz | CSHDLC | Responsable de base de datos | `backend/src/main/resources/db/migration/`, entidades y repositorios |
| Percy Moises Gonzales Vera | PMGV | Responsable de calidad (QA) | `backend/src/test/`, pruebas funcionales y reporte de incidencias |

## Ramas

| Rama | Uso |
|---|---|
| `main` | Versión estable entregada en cada avance (APF). Solo recibe merges desde `develop`. |
| `develop` | Integración del trabajo del equipo. |
| `feature/<modulo>-<descripcion>` | Una funcionalidad nueva, p. ej. `feature/movil-notificaciones`. |
| `fix/<descripcion>` | Corrección de un error encontrado en pruebas. |
| `docs/<descripcion>` | Cambios solo de documentación. |

Flujo: crear la rama desde `develop` → commits pequeños → *pull request* hacia `develop` con al menos una revisión
de otro integrante → al cerrar cada avance, merge de `develop` a `main` y etiqueta (`apf3`, `apf4`, …).

## Convención de commits

Formato *Conventional Commits* en español:

```
<tipo>(<alcance>): <descripción en imperativo>
```

* **Tipos:** `feat` (funcionalidad), `fix` (corrección), `docs`, `test`, `refactor`, `chore` (configuración).
* **Alcances:** `bd`, `backend`, `seguridad`, `ia`, `api`, `panel`, `movil`, `docs`.
* Ejemplos: `feat(api): agregar endpoint de reportes RP-06`, `fix(movil): corregir validación del login`.

## Antes de abrir un pull request

1. `cd backend && ./mvnw test` sin errores.
2. La app compila en Android Studio (`Build > Make Project`).
3. Describir en el PR qué requerimiento (RF/RNF) atiende y adjuntar captura si cambia una pantalla.
4. No subir contraseñas reales, `local.properties`, `backend/data/` ni carpetas `build/` o `target/`.
