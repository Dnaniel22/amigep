package pe.edu.utp.amigep.dto;

import jakarta.validation.constraints.*;
import pe.edu.utp.amigep.dto.ComunDtos.*;
import pe.edu.utp.amigep.modelo.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Panel web del administrador (A1 a A4 y módulos del menú lateral). */
public final class AdminDtos {

    private AdminDtos() {
    }

    /** A1 - Panel general. */
    public record DashboardDto(long clientesActivos, long nuevosEsteMes, long membresiasPorVencer,
                               BigDecimal pagosMes, Double variacionPagosPct, long asistenciasHoy,
                               long clientesConMembresia, List<VencimientoDto> proximasAVencer,
                               List<PagoMetodoDto> pagosPorMetodo, List<ConteoDto> rutinasPorEstado) {
    }

    public record VencimientoDto(Integer idCliente, Integer idSuscripcion, String cliente, String plan,
                                 LocalDate vence, long diasRestantes) {
    }

    public record PagoMetodoDto(String metodo, String etiqueta, BigDecimal monto, int porcentaje) {
    }

    public record ConteoDto(String clave, String etiqueta, long cantidad) {
    }

    /** A2 - fila de la tabla de clientes. estadoMembresia: ACTIVO, POR_VENCER, VENCIDO, SIN_MEMBRESIA. */
    public record ClienteFilaDto(Integer idCliente, String codigo, String nombre, String apellido, String email,
                                 String dni, String telefono, Integer idPlan, String plan, LocalDate fechaInicio,
                                 LocalDate fechaCaducidad, String estadoMembresia, boolean activo,
                                 Integer idEntrenador, String entrenador) {
    }

    public record ClienteDetalleDto(ClienteFilaDto resumen, String objetivo, String nivel, BigDecimal altura,
                                    LocalDate fechaNacimiento, LocalDate fechaRegistro, String codigoQR,
                                    List<MembresiaDto> membresias, List<PagoDto> pagos,
                                    List<AccesoDto> ultimosAccesos, String riesgo, Double probabilidadRiesgo) {
    }

    /** Alta / edición de cliente (BPM Fig. 4). La contraseña es obligatoria solo al crear. */
    public record GuardarClienteRequest(
            @NotBlank(message = "Ingrese el nombre") @Size(max = 50) String nombre,
            @NotBlank(message = "Ingrese el apellido") @Size(max = 50) String apellido,
            @NotBlank(message = "Ingrese el DNI") @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 dígitos") String dni,
            @NotBlank(message = "Ingrese el correo") @Email(message = "Correo no válido") @Size(max = 100) String email,
            @Size(max = 15) @Pattern(regexp = "^[0-9+ ]*$", message = "Teléfono no válido") String telefono,
            @Size(max = 50) String objetivo,
            Nivel nivel,
            @DecimalMin(value = "1.00", message = "Altura en metros") @DecimalMax(value = "2.50", message = "Altura en metros") BigDecimal altura,
            @Past(message = "Fecha no válida") LocalDate fechaNacimiento,
            Integer idEntrenador,
            @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String contrasena,
            /* Opcional al crear: registra la primera membresía y su pago. */
            Integer idPlan,
            MetodoPago metodoPago) {
    }

    public record CambiarEstadoRequest(@NotNull Boolean activo) {
    }

    public record TrabajadorDto(Integer idTrabajador, String nombre, String apellido, String iniciales, String dni,
                                String email, String rol, String especialidad, boolean activo,
                                long clientesAsignados) {
    }

    public record GuardarTrabajadorRequest(
            @NotBlank(message = "Ingrese el nombre") @Size(max = 50) String nombre,
            @NotBlank(message = "Ingrese el apellido") @Size(max = 50) String apellido,
            @NotBlank(message = "Ingrese el DNI") @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 dígitos") String dni,
            @NotBlank(message = "Ingrese el correo") @Email(message = "Correo no válido") String email,
            @NotNull(message = "Seleccione el rol") Rol rol,
            @Size(max = 100) String especialidad,
            @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String contrasena) {
    }

