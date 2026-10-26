package pe.edu.utp.amigep.controlador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.amigep.dto.AdminDtos.*;
import pe.edu.utp.amigep.dto.ComunDtos.*;
import pe.edu.utp.amigep.modelo.enums.MetodoPago;
import pe.edu.utp.amigep.modelo.enums.Rol;
import pe.edu.utp.amigep.seguridad.UsuarioAutenticado;
import pe.edu.utp.amigep.servicio.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Panel web · rol ADMINISTRADOR: panel general, clientes, membresías, pagos, personal y acceso QR. */
@Tag(name = "Panel web · Administrador")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminControlador {

    private final DashboardServicio dashboard;
    private final AdminClienteServicio clientes;
    private final MembresiaServicio membresias;
    private final TrabajadorServicio trabajadores;
    private final AccesoServicio accesos;

    /** A1 - Panel general. */
    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return dashboard.dashboard();
    }

    // ------------------------------------------------------------------ A2 Clientes

    @GetMapping("/clientes")
    public List<ClienteFilaDto> clientes(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) Integer idPlan,
                                         @RequestParam(required = false) String estado) {
        return clientes.listar(q, idPlan, estado);
    }

    @GetMapping("/clientes/{id}")
    public ClienteDetalleDto cliente(@PathVariable Integer id) {
        return clientes.detalle(id);
    }

    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteDetalleDto crearCliente(@Valid @RequestBody GuardarClienteRequest req) {
        return clientes.crear(req);
    }

    @PutMapping("/clientes/{id}")
    public ClienteDetalleDto actualizarCliente(@PathVariable Integer id, @Valid @RequestBody GuardarClienteRequest req) {
        return clientes.actualizar(id, req);
    }

    /** Baja lógica / reactivación. */
    @PatchMapping("/clientes/{id}/estado")
    public ClienteDetalleDto estadoCliente(@PathVariable Integer id, @Valid @RequestBody CambiarEstadoRequest req) {
        return clientes.cambiarEstado(id, req.activo());
    }

    @PostMapping("/clientes/{id}/regenerar-qr")
    public ClienteDetalleDto regenerarQr(@PathVariable Integer id) {
        return clientes.regenerarQr(id);
    }

    // ------------------------------------------------------------------ Membresías y pagos

    @GetMapping("/planes")
    public List<PlanDto> planes() {
        return membresias.planes();
    }

    @PostMapping("/planes")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanDto crearPlan(@Valid @RequestBody GuardarPlanRequest req) {
        return membresias.guardarPlan(null, req);
    }

    @PutMapping("/planes/{id}")
    public PlanDto actualizarPlan(@PathVariable Integer id, @Valid @RequestBody GuardarPlanRequest req) {
        return membresias.guardarPlan(id, req);
    }

    @GetMapping("/membresias")
    public List<MembresiaFilaDto> membresias(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String estado,
                                             @RequestParam(required = false) Integer idPlan) {
        return membresias.membresias(q, estado, idPlan);
    }

    /** Suspender / reactivar una membresía. */
    @PatchMapping("/membresias/{id}/estado")
    public MembresiaFilaDto estadoMembresia(@PathVariable Integer id, @Valid @RequestBody CambiarEstadoRequest req) {
        return membresias.cambiarEstadoMembresia(id, req.activo());
    }

    @GetMapping("/pagos")
    public List<PagoDto> pagos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                               @RequestParam(required = false) MetodoPago metodo,
                               @RequestParam(required = false) String q) {
        return membresias.pagos(desde, hasta, metodo, q);
    }

    /** Registrar pago en recepción / renovar membresía (BPM Fig. 5). */
    @PostMapping("/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoPagoDto registrarPago(@Valid @RequestBody RegistrarPagoRequest req) {
        return membresias.registrarPago(req);
    }

    // ------------------------------------------------------------------ Entrenadores y usuarios (Configuración)

    @GetMapping("/trabajadores")
    public List<TrabajadorDto> trabajadores(@RequestParam(required = false) Rol rol) {
        return trabajadores.listar(rol);
    }

    @GetMapping("/trabajadores/{id}")
    public TrabajadorDto trabajador(@PathVariable Integer id) {
        return trabajadores.obtener(id);
    }

    @PostMapping("/trabajadores")
    @ResponseStatus(HttpStatus.CREATED)
    public TrabajadorDto crearTrabajador(@Valid @RequestBody GuardarTrabajadorRequest req) {
        return trabajadores.crear(req);
    }

    @PutMapping("/trabajadores/{id}")
    public TrabajadorDto actualizarTrabajador(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id,
                                              @Valid @RequestBody GuardarTrabajadorRequest req) {
        return trabajadores.actualizar(id, req, u.id());
    }

    @PatchMapping("/trabajadores/{id}/estado")
    public TrabajadorDto estadoTrabajador(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Integer id,
                                          @Valid @RequestBody CambiarEstadoRequest req) {
        return trabajadores.cambiarEstado(id, req.activo(), u.id());
    }

    @PostMapping("/trabajadores/{id}/restablecer-contrasena")
    public Map<String, String> restablecerContrasena(@PathVariable Integer id) {
        return trabajadores.restablecerContrasena(id);
    }

    // ------------------------------------------------------------------ Control de acceso (RF-17)

    @PostMapping("/accesos/qr")
    public ResultadoAccesoDto accesoQr(@Valid @RequestBody AccesoQrRequest req) {
        return accesos.registrarPorQr(req.codigoQR());
    }

    @PostMapping("/accesos/manual")
    public ResultadoAccesoDto accesoManual(@Valid @RequestBody AccesoManualRequest req) {
        return accesos.registrarManual(req.dni());
    }

    @GetMapping("/accesos")
    public List<AccesoDto> accesosDelDia(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return accesos.delDia(fecha);
    }
}
