package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.AdminDtos.*;
import pe.edu.utp.amigep.excepcion.NoEncontradoException;
import pe.edu.utp.amigep.modelo.*;
import pe.edu.utp.amigep.modelo.enums.EstadoPago;
import pe.edu.utp.amigep.modelo.enums.TipoNotificacion;
import pe.edu.utp.amigep.repositorio.*;
import pe.edu.utp.amigep.servicio.ia.HistorialCliente;
import pe.edu.utp.amigep.servicio.ia.HistorialCliente.*;
import pe.edu.utp.amigep.servicio.ia.ModeloAbandono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** RF-18 y RF-19: predicción de abandono, acciones de retención y notificaciones (BPM Fig. 12, pantalla A3). */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetencionServicio {

    private final ClienteRepositorio clienteRepo;
    private final AccesoRepositorio accesoRepo;
    private final SuscripcionRepositorio suscripcionRepo;
    private final PagoRepositorio pagoRepo;
    private final RutinaRepositorio rutinaRepo;
    private final PrediccionRepositorio prediccionRepo;
    private final AccionRetencionRepositorio accionRepo;
    private final TrabajadorRepositorio trabajadorRepo;
    private final ConsultaClienteServicio consulta;
    private final NotificacionServicio notificaciones;
    private final ModeloAbandono modelo;
    private final Mapeador mapeador;

    /** Calcula el riesgo de cada cliente activo y registra las predicciones del día (reemplaza las de hoy). */
    @Transactional
    public Map<String, Object> actualizarPredicciones() {
        LocalDate hoy = consulta.hoy();
        Map<Integer, HistorialCliente> historiales = historiales();
        Map<Integer, Prediccion> deHoy = prediccionRepo.findByFechaPrediccion(hoy).stream()
                .collect(Collectors.toMap(p -> p.getCliente().getIdCliente(), p -> p, (a, b) -> b));
        Map<String, Integer> conteo = new LinkedHashMap<>(Map.of("ALTO", 0, "MEDIO", 0, "BAJO", 0));
        for (Cliente c : clienteRepo.findByActivoTrue()) {
            HistorialCliente h = historiales.getOrDefault(c.getIdCliente(), vacio(c));
            Caracteristicas car = h.caracteristicas(hoy);
            double p = modelo.predecir(car.x());
            // Una predicción por cliente y día: si ya se calculó hoy, se actualiza.
            Prediccion pred = Optional.ofNullable(deHoy.get(c.getIdCliente())).orElseGet(Prediccion::new);
            pred.setCliente(c);
            pred.setFechaPrediccion(hoy);
            pred.setProbabilidad(BigDecimal.valueOf(p).setScale(3, RoundingMode.HALF_UP));
            pred.setFactoresRiesgo(car.factores());
            pred.setEstado(ModeloAbandono.clasificar(p));
            prediccionRepo.save(pred);
            conteo.merge(pred.getEstado().name(), 1, Integer::sum);
        }
        log.info("Predicciones de abandono actualizadas: {}", conteo);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("fecha", hoy);
        r.put("clientesEvaluados", conteo.values().stream().mapToInt(Integer::intValue).sum());
        r.put("porEstado", conteo);
        return r;
    }

    /** Entrena el modelo con ejemplos históricos: cortes quincenales de los últimos 12 meses. */
    @Transactional(readOnly = true)
    public ModeloAbandono.ResultadoEntrenamiento entrenarModelo() {
        LocalDate hoy = consulta.hoy();
        List<double[]> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        for (HistorialCliente h : historiales().values()) {
            for (LocalDate corte = hoy.minusMonths(12); !corte.isAfter(hoy.minusDays(30)); corte = corte.plusDays(14)) {
                if (!h.eraCliente(corte)) continue;
                // Solo clientes que seguían activos en el corte (con membresía vigente o reciente)
                Caracteristicas car = h.caracteristicas(corte);
                if (car.x()[2] == 1 && car.x()[0] >= 1) continue; // ya había abandonado antes del corte
                xs.add(car.x());
                ys.add(h.etiquetaAbandono(corte));
            }
        }
        return modelo.entrenar(xs, ys);
    }

    private HistorialCliente vacio(Cliente c) {
        return new HistorialCliente(c.getIdCliente(), c.getFechaRegistro(), List.of(), List.of(), List.of(), List.of());
    }

    private Map<Integer, HistorialCliente> historiales() {
        Map<Integer, List<LocalDateTime>> accesos = new HashMap<>();
        for (Object[] f : accesoRepo.resumenParaModelo()) {
            accesos.computeIfAbsent((Integer) f[0], k -> new ArrayList<>()).add(aFechaHora(f[1]));
        }
        Map<Integer, List<PeriodoSuscripcion>> suscripciones = new HashMap<>();
        for (Object[] f : suscripcionRepo.resumenParaModelo()) {
            suscripciones.computeIfAbsent((Integer) f[0], k -> new ArrayList<>())
                    .add(new PeriodoSuscripcion(aFecha(f[1]), aFecha(f[2]), (Boolean) f[3]));
        }
        Map<Integer, List<PagoHist>> pagos = new HashMap<>();
        for (Object[] f : pagoRepo.resumenParaModelo()) {
            pagos.computeIfAbsent((Integer) f[0], k -> new ArrayList<>()).add(new PagoHist(aFecha(f[1]), (EstadoPago) f[2]));
        }
        Map<Integer, List<PeriodoRutina>> rutinas = new HashMap<>();
        for (Object[] f : rutinaRepo.resumenParaModelo()) {
            rutinas.computeIfAbsent((Integer) f[0], k -> new ArrayList<>())
                    .add(new PeriodoRutina(aFecha(f[1]), f[2] == null ? null : aFecha(f[2]), (Boolean) f[3]));
        }
        Map<Integer, HistorialCliente> mapa = new HashMap<>();
        for (Cliente c : clienteRepo.findAll()) {
            Integer id = c.getIdCliente();
            mapa.put(id, new HistorialCliente(id, c.getFechaRegistro(), accesos.getOrDefault(id, List.of()),
                    suscripciones.getOrDefault(id, List.of()), pagos.getOrDefault(id, List.of()), rutinas.getOrDefault(id, List.of())));
        }
        return mapa;
    }

    private static LocalDate aFecha(Object o) {
        return o instanceof Date d ? d.toLocalDate() : (LocalDate) o;
    }

    private static LocalDateTime aFechaHora(Object o) {
        return o instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) o;
    }

    // ------------------------------------------------------------------ Consultas del panel

    @Transactional(readOnly = true)
    public ResumenRetencionDto resumen() {
        LocalDate hoy = consulta.hoy();
        List<Prediccion> ultimas = prediccionRepo.ultimasPorCliente();
        Map<String, Long> porEstado = ultimas.stream().collect(Collectors.groupingBy(p -> p.getEstado().name(), Collectors.counting()));
        List<AccionRetencion> recientes = accionRepo.desde(hoy.minusDays(90));
        Set<Integer> contactados = recientes.stream().map(a -> a.getPrediccion().getCliente().getIdCliente()).collect(Collectors.toSet());
        Set<Integer> retenidos = recientes.stream().filter(a -> AccionRetencion.RETENIDO.equals(a.getResultado()))
                .map(a -> a.getPrediccion().getCliente().getIdCliente()).collect(Collectors.toSet());
        int pct = contactados.isEmpty() ? 0 : (int) Math.round(retenidos.size() * 100.0 / contactados.size());
        LocalDate ultima = ultimas.stream().map(Prediccion::getFechaPrediccion).max(Comparator.naturalOrder()).orElse(null);
        return new ResumenRetencionDto(porEstado.getOrDefault("ALTO", 0L), porEstado.getOrDefault("MEDIO", 0L),
                porEstado.getOrDefault("BAJO", 0L), accionRepo.countByFechaAccionGreaterThanEqual(hoy.withDayOfMonth(1)),
                pct, contactados.size(), retenidos.size(), ultima, modeloInfo());
    }

    public ModeloInfoDto modeloInfo() {
        return new ModeloInfoDto("Regresión logística", modelo.isEntrenado(), modelo.getMuestras(), modelo.getExactitud(),
                modelo.getSensibilidad(), modelo.getFechaEntrenamiento(), modelo.pesosActuales());
    }

    @Transactional(readOnly = true)
    public List<PrediccionDto> predicciones(String estado) {
        LocalDate hoy = consulta.hoy();
        Map<Integer, Long> accionesPorPrediccion = accionRepo.desde(hoy.minusDays(365)).stream()
                .collect(Collectors.groupingBy(a -> a.getPrediccion().getCliente().getIdCliente(), Collectors.counting()));
        return prediccionRepo.ultimasPorCliente().stream()
                .filter(p -> estado == null || estado.isBlank() || p.getEstado().name().equals(estado))
                .map(p -> {
                    Cliente c = p.getCliente();
                    String estadoMembresia = consulta.membresiaActual(c.getIdCliente())
                            .map(s -> Mapeador.estadoMembresia(s, hoy, Mapeador.DIAS_POR_VENCER)).orElse("SIN_MEMBRESIA");
                    return new PrediccionDto(p.getIdPrediccion(), c.getIdCliente(), Textos.nombreCorto(c.getNombre(), c.getApellido()),
                            Textos.iniciales(c.getNombre(), c.getApellido()), p.getFechaPrediccion(), p.getProbabilidad(),
                            p.getFactoresRiesgo(), p.getEstado().name(), estadoMembresia,
                            accionesPorPrediccion.getOrDefault(c.getIdCliente(), 0L));
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<AccionRetencionDto> acciones(int dias) {
        return accionRepo.desde(consulta.hoy().minusDays(dias)).stream().map(this::accionDto).toList();
    }

    /** BPM Fig. 12: revisa al cliente → registra la acción → genera y envía la notificación. */
    @Transactional
    public AccionRetencionDto registrarAccion(RegistrarAccionRequest req, Integer idTrabajador) {
        Prediccion p = prediccionRepo.findById(req.idPrediccion())
                .orElseThrow(() -> new NoEncontradoException("Predicción no encontrada"));
        Trabajador t = trabajadorRepo.findById(idTrabajador).orElseThrow();
        AccionRetencion a = new AccionRetencion();
        a.setPrediccion(p);
        a.setTrabajador(t);
        a.setTipoAccion(req.tipoAccion().trim());
        a.setDescripcion(Textos.vacioANull(req.descripcion()));
        a.setFechaAccion(consulta.hoy());
        a.setResultado(req.enviarNotificacion() ? AccionRetencion.ENVIADA : AccionRetencion.PENDIENTE);
        a = accionRepo.save(a);
        if (req.enviarNotificacion()) {
            String titulo = Textos.vacioANull(req.tituloNotificacion()) != null ? req.tituloNotificacion().trim() : a.getTipoAccion();
            String mensaje = Textos.vacioANull(req.mensajeNotificacion()) != null ? req.mensajeNotificacion().trim()
                    : mensajePorDefecto(a.getTipoAccion(), p.getCliente());
            notificaciones.enviar(p.getCliente(), TipoNotificacion.RETENCION, titulo, mensaje, a);
        }
        return accionDto(a);
    }

    @Transactional
    public AccionRetencionDto actualizarResultado(Integer idAccion, String resultado) {
        AccionRetencion a = accionRepo.findById(idAccion).orElseThrow(() -> new NoEncontradoException("Acción no encontrada"));
        a.setResultado(resultado);
        return accionDto(a);
    }

    private String mensajePorDefecto(String tipo, Cliente c) {
        String nombre = c.getNombre().split(" ")[0];
        String t = tipo.toLowerCase();
        if (t.contains("renov")) return "Hola " + nombre + ", tu membresía necesita renovarse. Acércate a recepción y sigue entrenando con nosotros.";
        if (t.contains("rutina")) return "Hola " + nombre + ", preparamos una nueva rutina con IA para ti. ¡Ábrela en la pestaña Entreno!";
        if (t.contains("descuento") || t.contains("oferta")) return "Hola " + nombre + ", tenemos un beneficio especial para tu próxima renovación. Consulta en recepción.";
        return "Hola " + nombre + ", te extrañamos en Angels GYM. ¡Te esperamos para tu próxima sesión!";
    }

    private AccionRetencionDto accionDto(AccionRetencion a) {
        Cliente c = a.getPrediccion().getCliente();
        return new AccionRetencionDto(a.getIdAccion(), a.getPrediccion().getIdPrediccion(), c.getIdCliente(),
                Textos.nombreCorto(c.getNombre(), c.getApellido()), a.getTipoAccion(), a.getDescripcion(), a.getFechaAccion(),
                a.getResultado(), a.getTrabajador().getNombreCompleto());
    }
}
