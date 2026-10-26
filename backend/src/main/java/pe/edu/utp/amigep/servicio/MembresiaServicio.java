package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.AdminDtos.*;
import pe.edu.utp.amigep.dto.ComunDtos.PagoDto;
import pe.edu.utp.amigep.dto.ComunDtos.PlanDto;
import pe.edu.utp.amigep.excepcion.NoEncontradoException;
import pe.edu.utp.amigep.excepcion.ReglaNegocioException;
import pe.edu.utp.amigep.modelo.*;
import pe.edu.utp.amigep.modelo.enums.EstadoPago;
import pe.edu.utp.amigep.modelo.enums.MetodoPago;
import pe.edu.utp.amigep.modelo.enums.TipoNotificacion;
import pe.edu.utp.amigep.repositorio.PagoRepositorio;
import pe.edu.utp.amigep.repositorio.PlanRepositorio;
import pe.edu.utp.amigep.repositorio.SuscripcionRepositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** RF-04 y RF-05: planes, membresías (suscripciones) y pagos registrados en recepción (BPM Fig. 5). */
@Service
@RequiredArgsConstructor
public class MembresiaServicio {

    private final PlanRepositorio planRepo;
    private final SuscripcionRepositorio suscripcionRepo;
    private final PagoRepositorio pagoRepo;
    private final ConsultaClienteServicio consulta;
    private final NotificacionServicio notificaciones;
    private final Mapeador mapeador;

    // ------------------------------------------------------------------ Planes

    @Transactional(readOnly = true)
    public List<PlanDto> planes() {
        return planRepo.findAllByOrderByPrecioAsc().stream().map(mapeador::plan).toList();
    }

    @Transactional
    public PlanDto guardarPlan(Integer idPlan, GuardarPlanRequest req) {
        Plan p = idPlan == null ? new Plan()
                : planRepo.findById(idPlan).orElseThrow(() -> new NoEncontradoException("Plan no encontrado"));
        p.setNombre(req.nombre().trim());
        p.setDuracionDias(req.duracionDias());
        p.setPrecio(req.precio());
        p.setDescripcion(Textos.vacioANull(req.descripcion()));
        return mapeador.plan(planRepo.save(p));
    }

    // ------------------------------------------------------------------ Membresías

    @Transactional(readOnly = true)
    public List<MembresiaFilaDto> membresias(String q, String estado, Integer idPlan) {
        LocalDate hoy = consulta.hoy();
        String filtro = q == null ? "" : q.trim().toLowerCase();
        List<Suscripcion> todas = suscripcionRepo.todasConDetalle();
        Map<Integer, Boolean> pagoValidado = new HashMap<>();
        pagoRepo.entreFechas(LocalDate.of(2000, 1, 1), LocalDate.of(2999, 12, 31)).forEach(p -> {
            if (p.getEstado() == EstadoPago.VALIDADO) pagoValidado.put(p.getSuscripcion().getIdSuscripcion(), true);
        });
        // Fin de cobertura por cliente: si otra membresía activa termina después, esta ya fue renovada.
        Map<Integer, LocalDate> finCobertura = new HashMap<>();
        todas.stream().filter(Suscripcion::isActivo).forEach(s -> finCobertura.merge(s.getCliente().getIdCliente(),
                s.getFechaCaducidad(), (a, b) -> a.isAfter(b) ? a : b));
        return todas.stream()
                .filter(s -> idPlan == null || s.getPlan().getIdPlan().equals(idPlan))
                .filter(s -> filtro.isEmpty() || s.getCliente().getNombreCompleto().toLowerCase().contains(filtro)
                        || s.getCliente().getCodigo().toLowerCase().contains(filtro) || s.getCliente().getDni().contains(filtro))
                .map(s -> {
                    var m = mapeador.membresia(s, hoy);
                    return new MembresiaFilaDto(s.getIdSuscripcion(), s.getCliente().getIdCliente(), s.getCliente().getCodigo(),
                            s.getCliente().getNombreCompleto(), s.getPlan().getIdPlan(), s.getPlan().getNombre(),
                            s.getFechaInicio(), s.getFechaCaducidad(), m.estado(), m.diasRestantes(),
                            pagoValidado.getOrDefault(s.getIdSuscripcion(), false),
                            finCobertura.getOrDefault(s.getCliente().getIdCliente(), s.getFechaCaducidad()).isAfter(s.getFechaCaducidad()));
                })
                .filter(f -> estado == null || estado.isBlank() || f.estado().equals(estado))
                .toList();
    }

    @Transactional
    public MembresiaFilaDto cambiarEstadoMembresia(Integer idSuscripcion, boolean activo) {
        Suscripcion s = suscripcionRepo.findById(idSuscripcion)
                .orElseThrow(() -> new NoEncontradoException("Membresía no encontrada"));
        boolean pagada = pagoRepo.findBySuscripcionIdSuscripcion(idSuscripcion).stream()
                .anyMatch(p -> p.getEstado() == EstadoPago.VALIDADO);
        if (activo && !pagada) {
            throw new ReglaNegocioException("No se puede activar una membresía sin un pago validado");
        }
        s.setActivo(activo);
        var m = mapeador.membresia(s, consulta.hoy());
        return new MembresiaFilaDto(s.getIdSuscripcion(), s.getCliente().getIdCliente(), s.getCliente().getCodigo(),
                s.getCliente().getNombreCompleto(), s.getPlan().getIdPlan(), s.getPlan().getNombre(),
                s.getFechaInicio(), s.getFechaCaducidad(), m.estado(), m.diasRestantes(), pagada, false);
    }

