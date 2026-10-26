package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.AdminDtos.*;
import pe.edu.utp.amigep.excepcion.ReglaNegocioException;
import pe.edu.utp.amigep.modelo.Cliente;
import pe.edu.utp.amigep.modelo.Suscripcion;
import pe.edu.utp.amigep.modelo.Trabajador;
import pe.edu.utp.amigep.modelo.enums.MetodoPago;
import pe.edu.utp.amigep.modelo.enums.Rol;
import pe.edu.utp.amigep.repositorio.*;

import java.time.LocalDate;
import java.util.*;

/** RF-03: registro, consulta y actualización de clientes desde el panel web (BPM Fig. 4, pantalla A2). */
@Service
@RequiredArgsConstructor
public class AdminClienteServicio {

    private final ClienteRepositorio clienteRepo;
    private final TrabajadorRepositorio trabajadorRepo;
    private final SuscripcionRepositorio suscripcionRepo;
    private final PagoRepositorio pagoRepo;
    private final AccesoRepositorio accesoRepo;
    private final PrediccionRepositorio prediccionRepo;
    private final ConsultaClienteServicio consulta;
    private final MembresiaServicio membresiaServicio;
    private final PasswordEncoder encoder;
    private final Mapeador mapeador;

    @Transactional(readOnly = true)
    public List<ClienteFilaDto> listar(String q, Integer idPlan, String estado) {
        String texto = q == null ? "" : q.trim();
        String idTexto = texto.toUpperCase().replace("CL-", "").replaceFirst("^0+(?=\\d)", "");
        LocalDate hoy = consulta.hoy();
        Map<Integer, Suscripcion> actuales = membresiasActuales(hoy);
        return clienteRepo.buscar(texto, idTexto).stream()
                .map(c -> fila(c, actuales.get(c.getIdCliente()), hoy))
                .filter(f -> idPlan == null || idPlan.equals(f.idPlan()))
                .filter(f -> estado == null || estado.isBlank() || estado.equals(f.estadoMembresia())
                        || ("INACTIVO".equals(estado) && !f.activo()))
                // Cuentas activas primero; dentro de cada grupo, las más recientes arriba
                .sorted(Comparator.comparing((ClienteFilaDto f) -> !f.activo()))
                .toList();
    }

    /** Membresía que se muestra por cliente: la vigente de mayor caducidad o, si no hay, la última iniciada. */
    private Map<Integer, Suscripcion> membresiasActuales(LocalDate hoy) {
        Map<Integer, Suscripcion> mapa = new HashMap<>();
        for (Suscripcion s : suscripcionRepo.todasConDetalle()) {
            if (s.getFechaInicio().isAfter(hoy)) continue;
            Integer id = s.getCliente().getIdCliente();
            Suscripcion actual = mapa.get(id);
            if (actual == null || prioridad(s, hoy) > prioridad(actual, hoy)
                    || (prioridad(s, hoy) == prioridad(actual, hoy) && s.getFechaCaducidad().isAfter(actual.getFechaCaducidad()))) {
                mapa.put(id, s);
            }
        }
        return mapa;
    }

    private static int prioridad(Suscripcion s, LocalDate hoy) {
        return s.esVigente(hoy) ? 2 : s.isActivo() ? 1 : 0;
    }

    static String estadoCliente(Suscripcion s, LocalDate hoy) {
        if (s == null) return "SIN_MEMBRESIA";
        return switch (Mapeador.estadoMembresia(s, hoy, Mapeador.DIAS_POR_VENCER)) {
            case "VIGENTE" -> "ACTIVO";
            case "POR_VENCER" -> "POR_VENCER";
            case "SUSPENDIDA" -> "SUSPENDIDO";
            default -> "VENCIDO";
        };
    }

    private ClienteFilaDto fila(Cliente c, Suscripcion s, LocalDate hoy) {
        Trabajador e = c.getEntrenador();
        return new ClienteFilaDto(c.getIdCliente(), c.getCodigo(), c.getNombre(), c.getApellido(), c.getEmail(),
                c.getDni(), c.getTelefono(), s == null ? null : s.getPlan().getIdPlan(), s == null ? null : s.getPlan().getNombre(),
                s == null ? null : s.getFechaInicio(), s == null ? null : s.getFechaCaducidad(), estadoCliente(s, hoy),
                c.isActivo(), e == null ? null : e.getIdTrabajador(), e == null ? null : e.getNombreCompleto());
    }

