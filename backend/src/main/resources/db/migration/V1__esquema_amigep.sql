-- =====================================================================
-- AMIGEP - Modelo físico (documento APF2, sección 11.3)
-- Script original del documento, sin cambios. Flyway lo ejecuta una sola
-- vez al iniciar el backend sobre una base de datos vacía.
-- Clientes y trabajadores no se eliminan físicamente (baja lógica: activo).
-- =====================================================================

CREATE TABLE Trabajador (
    idTrabajador    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(50)  NOT NULL,
    apellido        VARCHAR(50)  NOT NULL,
    dni             VARCHAR(10)  NOT NULL UNIQUE,
    email           VARCHAR(100) NOT NULL UNIQUE,
    contrasenaHash  VARCHAR(255) NOT NULL,
    rol             VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMINISTRADOR','ENTRENADOR')),
    especialidad    VARCHAR(100),
    activo          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE Cliente (
    idCliente        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idEntrenador     INTEGER,
    nombre           VARCHAR(50)  NOT NULL,
    apellido         VARCHAR(50)  NOT NULL,
    dni              VARCHAR(10)  NOT NULL UNIQUE,
    email            VARCHAR(100) NOT NULL UNIQUE,
    contrasenaHash   VARCHAR(255) NOT NULL,
    telefono         VARCHAR(15),
    codigoQR         VARCHAR(64)  NOT NULL UNIQUE,
    objetivo         VARCHAR(50),
    nivel            VARCHAR(20)  CHECK (nivel IN ('PRINCIPIANTE','INTERMEDIO','AVANZADO')),
    altura           DECIMAL(3,2),
    fechaNacimiento  DATE,
    fechaRegistro    DATE NOT NULL DEFAULT CURRENT_DATE,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_cliente_entrenador FOREIGN KEY (idEntrenador)
        REFERENCES Trabajador(idTrabajador) ON UPDATE CASCADE ON DELETE SET NULL
);
CREATE INDEX idx_cliente_entrenador ON Cliente(idEntrenador);

CREATE TABLE Plan (
    idPlan        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre        VARCHAR(50) NOT NULL,
    duracionDias  INTEGER NOT NULL CHECK (duracionDias > 0),
    precio        DECIMAL(10,2) NOT NULL CHECK (precio >= 0)
);

CREATE TABLE Suscripcion (
    idSuscripcion   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idCliente       INTEGER NOT NULL,
    idPlan          INTEGER NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    fechaInicio     DATE NOT NULL,
    fechaCaducidad  DATE NOT NULL,
    CONSTRAINT ck_suscripcion_fechas CHECK (fechaCaducidad > fechaInicio),
    CONSTRAINT fk_suscripcion_cliente FOREIGN KEY (idCliente)
        REFERENCES Cliente(idCliente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_suscripcion_plan FOREIGN KEY (idPlan)
        REFERENCES Plan(idPlan) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE INDEX idx_suscripcion_cliente ON Suscripcion(idCliente);
CREATE INDEX idx_suscripcion_plan    ON Suscripcion(idPlan);

CREATE TABLE Pago (
    idPago         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idSuscripcion  INTEGER NOT NULL,
    monto          DECIMAL(10,2) NOT NULL CHECK (monto > 0),
    fechaPago      DATE NOT NULL,
    metodoPago     VARCHAR(20) NOT NULL CHECK (metodoPago IN ('EFECTIVO','TARJETA','YAPE_PLIN')),
    estado         VARCHAR(20) NOT NULL CHECK (estado IN ('VALIDADO','RECHAZADO')),
    CONSTRAINT fk_pago_suscripcion FOREIGN KEY (idSuscripcion)
        REFERENCES Suscripcion(idSuscripcion) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE INDEX idx_pago_suscripcion ON Pago(idSuscripcion);

CREATE TABLE Acceso (
    idAcceso   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idCliente  INTEGER NOT NULL,
    fechaHora  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo       VARCHAR(10) NOT NULL CHECK (tipo IN ('QR','MANUAL')),
    CONSTRAINT fk_acceso_cliente FOREIGN KEY (idCliente)
        REFERENCES Cliente(idCliente) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE INDEX idx_acceso_cliente ON Acceso(idCliente);

CREATE TABLE Rutina (
    idRutina           INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idCliente          INTEGER NOT NULL,
    idTrabajador       INTEGER,
    objetivo           VARCHAR(50) NOT NULL,
    nivel              VARCHAR(20) NOT NULL CHECK (nivel IN ('PRINCIPIANTE','INTERMEDIO','AVANZADO')),
    ritmo              VARCHAR(50) NOT NULL,
    contenido          JSONB,
    origen             VARCHAR(12) NOT NULL CHECK (origen IN ('IA','ENTRENADOR')),
    fechaCreacion      DATE NOT NULL DEFAULT CURRENT_DATE,
    fechaFinalizacion  DATE,
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_rutina_cliente FOREIGN KEY (idCliente)
        REFERENCES Cliente(idCliente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_rutina_trabajador FOREIGN KEY (idTrabajador)
        REFERENCES Trabajador(idTrabajador) ON UPDATE CASCADE ON DELETE SET NULL
);
CREATE INDEX idx_rutina_cliente    ON Rutina(idCliente);
CREATE INDEX idx_rutina_trabajador ON Rutina(idTrabajador);

CREATE TABLE SesionEntrenamiento (
    idSesion       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idRutina       INTEGER NOT NULL,
    fecha          DATE NOT NULL,
    completada     BOOLEAN NOT NULL DEFAULT FALSE,
    observaciones  TEXT,
    CONSTRAINT fk_sesion_rutina FOREIGN KEY (idRutina)
        REFERENCES Rutina(idRutina) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE INDEX idx_sesion_rutina ON SesionEntrenamiento(idRutina);

CREATE TABLE PlanAlimenticio (
    idPlanAlimenticio  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idCliente          INTEGER NOT NULL,
    objetivo           VARCHAR(50) NOT NULL,
    ritmo              VARCHAR(50) NOT NULL,
    caloriasObjetivo   INTEGER CHECK (caloriasObjetivo > 0),
    contenido          JSONB,
    fechaCreacion      DATE NOT NULL DEFAULT CURRENT_DATE,
    fechaFinalizacion  DATE,
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_planalim_cliente FOREIGN KEY (idCliente)
        REFERENCES Cliente(idCliente) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE INDEX idx_planalim_cliente ON PlanAlimenticio(idCliente);

CREATE TABLE RegistroComida (
    idRegistro     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idCliente      INTEGER NOT NULL,
    fecha          DATE NOT NULL,
    tipoComida     VARCHAR(20) NOT NULL CHECK (tipoComida IN ('DESAYUNO','ALMUERZO','CENA','REFRIGERIO')),
    descripcion    VARCHAR(200) NOT NULL,
    calorias       INTEGER CHECK (calorias >= 0),
    proteinas      DECIMAL(6,2),
    carbohidratos  DECIMAL(6,2),
    grasas         DECIMAL(6,2),
    CONSTRAINT fk_comida_cliente FOREIGN KEY (idCliente)
        REFERENCES Cliente(idCliente) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE INDEX idx_comida_cliente ON RegistroComida(idCliente);

CREATE TABLE Progreso (
    idProgreso     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idCliente      INTEGER NOT NULL,
    fecha          DATE NOT NULL,
    peso           DECIMAL(5,2),
    grasaCorporal  DECIMAL(5,2),
    medidas        JSONB,
    observaciones  TEXT,
    CONSTRAINT fk_progreso_cliente FOREIGN KEY (idCliente)
        REFERENCES Cliente(idCliente) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE INDEX idx_progreso_cliente ON Progreso(idCliente);

CREATE TABLE Prediccion (
    idPrediccion     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idCliente        INTEGER NOT NULL,
    fechaPrediccion  DATE NOT NULL,
    probabilidad     DECIMAL(4,3) NOT NULL CHECK (probabilidad BETWEEN 0 AND 1),
    factoresRiesgo   JSONB,
    estado           VARCHAR(10) NOT NULL CHECK (estado IN ('ALTO','MEDIO','BAJO')),
    CONSTRAINT fk_prediccion_cliente FOREIGN KEY (idCliente)
        REFERENCES Cliente(idCliente) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE INDEX idx_prediccion_cliente ON Prediccion(idCliente);

CREATE TABLE AccionRetencion (
    idAccion      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idPrediccion  INTEGER NOT NULL,
    idTrabajador  INTEGER NOT NULL,
    tipoAccion    VARCHAR(100) NOT NULL,
    descripcion   TEXT,
    fechaAccion   DATE NOT NULL,
    resultado     VARCHAR(50),
    CONSTRAINT fk_accion_prediccion FOREIGN KEY (idPrediccion)
        REFERENCES Prediccion(idPrediccion) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_accion_trabajador FOREIGN KEY (idTrabajador)
        REFERENCES Trabajador(idTrabajador) ON UPDATE CASCADE ON DELETE RESTRICT
);
CREATE INDEX idx_accion_prediccion ON AccionRetencion(idPrediccion);
CREATE INDEX idx_accion_trabajador ON AccionRetencion(idTrabajador);

CREATE TABLE Notificacion (
    idNotificacion  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idCliente       INTEGER NOT NULL,
    idAccion        INTEGER,
    titulo          VARCHAR(100) NOT NULL,
    mensaje         TEXT,
    fechaEnvio      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leido           BOOLEAN NOT NULL DEFAULT FALSE,
    tipo            VARCHAR(30) NOT NULL CHECK (tipo IN ('VENCIMIENTO','RUTINA','RETENCION')),
    CONSTRAINT fk_notif_cliente FOREIGN KEY (idCliente)
        REFERENCES Cliente(idCliente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_notif_accion FOREIGN KEY (idAccion)
        REFERENCES AccionRetencion(idAccion) ON UPDATE CASCADE ON DELETE SET NULL
);
CREATE INDEX idx_notif_cliente ON Notificacion(idCliente);
CREATE INDEX idx_notif_accion  ON Notificacion(idAccion);
