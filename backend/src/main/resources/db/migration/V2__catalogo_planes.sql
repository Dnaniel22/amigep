-- =====================================================================
-- AMIGEP - Ajuste mínimo sobre el modelo del documento + datos maestros
-- Plan.descripcion: texto corto que muestran las pantallas C4 (app) y
-- el panel web junto a cada plan ("Acceso al gimnasio · Control de asistencia").
-- =====================================================================

ALTER TABLE Plan ADD COLUMN descripcion VARCHAR(200);

INSERT INTO Plan (nombre, duracionDias, precio, descripcion) VALUES
    ('Básico',   30,  79.00, 'Acceso al gimnasio · Control de asistencia'),
    ('Estándar', 30, 129.00, 'Acceso completo · Seguimiento de progreso'),
    ('VIP',      30, 179.00, 'Rutinas y dieta con IA · Soporte prioritario');
