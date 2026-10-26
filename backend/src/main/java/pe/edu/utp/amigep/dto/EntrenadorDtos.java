package pe.edu.utp.amigep.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pe.edu.utp.amigep.dto.ComunDtos.*;
import pe.edu.utp.amigep.modelo.enums.Nivel;

import java.math.BigDecimal;
import java.util.List;

/** Pantallas del entrenador en la app móvil (E1, E2, Rutinas, Agenda). */
public final class EntrenadorDtos {

    private EntrenadorDtos() {
    }

    /** E1 - fila de "Mis clientes". estadoRutina: VIGENTE, POR_FINALIZAR o PENDIENTE. */
    public record ClienteAsignadoDto(Integer idCliente, String nombre, String apellido, String iniciales,
                                     String objetivo, String nivel, String rutina, int semanaActual,
                                     int semanasTotales, String estadoRutina, String detalle) {
    }

    public record ResumenClientesDto(int total, int vigentes, int porFinalizar, int pendientes,
                                     List<ClienteAsignadoDto> clientes) {
    }

    /** E2 - detalle de cliente y rutina. */
    public record DetalleClienteDto(Integer idCliente, String nombre, String apellido, String iniciales,
                                    String email, String telefono, String objetivo, String nivel,
                                    BigDecimal altura, Integer edad, MembresiaDto membresia,
                                    RutinaDto rutinaActiva, BigDecimal pesoActual, Double variacionPesoPct,
                                    int asistenciasMes, int diasMes, List<AccesoDto> ultimasAsistencias,
                                    EstadisticasProgresoDto estadisticas) {
    }

    /** Crear o actualizar rutina (BPM Fig. 8: define ejercicios, series y repeticiones). */
    public record GuardarRutinaRequest(
            @NotBlank(message = "Ingrese un nombre para la rutina") @Size(max = 60) String nombre,
            @NotBlank(message = "Seleccione el objetivo") String objetivo,
            @NotNull(message = "Seleccione el nivel") Nivel nivel,
            @NotNull @Min(value = 1, message = "Mínimo 1 sesión") @Max(value = 7, message = "Máximo 7 sesiones") Integer sesionesPorSemana,
            @NotNull @Min(value = 1, message = "Mínimo 1 semana") @Max(value = 24, message = "Máximo 24 semanas") Integer semanas,
            @NotEmpty(message = "Agregue al menos un día de entrenamiento") @Valid List<DiaRutinaDto> dias,
            List<String> notas) {
    }

    public record DiaRutinaDto(
            @NotBlank(message = "Ingrese el nombre del día") String nombre,
            String enfoque,
            @NotEmpty(message = "Cada día necesita al menos un ejercicio") @Valid List<EjercicioDto> ejercicios) {
    }

    public record EjercicioDto(
            @NotBlank(message = "Ingrese el ejercicio") String nombre,
            @NotNull @Min(value = 1, message = "Series no válidas") @Max(value = 10, message = "Series no válidas") Integer series,
            @NotBlank(message = "Ingrese las repeticiones") String repeticiones,
            @Min(0) @Max(600) Integer descansoSeg) {
    }

    /** "Ver alimentación" desde el detalle del cliente (consulta, RF-12). */
    public record AlimentacionClienteDto(pe.edu.utp.amigep.dto.ClienteAppDtos.AlimentacionDto hoy,
                                         List<RegistroComidaDto> ultimos7Dias, int diasConRegistro) {
    }

    public record PerfilEntrenadorDto(Integer idTrabajador, String nombre, String apellido, String iniciales,
                                      String email, String especialidad, int clientesAsignados,
                                      int rutinasActivas) {
    }
}