    @Transactional(readOnly = true)
    public ClienteDetalleDto detalle(Integer idCliente) {
        Cliente c = consulta.cliente(idCliente);
        LocalDate hoy = consulta.hoy();
        Suscripcion actual = consulta.membresiaActual(idCliente).orElse(null);
        var prediccion = prediccionRepo.findFirstByClienteIdClienteOrderByIdPrediccionDesc(idCliente);
        return new ClienteDetalleDto(fila(c, actual, hoy), c.getObjetivo(), c.getNivel() == null ? null : c.getNivel().name(),
                c.getAltura(), c.getFechaNacimiento(), c.getFechaRegistro(), c.getCodigoQR(),
                suscripcionRepo.deCliente(idCliente).stream().map(s -> mapeador.membresia(s, hoy)).toList(),
                pagoRepo.deCliente(idCliente).stream().map(mapeador::pago).toList(),
                accesoRepo.findTop10ByClienteIdClienteOrderByFechaHoraDesc(idCliente).stream().map(mapeador::acceso).toList(),
                prediccion.map(p -> p.getEstado().name()).orElse(null),
                prediccion.map(p -> p.getProbabilidad().doubleValue()).orElse(null));
    }

    @Transactional
    public ClienteDetalleDto crear(GuardarClienteRequest req) {
        if (req.contrasena() == null || req.contrasena().isBlank()) {
            throw new ReglaNegocioException("Revise los datos ingresados", Map.of("contrasena", "Ingrese una contraseña inicial"));
        }
        validarUnicos(req, null);
        Cliente c = new Cliente();
        aplicar(c, req);
        c.setContrasenaHash(encoder.encode(req.contrasena()));
        c.setCodigoQR(nuevoCodigoQR());
        c.setFechaRegistro(consulta.hoy());
        c.setActivo(true);
        c = clienteRepo.save(c);
        if (req.idPlan() != null) {
            MetodoPago metodo = req.metodoPago() != null ? req.metodoPago() : MetodoPago.EFECTIVO;
            membresiaServicio.registrarPago(new RegistrarPagoRequest(c.getIdCliente(), req.idPlan(), null, metodo, null, null));
        }
        return detalle(c.getIdCliente());
    }

    @Transactional
    public ClienteDetalleDto actualizar(Integer idCliente, GuardarClienteRequest req) {
        Cliente c = consulta.cliente(idCliente);
        validarUnicos(req, c);
        aplicar(c, req);
        if (req.contrasena() != null && !req.contrasena().isBlank()) c.setContrasenaHash(encoder.encode(req.contrasena()));
        return detalle(idCliente);
    }

    @Transactional
    public ClienteDetalleDto cambiarEstado(Integer idCliente, boolean activo) {
        consulta.cliente(idCliente).setActivo(activo);
        return detalle(idCliente);
    }

    /** Por ejemplo, si el cliente cambió de celular: el QR anterior deja de funcionar. */
    @Transactional
    public ClienteDetalleDto regenerarQr(Integer idCliente) {
        consulta.cliente(idCliente).setCodigoQR(nuevoCodigoQR());
        return detalle(idCliente);
    }

    private String nuevoCodigoQR() {
        String codigo;
        do {
            codigo = "AMIGEP-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        } while (clienteRepo.findByCodigoQR(codigo).isPresent());
        return codigo;
    }

    private void aplicar(Cliente c, GuardarClienteRequest req) {
        c.setNombre(req.nombre().trim());
        c.setApellido(req.apellido().trim());
        c.setDni(req.dni().trim());
        c.setEmail(req.email().trim().toLowerCase());
        c.setTelefono(Textos.vacioANull(req.telefono()));
        c.setObjetivo(Textos.vacioANull(req.objetivo()));
        c.setNivel(req.nivel());
        c.setAltura(req.altura());
        c.setFechaNacimiento(req.fechaNacimiento());
        if (req.idEntrenador() == null) {
            c.setEntrenador(null);
        } else {
            Trabajador e = trabajadorRepo.findById(req.idEntrenador())
                    .filter(t -> t.getRol() == Rol.ENTRENADOR && t.isActivo())
                    .orElseThrow(() -> new ReglaNegocioException("Revise los datos ingresados",
                            Map.of("idEntrenador", "Seleccione un entrenador activo")));
            c.setEntrenador(e);
        }
    }

    private void validarUnicos(GuardarClienteRequest req, Cliente actual) {
        Map<String, String> errores = new LinkedHashMap<>();
        clienteRepo.findByEmailIgnoreCase(req.email().trim())
                .filter(o -> actual == null || !o.getIdCliente().equals(actual.getIdCliente()))
                .ifPresent(o -> errores.put("email", "El correo ya está registrado"));
        clienteRepo.findByDni(req.dni().trim())
                .filter(o -> actual == null || !o.getIdCliente().equals(actual.getIdCliente()))
                .ifPresent(o -> errores.put("dni", "El DNI ya está registrado"));
        if (!errores.isEmpty()) throw new ReglaNegocioException("Revise los datos ingresados", errores);
    }
}