    // ------------------------------------------------------------------ Pagos

    @Transactional(readOnly = true)
    public List<PagoDto> pagos(LocalDate desde, LocalDate hasta, MetodoPago metodo, String q) {
        LocalDate hoy = consulta.hoy();
        LocalDate d = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : hoy;
        String filtro = q == null ? "" : q.trim().toLowerCase();
        return pagoRepo.entreFechas(d, h).stream()
                .filter(p -> metodo == null || p.getMetodoPago() == metodo)
                .filter(p -> filtro.isEmpty() || p.getSuscripcion().getCliente().getNombreCompleto().toLowerCase().contains(filtro))
                .map(mapeador::pago).toList();
    }

    /**
     * Registra el pago hecho en recepción. Pago válido → la suscripción queda activa (renovación);
     * pago rechazado → se guarda con estado RECHAZADO y la membresía queda pendiente hasta un nuevo pago.
     */
    @Transactional
    public ResultadoPagoDto registrarPago(RegistrarPagoRequest req) {
        Cliente cliente = consulta.cliente(req.idCliente());
        if (!cliente.isActivo()) throw new ReglaNegocioException("El cliente está desactivado");
        EstadoPago estado = req.estado() != null ? req.estado() : EstadoPago.VALIDADO;
        LocalDate hoy = consulta.hoy();

        Suscripcion s;
        if (req.idSuscripcion() != null) {
            s = suscripcionRepo.findById(req.idSuscripcion())
                    .orElseThrow(() -> new NoEncontradoException("Membresía no encontrada"));
            if (!s.getCliente().getIdCliente().equals(cliente.getIdCliente())) {
                throw new ReglaNegocioException("La membresía no pertenece al cliente seleccionado");
            }
            if (pagoRepo.findBySuscripcionIdSuscripcion(s.getIdSuscripcion()).stream().anyMatch(p -> p.getEstado() == EstadoPago.VALIDADO)) {
                throw new ReglaNegocioException("Esta membresía ya tiene un pago validado");
            }
        } else {
            if (req.idPlan() == null) throw new ReglaNegocioException("Seleccione el plan", Map.of("idPlan", "Seleccione el plan"));
            Plan plan = planRepo.findById(req.idPlan()).orElseThrow(() -> new NoEncontradoException("Plan no encontrado"));
            // Renovación anticipada: la nueva membresía empieza al día siguiente de la cobertura actual.
            LocalDate inicio = suscripcionRepo.deCliente(cliente.getIdCliente()).stream()
                    .filter(x -> x.isActivo() && !x.getFechaCaducidad().isBefore(hoy))
                    .map(Suscripcion::getFechaCaducidad)
                    .max(Comparator.naturalOrder())
                    .map(f -> f.plusDays(1))
                    .orElse(hoy);
            s = new Suscripcion();
            s.setCliente(cliente);
            s.setPlan(plan);
            s.setFechaInicio(inicio);
            s.setFechaCaducidad(inicio.plusDays(plan.getDuracionDias()));
            s.setActivo(false);
            s = suscripcionRepo.save(s);
        }

        BigDecimal monto = req.monto() != null ? req.monto() : s.getPlan().getPrecio();
        if (monto.signum() <= 0) throw new ReglaNegocioException("El monto debe ser mayor a cero");
        Pago pago = new Pago();
        pago.setSuscripcion(s);
        pago.setMonto(monto);
        pago.setFechaPago(hoy);
        pago.setMetodoPago(req.metodoPago());
        pago.setEstado(estado);
        pago = pagoRepo.save(pago);

        String mensaje;
        if (estado == EstadoPago.VALIDADO) {
            s.setActivo(true);
            mensaje = "Pago registrado. Membresía " + s.getPlan().getNombre() + " vigente del "
                    + s.getFechaInicio().format(Textos.FECHA) + " al " + s.getFechaCaducidad().format(Textos.FECHA) + ".";
            notificaciones.enviar(cliente, TipoNotificacion.VENCIMIENTO, "Membresía renovada",
                    "Tu plan " + s.getPlan().getNombre() + " está vigente hasta el " + s.getFechaCaducidad().format(Textos.FECHA)
                            + ". ¡Gracias por seguir entrenando con nosotros!", null);
        } else {
            mensaje = "Pago rechazado. La membresía queda pendiente hasta registrar un pago válido.";
        }
        return new ResultadoPagoDto(mapeador.pago(pago), mapeador.membresia(s, hoy), mensaje);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> totalesPorMetodo(LocalDate desde, LocalDate hasta) {
        return pagoRepo.entreFechas(desde, hasta).stream()
                .filter(p -> p.getEstado() == EstadoPago.VALIDADO)
                .collect(Collectors.groupingBy(p -> p.getMetodoPago().name(), TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Pago::getMonto, BigDecimal::add)));
    }
}
