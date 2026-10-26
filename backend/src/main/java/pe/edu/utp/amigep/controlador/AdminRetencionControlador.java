package pe.edu.utp.amigep.controlador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.amigep.dto.AdminDtos.*;
import pe.edu.utp.amigep.dto.ComunDtos.NotificacionDto;
import pe.edu.utp.amigep.seguridad.UsuarioAutenticado;
import pe.edu.utp.amigep.servicio.ConsultaClienteServicio;
import pe.edu.utp.amigep.servicio.Mapeador;
import pe.edu.utp.amigep.servicio.NotificacionServicio;
import pe.edu.utp.amigep.servicio.RetencionServicio;
import pe.edu.utp.amigep.servicio.ia.ModeloAbandono;

import java.util.List;
import java.util.Map;

/** Panel web · A3 Retención IA (RF-18) y notificaciones (RF-19). */
@Tag(name = "Panel web · Retención IA")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRetencionControlador {

    private final RetencionServicio retencion;
    private final NotificacionServicio notificaciones;
    private final ConsultaClienteServicio consulta;
    private final Mapeador mapeador;

    @GetMapping("/retencion/resumen")
    public ResumenRetencionDto resumen() {
        return retencion.resumen();
    }

    @GetMapping("/retencion/predicciones")
    public List<PrediccionDto> predicciones(@RequestParam(required = false) String estado) {
        return retencion.predicciones(estado);
    }

    /** "Actualizar predicciones": ejecuta el módulo inteligente sobre todos los clientes activos. */
    @PostMapping("/retencion/predicciones/actualizar")
    public Map<String, Object> actualizar() {
        return retencion.actualizarPredicciones();
    }

    /** Reentrena el modelo de clasificación con el historial de asistencias, pagos y suscripciones. */
    @PostMapping("/retencion/modelo/entrenar")
    public ModeloAbandono.ResultadoEntrenamiento entrenar() {
        return retencion.entrenarModelo();
    }

    @GetMapping("/retencion/modelo")
    public ModeloInfoDto modelo() {
        return retencion.modeloInfo();
    }

    @GetMapping("/retencion/acciones")
    public List<AccionRetencionDto> acciones(@RequestParam(defaultValue = "60") int dias) {
        return retencion.acciones(dias);
    }

    @PostMapping("/retencion/acciones")
    @ResponseStatus(HttpStatus.CREATED)
    public AccionRetencionDto registrarAccion(@AuthenticationPrincipal UsuarioAutenticado u,
                                              @Valid @RequestBody RegistrarAccionRequest req) {
        return retencion.registrarAccion(req, u.id());
    }

    @PatchMapping("/retencion/acciones/{id}")
    public AccionRetencionDto actualizarResultado(@PathVariable Integer id, @Valid @RequestBody ActualizarResultadoRequest req) {
        return retencion.actualizarResultado(id, req.resultado());
    }

    @GetMapping("/notificaciones")
    public List<NotificacionDto> notificaciones() {
        return notificaciones.recientes();
    }

    @PostMapping("/notificaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificacionDto enviar(@Valid @RequestBody EnviarNotificacionRequest req) {
        return mapeador.notificacion(notificaciones.enviar(consulta.cliente(req.idCliente()), req.tipo(), req.titulo(),
                req.mensaje(), null));
    }
}
