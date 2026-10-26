package pe.edu.utp.amigep.controlador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.amigep.dto.ClienteAppDtos.*;
import pe.edu.utp.amigep.dto.ComunDtos.*;
import pe.edu.utp.amigep.modelo.enums.Nivel;
import pe.edu.utp.amigep.seguridad.UsuarioAutenticado;
import pe.edu.utp.amigep.servicio.*;

import java.util.List;

/** App móvil · rol CLIENTE (pantallas C2 a C6). Cada cliente solo accede a sus propios datos. */
@Tag(name = "App · Cliente")
@RestController
@RequestMapping("/api/cliente")
@RequiredArgsConstructor
public class ClienteAppControlador {

    private final ClienteAppServicio clienteApp;
    private final RutinaServicio rutinas;
    private final AlimentacionServicio alimentacion;
    private final NotificacionServicio notificaciones;

    /** C2 - Inicio: QR, plan vigente, sesión de hoy. */
    @GetMapping("/inicio")
    public InicioClienteDto inicio(@AuthenticationPrincipal UsuarioAutenticado u) {
        return clienteApp.inicio(u.id());
    }

    /** C6 - Perfil y progreso. */
    @GetMapping("/perfil")
    public PerfilClienteDto perfil(@AuthenticationPrincipal UsuarioAutenticado u) {
        return clienteApp.perfil(u.id());
    }

    @PutMapping("/perfil")
    public PerfilClienteDto actualizarPerfil(@AuthenticationPrincipal UsuarioAutenticado u,
                                             @Valid @RequestBody ActualizarPerfilRequest req) {
        return clienteApp.actualizarPerfil(u.id(), req);
    }

    /** C4 - Membresías y pagos. */
    @GetMapping("/membresia")
    public MembresiaClienteDto membresia(@AuthenticationPrincipal UsuarioAutenticado u) {
        return clienteApp.membresia(u.id());
    }

    /** C3 - Entrenamiento: rutina activa, anteriores y sesiones. */
    @GetMapping("/rutinas")
    public RutinasClienteDto rutinas(@AuthenticationPrincipal UsuarioAutenticado u) {
        return rutinas.rutinasDeCliente(u.id());
    }

    /** C3 - Ejercicios recomendados para el objetivo seleccionado (sin guardar). */
    @GetMapping("/rutinas/vista-previa")
    public VistaPreviaRutinaDto vistaPrevia(@AuthenticationPrincipal UsuarioAutenticado u,
                                            @RequestParam String objetivo,
                                            @RequestParam(required = false) Nivel nivel,
                                            @RequestParam(required = false) Integer sesionesPorSemana) {
        return rutinas.vistaPrevia(u.id(), objetivo, nivel, sesionesPorSemana);
    }

    /** C3 - "Generar rutina personalizada" (BPM Fig. 9). */
    @PostMapping("/rutinas/generar")
    public RutinaDto generarRutina(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody GenerarRutinaRequest req) {
        return rutinas.generarConIa(u.id(), req);
    }

    @PostMapping("/rutinas/{idRutina}/sesiones")
    public SesionDto registrarSesion(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer idRutina,
                                     @Valid @RequestBody(required = false) RegistrarSesionRequest req) {
        return rutinas.registrarSesion(u.id(), idRutina, req);
    }

    /** C5 - Alimentación IA. */
    @GetMapping("/alimentacion")
    public AlimentacionDto alimentacion(@AuthenticationPrincipal UsuarioAutenticado u) {
        return alimentacion.alimentacion(u.id());
    }

    @PostMapping("/alimentacion/generar")
    public AlimentacionDto generarPlan(@AuthenticationPrincipal UsuarioAutenticado u,
                                       @RequestBody(required = false) GenerarPlanAlimenticioRequest req) {
        return alimentacion.generarPlan(u.id(), req == null ? null : req.objetivo());
    }

    @PostMapping("/alimentacion/comidas")
    public EvaluacionComidaDto registrarComida(@AuthenticationPrincipal UsuarioAutenticado u,
                                               @Valid @RequestBody RegistrarComidaRequest req) {
        return alimentacion.registrarComida(u.id(), req);
    }

    @GetMapping("/progreso")
    public EstadisticasProgresoDto progreso(@AuthenticationPrincipal UsuarioAutenticado u) {
        return clienteApp.progreso(u.id());
    }

    @PostMapping("/progreso")
    public EstadisticasProgresoDto registrarProgreso(@AuthenticationPrincipal UsuarioAutenticado u,
                                                     @Valid @RequestBody RegistrarProgresoRequest req) {
        return clienteApp.registrarProgreso(u.id(), req);
    }

    @GetMapping("/notificaciones")
    public List<NotificacionDto> notificaciones(@AuthenticationPrincipal UsuarioAutenticado u) {
        return notificaciones.deCliente(u.id());
    }

    @PatchMapping("/notificaciones/{id}/leida")
    public NotificacionDto marcarLeida(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id) {
        return notificaciones.marcarLeida(id, u.id());
    }
}
