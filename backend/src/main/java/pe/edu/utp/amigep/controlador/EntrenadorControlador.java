package pe.edu.utp.amigep.controlador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.amigep.dto.ComunDtos.AccesoDto;
import pe.edu.utp.amigep.dto.ComunDtos.RutinaDto;
import pe.edu.utp.amigep.dto.EntrenadorDtos.*;
import pe.edu.utp.amigep.seguridad.UsuarioAutenticado;
import pe.edu.utp.amigep.servicio.AlimentacionServicio;
import pe.edu.utp.amigep.servicio.EntrenadorServicio;
import pe.edu.utp.amigep.servicio.RutinaServicio;

import java.time.LocalDate;
import java.util.List;

/** App móvil · rol ENTRENADOR. Solo accede a los clientes que tiene asignados. */
@Tag(name = "App · Entrenador")
@RestController
@RequestMapping("/api/entrenador")
@RequiredArgsConstructor
public class EntrenadorControlador {

    private final EntrenadorServicio entrenador;
    private final RutinaServicio rutinas;
    private final AlimentacionServicio alimentacion;

    /** E1 - Mis clientes (filtros: VIGENTE, POR_FINALIZAR, PENDIENTE). */
    @GetMapping("/clientes")
    public ResumenClientesDto clientes(@AuthenticationPrincipal UsuarioAutenticado u,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) String estado) {
        return entrenador.clientes(u.id(), q, estado);
    }

    /** E2 - Detalle de cliente y rutina. */
    @GetMapping("/clientes/{idCliente}")
    public DetalleClienteDto detalle(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer idCliente) {
        return entrenador.detalle(u.id(), idCliente);
    }

    @GetMapping("/clientes/{idCliente}/rutinas")
    public List<RutinaDto> rutinasDeCliente(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer idCliente) {
        return rutinas.rutinasDeClienteParaEntrenador(u.id(), idCliente);
    }

    /** Asignar nueva rutina (desactiva la anterior y notifica al cliente). */
    @PostMapping("/clientes/{idCliente}/rutinas")
    public RutinaDto crearRutina(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer idCliente,
                                 @Valid @RequestBody GuardarRutinaRequest req) {
        return rutinas.guardarDeEntrenador(u.id(), idCliente, null, req);
    }

    /** "Actualizar rutina" (E2). */
    @PutMapping("/clientes/{idCliente}/rutinas/{idRutina}")
    public RutinaDto actualizarRutina(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer idCliente,
                                      @PathVariable Integer idRutina, @Valid @RequestBody GuardarRutinaRequest req) {
        return rutinas.guardarDeEntrenador(u.id(), idCliente, idRutina, req);
    }

    /** "Ver alimentación" (consulta). */
    @GetMapping("/clientes/{idCliente}/alimentacion")
    public AlimentacionClienteDto alimentacion(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer idCliente) {
        return alimentacion.paraEntrenador(u.id(), idCliente);
    }

    @GetMapping("/rutinas")
    public List<RutinaDto> rutinas(@AuthenticationPrincipal UsuarioAutenticado u) {
        return rutinas.rutinasDeEntrenador(u.id());
    }

    /** Agenda: asistencias de sus clientes (por defecto, últimos 7 días). */
    @GetMapping("/agenda")
    public List<AccesoDto> agenda(@AuthenticationPrincipal UsuarioAutenticado u,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return entrenador.agenda(u.id(), desde, hasta);
    }

    @GetMapping("/perfil")
    public PerfilEntrenadorDto perfil(@AuthenticationPrincipal UsuarioAutenticado u) {
        return entrenador.perfil(u.id());
    }
}
