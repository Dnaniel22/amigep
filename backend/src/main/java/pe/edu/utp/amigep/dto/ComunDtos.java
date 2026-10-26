package pe.edu.utp.amigep.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** DTOs compartidos por los tres roles. */
public final class ComunDtos {

    private ComunDtos() {
    }

    public record MensajeDto(String mensaje) {
    }

    public record PlanDto(Integer idPlan, String nombre, Integer duracionDias, BigDecimal precio, String descripcion) {
    }

    /**
     * @param estado VIGENTE, POR_VENCER, VENCIDA, PROGRAMADA o SUSPENDIDA (clasificación del reporte RP-02)
     */
    public record MembresiaDto(Integer idSuscripcion, Integer idPlan, String plan, BigDecimal precio,
                               LocalDate fechaInicio, LocalDate fechaCaducidad, boolean activo,
                               String estado, long diasRestantes) {
    }

    public record PagoDto(Integer idPago, Integer idSuscripcion, Integer idCliente, String cliente, String plan,
                          BigDecimal monto, LocalDate fechaPago, String metodoPago, String estado) {
    }

    public record AccesoDto(Integer idAcceso, Integer idCliente, String codigoCliente, String cliente,
                            LocalDateTime fechaHora, String tipo) {
    }

    public record NotificacionDto(Integer idNotificacion, Integer idCliente, String cliente, String titulo,
                                  String mensaje, LocalDateTime fechaEnvio, boolean leido, String tipo) {
    }

    public record EntrenadorResumenDto(Integer idTrabajador, String nombre, String iniciales, String especialidad) {
    }

    public record RutinaDto(Integer idRutina, Integer idCliente, String cliente, String nombre, String objetivo,
                            String nivel, String ritmo, String origen, String entrenador,
                            LocalDate fechaCreacion, LocalDate fechaFinalizacion, boolean activo,
                            int semanaActual, int semanasTotales, long sesionesCompletadas,
                            int cumplimientoPct, String estado, Map<String, Object> contenido) {
    }

    public record SesionDto(Integer idSesion, LocalDate fecha, boolean completada, String observaciones) {
    }

    public record ProgresoDto(Integer idProgreso, LocalDate fecha, BigDecimal peso, BigDecimal grasaCorporal,
                              Map<String, Object> medidas, String observaciones) {
    }

    /** Estadísticas de progreso que muestran C6 y E2 (RF-14). */
    public record EstadisticasProgresoDto(BigDecimal pesoActual, BigDecimal pesoInicial, Double variacionPesoPct,
                                          int asistencias30, int asistencia30Pct,
                                          long sesionesCompletadas, int cumplimientoRutinasPct,
                                          List<ProgresoDto> mediciones) {
    }

    public record RegistroComidaDto(Integer idRegistro, LocalDate fecha, String tipoComida, String descripcion,
                                    Integer calorias, BigDecimal proteinas, BigDecimal carbohidratos,
                                    BigDecimal grasas) {
    }

    public record PlanAlimenticioDto(Integer idPlanAlimenticio, String objetivo, String ritmo,
                                     Integer caloriasObjetivo, Map<String, Object> contenido,
                                     LocalDate fechaCreacion) {
    }

    /** Macros del día: porcentajes calculados sobre las calorías aportadas por cada macronutriente. */
    public record ResumenNutricionalDto(LocalDate fecha, int calorias, Integer caloriasObjetivo,
                                        int pctMetaCalorica, double proteinas, double carbohidratos, double grasas,
                                        int pctProteinas, int pctCarbohidratos, int pctGrasas, int comidasRegistradas) {
    }

    /** Respuesta genérica de reportes RP-01..RP-08: columnas + filas + totales. */
    public record ReporteDto(String codigo, String titulo, String descripcion, LocalDateTime generado,
                             Map<String, Object> filtros, List<ColumnaDto> columnas,
                             List<Map<String, Object>> filas, Map<String, Object> resumen) {
    }

    /** tipo: texto, numero, moneda, fecha, fechaHora, porcentaje, estado */
    public record ColumnaDto(String clave, String titulo, String tipo) {
    }
}
