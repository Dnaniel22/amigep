package pe.edu.utp.amigep.dto;

import jakarta.validation.constraints.*;
import pe.edu.utp.amigep.dto.ComunDtos.*;
import pe.edu.utp.amigep.modelo.enums.Nivel;
import pe.edu.utp.amigep.modelo.enums.TipoComida;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Pantallas del cliente en la app móvil (C2 a C6). */
public final class ClienteAppDtos {

    private ClienteAppDtos() {
    }

    /** C2 - Inicio: QR personal, plan vigente y sesión del día. */
    public record InicioClienteDto(Integer idCliente, String nombre, String apellido, String iniciales,
                                   String codigoQR, MembresiaDto membresia, SesionHoyDto sesionHoy,
                                   long notificacionesNoLeidas, int asistenciasMes) {
    }

    public record SesionHoyDto(Integer idRutina, int dia, String titulo, String detalle, String enfoque,
                               List<Map<String, Object>> ejercicios, boolean completada) {
    }

    /** C6 - Perfil y progreso. */
    public record PerfilClienteDto(Integer idCliente, String codigo, String nombre, String apellido,
                                   String iniciales, String email, String telefono, String dni,
                                   String objetivo, String nivel, BigDecimal altura, BigDecimal peso,
                                   LocalDate fechaNacimiento, LocalDate fechaRegistro, String plan,
                                   EntrenadorResumenDto entrenador, EstadisticasProgresoDto estadisticas) {
    }

    public record ActualizarPerfilRequest(
            @Size(max = 15, message = "Máximo 15 caracteres") @Pattern(regexp = "^[0-9+ ]*$", message = "Teléfono no válido") String telefono,
            @Size(max = 50) String objetivo,
            Nivel nivel,
            @DecimalMin(value = "1.00", message = "Altura en metros, p. ej. 1.75") @DecimalMax(value = "2.50", message = "Altura en metros, p. ej. 1.75") BigDecimal altura,
            @Past(message = "Fecha de nacimiento no válida") LocalDate fechaNacimiento,
            @DecimalMin(value = "25", message = "Peso no válido") @DecimalMax(value = "350", message = "Peso no válido") BigDecimal peso) {
    }

    /** C4 - Membresías y pagos. */
    public record MembresiaClienteDto(MembresiaDto actual, MembresiaDto proxima, List<PlanDto> planes,
                                      List<PagoDto> historialPagos, String formaPago) {
    }

    /** C3 - Generar rutina con IA (BPM Fig. 9). Los datos faltantes se completan aquí. */
    public record GenerarRutinaRequest(
            @NotBlank(message = "Seleccione un objetivo") String objetivo,
            Nivel nivel,
            @Min(value = 2, message = "Mínimo 2 sesiones por semana") @Max(value = 6, message = "Máximo 6 sesiones por semana") Integer sesionesPorSemana,
            @DecimalMin(value = "25", message = "Peso no válido") @DecimalMax(value = "350", message = "Peso no válido") BigDecimal peso,
            @DecimalMin(value = "1.00", message = "Altura en metros") @DecimalMax(value = "2.50", message = "Altura en metros") BigDecimal altura) {
    }

    public record RegistrarSesionRequest(Boolean completada, @Size(max = 500) String observaciones) {
    }

    /** C5 - Alimentación IA. */
    public record AlimentacionDto(PlanAlimenticioDto plan, ResumenNutricionalDto hoy, String sugerencia,
                                  List<RegistroComidaDto> registrosHoy, String aviso) {
    }

    public record GenerarPlanAlimenticioRequest(String objetivo) {
    }

    public record RegistrarComidaRequest(
            @NotNull(message = "Seleccione el tipo de comida") TipoComida tipoComida,
            @NotBlank(message = "Describa la comida") @Size(max = 200) String descripcion,
            @NotNull(message = "Ingrese las calorías") @PositiveOrZero @Max(value = 5000, message = "Valor no válido") Integer calorias,
            @PositiveOrZero BigDecimal proteinas,
            @PositiveOrZero BigDecimal carbohidratos,
            @PositiveOrZero BigDecimal grasas,
            @PastOrPresent LocalDate fecha) {
    }

    /** Resultado de comparar el consumo con la meta del plan (BPM Fig. 10). */
    public record EvaluacionComidaDto(RegistroComidaDto registro, boolean cumpleObjetivo, String mensaje,
                                      ResumenNutricionalDto hoy) {
    }

    public record RegistrarProgresoRequest(
            @NotNull(message = "Ingrese su peso") @DecimalMin(value = "25", message = "Peso no válido") @DecimalMax(value = "350", message = "Peso no válido") BigDecimal peso,
            @DecimalMin("2") @DecimalMax("70") BigDecimal grasaCorporal,
            Map<String, Object> medidas,
            @Size(max = 500) String observaciones) {
    }

    public record VistaPreviaRutinaDto(String nombre, String objetivo, String nivel, String ritmo,
                                       Map<String, Object> contenido) {
    }

    public record RutinasClienteDto(RutinaDto activa, List<RutinaDto> anteriores,
                                    List<SesionDto> sesionesRecientes) {
    }
}