    public record GuardarPlanRequest(
            @NotBlank(message = "Ingrese el nombre") @Size(max = 50) String nombre,
            @NotNull @Min(value = 1, message = "Duración no válida") Integer duracionDias,
            @NotNull @DecimalMin(value = "0", message = "Precio no válido") BigDecimal precio,
            @Size(max = 200) String descripcion) {
    }

    /**
     * Pago en recepción (BPM Fig. 5). Si se indica idSuscripcion se reintenta el pago de esa membresía;
     * si no, se crea la renovación con el plan elegido.
     */
    public record RegistrarPagoRequest(
            @NotNull(message = "Seleccione el cliente") Integer idCliente,
            Integer idPlan,
            Integer idSuscripcion,
            @NotNull(message = "Seleccione el método de pago") MetodoPago metodoPago,
            EstadoPago estado,
            @DecimalMin(value = "0.01", message = "Monto no válido") BigDecimal monto) {
    }

    public record ResultadoPagoDto(PagoDto pago, MembresiaDto membresia, String mensaje) {
    }

    /** Fila del módulo Membresías del panel. */
    public record MembresiaFilaDto(Integer idSuscripcion, Integer idCliente, String codigoCliente, String cliente,
                                   Integer idPlan, String plan, LocalDate fechaInicio, LocalDate fechaCaducidad,
                                   String estado, long diasRestantes, boolean pagada, boolean renovada) {
    }

    public record AccesoQrRequest(@NotBlank(message = "Escanee o ingrese el código QR") String codigoQR) {
    }

    public record AccesoManualRequest(@NotBlank @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 dígitos") String dni) {
    }

    /** motivo: OK, YA_REGISTRADO, NO_IDENTIFICADO, CLIENTE_INACTIVO, SIN_MEMBRESIA */
    public record ResultadoAccesoDto(boolean permitido, String motivo, String mensaje, Integer idCliente,
                                     String codigoCliente, String cliente, String iniciales,
                                     MembresiaDto membresia, AccesoDto acceso) {
    }

    /** A3 - Retención. */
    public record PrediccionDto(Integer idPrediccion, Integer idCliente, String cliente, String iniciales,
                                LocalDate fechaPrediccion, BigDecimal probabilidad, List<String> factoresRiesgo,
                                String estado, String estadoMembresia, long acciones) {
    }

    public record ResumenRetencionDto(long riesgoAlto, long riesgoMedio, long riesgoBajo, long accionesMes,
                                      int clientesRetenidosPct, long contactados, long retenidos,
                                      LocalDate ultimaActualizacion, ModeloInfoDto modelo) {
    }

    public record ModeloInfoDto(String tipo, boolean entrenado, int muestras, Double exactitud, Double sensibilidad,
                                String fechaEntrenamiento, java.util.Map<String, Double> pesos) {
    }

    public record AccionRetencionDto(Integer idAccion, Integer idPrediccion, Integer idCliente, String cliente,
                                     String tipoAccion, String descripcion, LocalDate fechaAccion,
                                     String resultado, String trabajador) {
    }

    public record RegistrarAccionRequest(
            @NotNull(message = "Seleccione la predicción / cliente") Integer idPrediccion,
            @NotBlank(message = "Seleccione el tipo de acción") @Size(max = 100) String tipoAccion,
            String descripcion,
            boolean enviarNotificacion,
            @Size(max = 100) String tituloNotificacion,
            String mensajeNotificacion) {
    }

    public record ActualizarResultadoRequest(
            @NotBlank @Pattern(regexp = "PENDIENTE|ENVIADA|RETENIDO|NO_RETENIDO", message = "Resultado no válido") String resultado) {
    }

    public record EnviarNotificacionRequest(
            @NotNull(message = "Seleccione el cliente") Integer idCliente,
            @NotBlank(message = "Ingrese el título") @Size(max = 100) String titulo,
            String mensaje,
            @NotNull TipoNotificacion tipo) {
    }
}
