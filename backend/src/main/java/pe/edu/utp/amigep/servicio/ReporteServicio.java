package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.ComunDtos.*;
import pe.edu.utp.amigep.excepcion.AccesoDenegadoException;
import pe.edu.utp.amigep.excepcion.NoEncontradoException;
import pe.edu.utp.amigep.excepcion.ReglaNegocioException;
import pe.edu.utp.amigep.modelo.*;
import pe.edu.utp.amigep.modelo.enums.EstadoPago;
import pe.edu.utp.amigep.repositorio.*;
import pe.edu.utp.amigep.seguridad.UsuarioAutenticado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RF-20: reportes RP-01 a RP-08 (sección 13). Se generan bajo demanda, con filtros, según el rol
 * y sin modificar los registros (transacciones de solo lectura).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServicio {

    private static final String ADMIN = UsuarioAutenticado.ADMINISTRADOR;
    private static final String ENTRENADOR = UsuarioAutenticado.ENTRENADOR;
    private static final String CLIENTE = UsuarioAutenticado.CLIENTE;

    /** Usuarios autorizados por reporte (tabla 13.1). */
    private static final Map<String, Set<String>> AUTORIZADOS = Map.of(
            "RP-01", Set.of(ADMIN),
            "RP-02", Set.of(ADMIN),
            "RP-03", Set.of(ADMIN),
            "RP-04", Set.of(ADMIN, ENTRENADOR),
            "RP-05", Set.of(ENTRENADOR, ADMIN),
            "RP-06", Set.of(CLIENTE, ENTRENADOR),
            "RP-07", Set.of(CLIENTE, ENTRENADOR),
            "RP-08", Set.of(CLIENTE, ADMIN));

    private final ClienteRepositorio clienteRepo;
    private final SuscripcionRepositorio suscripcionRepo;
    private final PagoRepositorio pagoRepo;
    private final AccesoRepositorio accesoRepo;
    private final RutinaRepositorio rutinaRepo;
    private final ProgresoRepositorio progresoRepo;
    private final RegistroComidaRepositorio comidaRepo;
    private final SesionEntrenamientoRepositorio sesionRepo;
    private final ConsultaClienteServicio consulta;
    private final DashboardServicio dashboardServicio;
    private final AlimentacionServicio alimentacionServicio;

    public List<Map<String, Object>> catalogo(UsuarioAutenticado u) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String[][] datos = {
                {"RP-01", "Clientes activos", "Membresía vigente y datos de contacto."},
                {"RP-02", "Membresías y vencimientos", "Vigencia y renovaciones próximas."},
                {"RP-03", "Pagos por periodo", "Totales y subtotales por método."},
                {"RP-04", "Asistencia por cliente", "Frecuencia de visitas al gimnasio."},
                {"RP-05", "Rutinas asignadas", "Distribución por entrenador y nivel."},
                {"RP-06", "Progreso del cliente", "Mediciones, asistencias y cumplimiento."},
                {"RP-07", "Seguimiento alimenticio", "Consumo frente a la meta calórica."},
                {"RP-08", "Resumen del dashboard", "Vista consolidada por rol."}};
        for (String[] d : datos) {
            if (AUTORIZADOS.get(d[0]).contains(u.rol())) {
                lista.add(Map.of("codigo", d[0], "titulo", d[1], "descripcion", d[2]));
            }
        }
        return lista;
    }

    public ReporteDto generar(String codigo, Map<String, String> filtros, UsuarioAutenticado u) {
        String rp = codigo.toUpperCase();
        Set<String> roles = AUTORIZADOS.get(rp);
        if (roles == null) throw new NoEncontradoException("Reporte no encontrado: " + codigo);
        if (!roles.contains(u.rol())) throw new AccesoDenegadoException("Su rol no está autorizado para el reporte " + rp);
        Filtros f = new Filtros(filtros);
        return switch (rp) {
            case "RP-01" -> rp01(f);
            case "RP-02" -> rp02(f);
            case "RP-03" -> rp03(f);
            case "RP-04" -> rp04(f, u);
            case "RP-05" -> rp05(f, u);
            case "RP-06" -> rp06(f, u);
            case "RP-07" -> rp07(f, u);
            default -> rp08(u);
        };
    }

    // ------------------------------------------------------------------ RP-01

    private ReporteDto rp01(Filtros f) {
        LocalDate hoy = consulta.hoy();
        String q = f.texto("q");
        Integer idPlan = f.entero("idPlan");
        Map<Integer, Suscripcion> vigente = new LinkedHashMap<>();
        for (Suscripcion s : suscripcionRepo.todasConDetalle()) {
            Cliente c = s.getCliente();
            if (!c.isActivo() || !s.esVigente(hoy)) continue;
            Suscripcion previa = vigente.get(c.getIdCliente());
            if (previa == null || s.getFechaCaducidad().isAfter(previa.getFechaCaducidad())) vigente.put(c.getIdCliente(), s);
        }
        List<Map<String, Object>> filas = vigente.values().stream()
                .filter(s -> idPlan == null || s.getPlan().getIdPlan().equals(idPlan))
                .filter(s -> coincide(s.getCliente(), q))
                .sorted(Comparator.comparing(s -> s.getCliente().getApellido()))
                .map(s -> fila("codigo", s.getCliente().getCodigo(), "cliente", s.getCliente().getNombreCompleto(),
                        "email", s.getCliente().getEmail(), "telefono", s.getCliente().getTelefono(), "estadoCuenta", "HABILITADO",
                        "plan", s.getPlan().getNombre(), "fechaInicio", s.getFechaInicio(), "fechaCaducidad", s.getFechaCaducidad()))
                .toList();
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalClientesActivos", filas.size());
        resumen.put("porPlan", contar(filas, "plan"));
        return reporte("RP-01", "Reporte de clientes activos",
                "Clientes habilitados con una membresía vigente. Cada cliente se cuenta una sola vez.",
                f.usados("q", "idPlan"),
                List.of(col("codigo", "Código"), col("cliente", "Cliente"), col("email", "Correo"), col("telefono", "Teléfono"),
                        col("estadoCuenta", "Cuenta", "estado"), col("plan", "Plan"), col("fechaInicio", "Inicio", "fecha"),
                        col("fechaCaducidad", "Vencimiento", "fecha")),
                filas, resumen);
    }

    // ------------------------------------------------------------------ RP-02

    private ReporteDto rp02(Filtros f) {
        LocalDate hoy = consulta.hoy();
        String q = f.texto("q");
        Integer idPlan = f.entero("idPlan");
        String estado = f.texto("estado");
        int anticipacion = Optional.ofNullable(f.entero("diasAnticipacion")).orElse(Mapeador.DIAS_POR_VENCER);
        LocalDate desde = f.fecha("desde", null);
        LocalDate hasta = f.fecha("hasta", null);
        List<Map<String, Object>> filas = suscripcionRepo.todasConDetalle().stream()
                .filter(s -> idPlan == null || s.getPlan().getIdPlan().equals(idPlan))
                .filter(s -> coincide(s.getCliente(), q))
                .filter(s -> desde == null || !s.getFechaCaducidad().isBefore(desde))
                .filter(s -> hasta == null || !s.getFechaCaducidad().isAfter(hasta))
                .map(s -> {
                    String e = Mapeador.estadoMembresia(s, hoy, anticipacion);
                    long dias = ChronoUnit.DAYS.between(hoy, s.getFechaCaducidad());
                    return fila("codigo", String.format("SU-%04d", s.getIdSuscripcion()), "cliente", s.getCliente().getNombreCompleto(),
                            "plan", s.getPlan().getNombre(), "fechaInicio", s.getFechaInicio(), "fechaCaducidad", s.getFechaCaducidad(),
                            "estado", e, "diasRestantes", Math.max(0, dias));
                })
                .filter(r -> estado == null || estado.equals(r.get("estado")))
                .sorted(Comparator.comparing(r -> (LocalDate) r.get("fechaCaducidad")))
                .toList();
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("total", filas.size());
        resumen.put("porEstado", contar(filas, "estado"));
        resumen.put("diasAnticipacion", anticipacion);
        return reporte("RP-02", "Reporte de membresías y vencimientos",
                "Clasificación: suspendida, vencida, programada, por vencer o vigente.",
                f.usados("q", "idPlan", "estado", "desde", "hasta", "diasAnticipacion"),
                List.of(col("codigo", "Código"), col("cliente", "Cliente"), col("plan", "Plan"),
                        col("fechaInicio", "Inicio", "fecha"), col("fechaCaducidad", "Vencimiento", "fecha"),
                        col("estado", "Estado", "estado"), col("diasRestantes", "Días restantes", "numero")),
                filas, resumen);
    }

    // ------------------------------------------------------------------ RP-03

    private ReporteDto rp03(Filtros f) {
        LocalDate hoy = consulta.hoy();
        LocalDate desde = f.fecha("desde", hoy.withDayOfMonth(1));
        LocalDate hasta = f.fecha("hasta", hoy);
        Integer idCliente = f.entero("idCliente");
        Integer idPlan = f.entero("idPlan");
        String metodo = f.texto("metodo");
        List<Pago> pagos = pagoRepo.entreFechas(desde, hasta).stream()
                .filter(p -> idCliente == null || p.getSuscripcion().getCliente().getIdCliente().equals(idCliente))
                .filter(p -> idPlan == null || p.getSuscripcion().getPlan().getIdPlan().equals(idPlan))
                .filter(p -> metodo == null || p.getMetodoPago().name().equals(metodo))
                .toList();
        List<Map<String, Object>> filas = pagos.stream().map(p -> fila(
                "codigo", String.format("PG-%04d", p.getIdPago()), "fechaPago", p.getFechaPago(),
                "cliente", p.getSuscripcion().getCliente().getNombreCompleto(),
                "membresia", String.format("SU-%04d", p.getSuscripcion().getIdSuscripcion()),
                "plan", p.getSuscripcion().getPlan().getNombre(), "metodoPago", Textos.etiquetaMetodo(p.getMetodoPago().name()),
                "monto", p.getMonto(), "estado", p.getEstado().name())).toList();
        List<Pago> validados = pagos.stream().filter(p -> p.getEstado() == EstadoPago.VALIDADO).toList();
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        for (String m : List.of("YAPE_PLIN", "TARJETA", "EFECTIVO")) {
            porMetodo.put(Textos.etiquetaMetodo(m), validados.stream().filter(p -> p.getMetodoPago().name().equals(m))
                    .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("cantidadPagos", validados.size());
        resumen.put("pagosRechazados", pagos.size() - validados.size());
        resumen.put("subtotalesPorMetodo", porMetodo);
        resumen.put("totalPeriodo", validados.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add));
        return reporte("RP-03", "Reporte de pagos por periodo",
                "Pagos registrados en recepción del " + desde.format(Textos.FECHA) + " al " + hasta.format(Textos.FECHA)
                        + ". El total considera solo pagos validados (en soles).",
                f.usadosCon(Map.of("desde", desde, "hasta", hasta), "idCliente", "idPlan", "metodo"),
                List.of(col("codigo", "Código"), col("fechaPago", "Fecha", "fecha"), col("cliente", "Cliente"),
                        col("membresia", "Membresía"), col("plan", "Plan"), col("metodoPago", "Método"),
                        col("monto", "Monto", "moneda"), col("estado", "Estado", "estado")),
                filas, resumen);
    }

    // ------------------------------------------------------------------ RP-04

    private ReporteDto rp04(Filtros f, UsuarioAutenticado u) {
        LocalDate hoy = consulta.hoy();
        LocalDate desde = f.fecha("desde", hoy.minusDays(29));
        LocalDate hasta = f.fecha("hasta", hoy);
        Integer idCliente = f.entero("idCliente");
        Set<Integer> permitidos = u.esEntrenador() ? asignados(u.id()) : null;
        if (idCliente != null && permitidos != null && !permitidos.contains(idCliente)) {
            throw new AccesoDenegadoException("Este cliente no está asignado a usted");
        }
        List<Acceso> accesos = accesoRepo.entre(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()).stream()
                .filter(a -> permitidos == null || permitidos.contains(a.getCliente().getIdCliente()))
                .filter(a -> idCliente == null || a.getCliente().getIdCliente().equals(idCliente))
                .toList();
        List<Map<String, Object>> filas = accesos.stream().map(a -> fila(
                "codigo", a.getCliente().getCodigo(), "cliente", a.getCliente().getNombreCompleto(),
                "fecha", a.getFechaHora().toLocalDate(), "hora", a.getFechaHora().toLocalTime().withNano(0).toString(),
                "tipo", a.getTipo().name())).toList();
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalIngresos", accesos.size());
        resumen.put("diasConAsistencia", accesos.stream().map(a -> a.getFechaHora().toLocalDate()).distinct().count());
        resumen.put("clientesDistintos", accesos.stream().map(a -> a.getCliente().getIdCliente()).distinct().count());
        return reporte("RP-04", "Reporte de asistencia por cliente y periodo",
                "Cada asistencia se cuenta una vez. No se calcula permanencia porque no se registra la hora de salida.",
                f.usadosCon(Map.of("desde", desde, "hasta", hasta), "idCliente"),
                List.of(col("codigo", "Código"), col("cliente", "Cliente"), col("fecha", "Fecha", "fecha"),
                        col("hora", "Hora de ingreso"), col("tipo", "Registro", "estado")),
                filas, resumen);
    }

    // ------------------------------------------------------------------ RP-05

    private ReporteDto rp05(Filtros f, UsuarioAutenticado u) {
        LocalDate hoy = consulta.hoy();
        Integer idCliente = f.entero("idCliente");
        Integer idEntrenador = u.esEntrenador() ? null : f.entero("idEntrenador");
        String objetivo = f.texto("objetivo");
        String nivel = f.texto("nivel");
        String origen = f.texto("origen");
        String estado = f.texto("estado");
        LocalDate desde = f.fecha("desde", null);
        LocalDate hasta = f.fecha("hasta", null);
        List<Rutina> rutinas = u.esEntrenador() ? rutinaRepo.deClientesDeEntrenador(u.id()) : rutinaRepo.todasConDetalle();
        List<Map<String, Object>> filas = rutinas.stream()
                .filter(r -> idCliente == null || r.getCliente().getIdCliente().equals(idCliente))
                .filter(r -> idEntrenador == null || (r.getTrabajador() != null && r.getTrabajador().getIdTrabajador().equals(idEntrenador)))
                .filter(r -> objetivo == null || r.getObjetivo().equalsIgnoreCase(objetivo))
                .filter(r -> nivel == null || r.getNivel().name().equals(nivel))
                .filter(r -> origen == null || r.getOrigen().name().equals(origen))
                .filter(r -> estado == null || (estado.equals("ACTIVA") == r.isActivo()))
                .filter(r -> desde == null || !r.getFechaCreacion().isBefore(desde))
                .filter(r -> hasta == null || !r.getFechaCreacion().isAfter(hasta))
                .map(r -> {
                    long completadas = sesionRepo.countByRutinaIdRutinaAndCompletadaTrue(r.getIdRutina());
                    return fila("codigo", String.format("RT-%04d", r.getIdRutina()), "cliente", r.getCliente().getNombreCompleto(),
                            "rutina", Mapeador.nombreRutina(r), "objetivo", r.getObjetivo(), "nivel", r.getNivel().name(),
                            "origen", r.getOrigen().name(),
                            "entrenador", r.getTrabajador() == null ? "Módulo IA" : r.getTrabajador().getNombreCompleto(),
                            "fechaCreacion", r.getFechaCreacion(), "estado", r.isActivo() ? "ACTIVA" : "INACTIVA",
                            "sesionesCompletadas", completadas, "cumplimiento", Mapeador.cumplimiento(r, completadas, hoy),
                            "detalle", detalleRutina(r));
                }).toList();
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalRutinas", filas.size());
        resumen.put("porOrigen", contar(filas, "origen"));
        resumen.put("porNivel", contar(filas, "nivel"));
        resumen.put("porEstado", contar(filas, "estado"));
        return reporte("RP-05", "Reporte de rutinas asignadas",
                "El cumplimiento se calcula con las sesiones registradas. Cada rutina se cuenta una sola vez.",
                f.usados("idCliente", "idEntrenador", "objetivo", "nivel", "origen", "estado", "desde", "hasta"),
                List.of(col("codigo", "Código"), col("cliente", "Cliente"), col("rutina", "Rutina"), col("objetivo", "Objetivo"),
                        col("nivel", "Nivel", "estado"), col("origen", "Origen", "estado"), col("entrenador", "Entrenador"),
                        col("fechaCreacion", "Fecha", "fecha"), col("estado", "Estado", "estado"),
                        col("cumplimiento", "Cumplimiento", "porcentaje"), col("detalle", "Ejercicios")),
                filas, resumen);
    }

    @SuppressWarnings("unchecked")
    private static String detalleRutina(Rutina r) {
        if (r.getContenido() == null || !(r.getContenido().get("dias") instanceof List<?> dias)) return "";
        StringBuilder sb = new StringBuilder();
        for (Object o : dias) {
            Map<String, Object> d = (Map<String, Object>) o;
            sb.append(d.get("nombre")).append(": ");
            List<Map<String, Object>> ejercicios = (List<Map<String, Object>>) d.getOrDefault("ejercicios", List.of());
            sb.append(ejercicios.stream().map(e -> e.get("nombre") + " " + e.get("series") + "×" + e.get("repeticiones"))
                    .collect(Collectors.joining(", "))).append(" | ");
        }
        return sb.length() > 3 ? sb.substring(0, sb.length() - 3) : sb.toString();
    }

    // ------------------------------------------------------------------ RP-06

    private ReporteDto rp06(Filtros f, UsuarioAutenticado u) {
        Cliente c = clienteDelReporte(f, u);
        LocalDate hoy = consulta.hoy();
        LocalDate desde = f.fecha("desde", hoy.minusMonths(3));
        LocalDate hasta = f.fecha("hasta", hoy);
        List<Progreso> mediciones = progresoRepo.findByClienteIdClienteOrderByFechaAscIdProgresoAsc(c.getIdCliente()).stream()
                .filter(p -> !p.getFecha().isBefore(desde) && !p.getFecha().isAfter(hasta)).toList();
        List<Map<String, Object>> filas = mediciones.stream().map(p -> fila("fecha", p.getFecha(), "peso", p.getPeso(),
                "grasaCorporal", p.getGrasaCorporal(), "medidas", medidasTexto(p.getMedidas()),
                "observaciones", p.getObservaciones())).toList();
        List<Progreso> conPeso = mediciones.stream().filter(p -> p.getPeso() != null).toList();
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("cliente", c.getNombreCompleto());
        resumen.put("objetivo", c.getObjetivo());
        if (conPeso.size() >= 2 && !conPeso.get(0).getFecha().equals(conPeso.get(conPeso.size() - 1).getFecha())) {
            resumen.put("variacionPesoKg", conPeso.get(conPeso.size() - 1).getPeso().subtract(conPeso.get(0).getPeso()));
        } else {
            resumen.put("variacionPesoKg", "Se requieren dos mediciones en fechas distintas");
        }
        resumen.put("asistencias", consulta.asistenciasEntre(c.getIdCliente(), desde, hasta));
        List<SesionEntrenamiento> sesiones = sesionRepo.findByRutinaClienteIdClienteAndFechaBetween(c.getIdCliente(), desde, hasta);
        resumen.put("sesionesRegistradas", sesiones.size());
        resumen.put("sesionesCompletadas", sesiones.stream().filter(SesionEntrenamiento::isCompletada).count());
        resumen.put("rutinas", rutinaRepo.findByClienteIdClienteOrderByFechaCreacionDescIdRutinaDesc(c.getIdCliente()).stream()
                .filter(r -> !r.getFechaCreacion().isAfter(hasta) && (r.getFechaFinalizacion() == null || !r.getFechaFinalizacion().isBefore(desde)))
                .map(r -> Mapeador.nombreRutina(r) + " (" + (r.isActivo() ? "activa" : "finalizada") + ")").toList());
        return reporte("RP-06", "Reporte de progreso del cliente", "Mediciones, asistencias y cumplimiento de sesiones.",
                f.usadosCon(Map.of("desde", desde, "hasta", hasta, "idCliente", c.getIdCliente())),
                List.of(col("fecha", "Fecha", "fecha"), col("peso", "Peso (kg)", "numero"),
                        col("grasaCorporal", "Grasa corporal (%)", "numero"), col("medidas", "Medidas (cm)"),
                        col("observaciones", "Observaciones")),
                filas, resumen);
    }

    private static String medidasTexto(Map<String, Object> medidas) {
        if (medidas == null || medidas.isEmpty()) return "";
        return medidas.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
    }

    // ------------------------------------------------------------------ RP-07

    private ReporteDto rp07(Filtros f, UsuarioAutenticado u) {
        Cliente c = clienteDelReporte(f, u);
        LocalDate hoy = consulta.hoy();
        LocalDate desde = f.fecha("desde", hoy.minusDays(6));
        LocalDate hasta = f.fecha("hasta", hoy);
        String tipo = f.texto("tipoComida");
        List<RegistroComida> registros = comidaRepo
                .findByClienteIdClienteAndFechaBetweenOrderByFechaDescIdRegistroDesc(c.getIdCliente(), desde, hasta).stream()
                .filter(r -> tipo == null || r.getTipoComida().name().equals(tipo)).toList();
        List<Map<String, Object>> filas = registros.stream().map(r -> fila("fecha", r.getFecha(),
                "tipoComida", r.getTipoComida().name(), "descripcion", r.getDescripcion(), "calorias", r.getCalorias(),
                "proteinas", r.getProteinas(), "carbohidratos", r.getCarbohidratos(), "grasas", r.getGrasas())).toList();
        PlanAlimenticio plan = alimentacionServicio.planActivo(c.getIdCliente());
        Map<LocalDate, Integer> porDia = new TreeMap<>(Comparator.reverseOrder());
        registros.forEach(r -> porDia.merge(r.getFecha(), r.getCalorias() == null ? 0 : r.getCalorias(), Integer::sum));
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("cliente", c.getNombreCompleto());
        resumen.put("objetivo", plan != null ? plan.getObjetivo() : c.getObjetivo());
        resumen.put("caloriasObjetivo", plan != null ? plan.getCaloriasObjetivo() : null);
        resumen.put("diasConRegistro", porDia.size());
        List<Map<String, Object>> totales = new ArrayList<>();
        porDia.forEach((dia, kcal) -> {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("fecha", dia);
            t.put("calorias", kcal);
            if (plan != null && plan.getCaloriasObjetivo() != null) t.put("pctMeta", Math.round(kcal * 100.0 / plan.getCaloriasObjetivo()));
            totales.add(t);
        });
        resumen.put("totalesDiarios", totales);
        if (plan != null && plan.getContenido() != null) resumen.put("recomendaciones", plan.getContenido().get("recomendaciones"));
        return reporte("RP-07", "Reporte de seguimiento alimenticio",
                "Registros de alimentación frente al objetivo personal y la meta calórica diaria.",
                f.usadosCon(Map.of("desde", desde, "hasta", hasta, "idCliente", c.getIdCliente()), "tipoComida"),
                List.of(col("fecha", "Fecha", "fecha"), col("tipoComida", "Tipo", "estado"), col("descripcion", "Descripción"),
                        col("calorias", "Calorías", "numero"), col("proteinas", "Proteínas (g)", "numero"),
                        col("carbohidratos", "Carbohidratos (g)", "numero"), col("grasas", "Grasas (g)", "numero")),
                filas, resumen);
    }

    // ------------------------------------------------------------------ RP-08

    private ReporteDto rp08(UsuarioAutenticado u) {
        LocalDate hoy = consulta.hoy();
        List<Map<String, Object>> filas = new ArrayList<>();
        if (u.esAdministrador()) {
            var d = dashboardServicio.dashboard();
            filas.add(indicador("Clientes activos", d.clientesActivos()));
            filas.add(indicador("Clientes nuevos este mes", d.nuevosEsteMes()));
            filas.add(indicador("Membresías por vencer (7 días)", d.membresiasPorVencer()));
            filas.add(indicador("Pagos del mes", Textos.soles(d.pagosMes())));
            d.pagosPorMetodo().forEach(m -> filas.add(indicador("Pagos · " + m.etiqueta(), Textos.soles(m.monto()))));
            filas.add(indicador("Asistencias hoy", d.asistenciasHoy()));
            filas.add(indicador("Asistencias del mes",
                    accesoRepo.entre(hoy.withDayOfMonth(1).atStartOfDay(), hoy.plusDays(1).atStartOfDay()).size()));
            d.rutinasPorEstado().forEach(r -> filas.add(indicador("Rutinas · " + r.etiqueta(), r.cantidad())));
        } else {
            Cliente c = consulta.cliente(u.id());
            var est = consulta.estadisticas(c);
            filas.add(indicador("Asistencias del mes", consulta.asistenciasEntre(c.getIdCliente(), hoy.withDayOfMonth(1), hoy)));
            filas.add(indicador("Último peso", est.pesoActual() == null ? "Sin registro" : est.pesoActual() + " kg"));
            filas.add(indicador("Variación de peso", est.variacionPesoPct() == null ? "—" : est.variacionPesoPct() + " %"));
            consulta.rutinaActiva(c.getIdCliente()).ifPresentOrElse(
                    r -> filas.add(indicador("Rutina", Mapeador.nombreRutina(r) + " · " + Textos.capitalizar(Mapeador.estadoRutina(r, hoy)))),
                    () -> filas.add(indicador("Rutina", "Sin rutina activa")));
            filas.add(indicador("Cumplimiento de rutina", est.cumplimientoRutinasPct() + " %"));
            var alim = alimentacionServicio.alimentacion(c.getIdCliente());
            filas.add(indicador("Calorías de hoy", alim.hoy().calorias()
                    + (alim.hoy().caloriasObjetivo() == null ? "" : " de " + alim.hoy().caloriasObjetivo() + " kcal")));
            filas.add(indicador("Recomendación", alim.sugerencia()));
        }
        return reporte("RP-08", "Reporte resumen del dashboard",
                u.esAdministrador() ? "Vista del administrador" : "Vista del cliente", Map.of(),
                List.of(col("indicador", "Indicador"), col("valor", "Valor")), filas, Map.of("generadoPara", u.rol()));
    }

    // ------------------------------------------------------------------ Apoyo

    /** Cliente fijo para el rol cliente; para el entrenador, solo uno de sus asignados. */
    private Cliente clienteDelReporte(Filtros f, UsuarioAutenticado u) {
        if (u.esCliente()) return consulta.cliente(u.id());
        Integer idCliente = f.entero("idCliente");
        if (idCliente == null) throw new ReglaNegocioException("Seleccione un cliente", Map.of("idCliente", "Seleccione un cliente"));
        return consulta.clienteDeEntrenador(idCliente, u.id());
    }

    private Set<Integer> asignados(Integer idEntrenador) {
        return clienteRepo.asignadosA(idEntrenador).stream().map(Cliente::getIdCliente).collect(Collectors.toSet());
    }

    private static boolean coincide(Cliente c, String q) {
        if (q == null) return true;
        String t = q.toLowerCase();
        return c.getNombreCompleto().toLowerCase().contains(t) || c.getCodigo().toLowerCase().contains(t) || c.getDni().contains(t);
    }

    private static Map<String, Object> fila(Object... claveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) m.put((String) claveValor[i], claveValor[i + 1]);
        return m;
    }

    private static Map<String, Object> indicador(String nombre, Object valor) {
        return fila("indicador", nombre, "valor", valor);
    }

    private static Map<String, Long> contar(List<Map<String, Object>> filas, String clave) {
        return filas.stream().collect(Collectors.groupingBy(r -> String.valueOf(r.get(clave)), TreeMap::new, Collectors.counting()));
    }

    private static ColumnaDto col(String clave, String titulo) {
        return new ColumnaDto(clave, titulo, "texto");
    }

    private static ColumnaDto col(String clave, String titulo, String tipo) {
        return new ColumnaDto(clave, titulo, tipo);
    }

    private static ReporteDto reporte(String codigo, String titulo, String descripcion, Map<String, Object> filtros,
                                      List<ColumnaDto> columnas, List<Map<String, Object>> filas, Map<String, Object> resumen) {
        return new ReporteDto(codigo, titulo, descripcion, LocalDateTime.now().withNano(0), filtros, columnas, filas, resumen);
    }

    /** Lectura tolerante de los filtros recibidos como parámetros de la URL. */
    private static final class Filtros {
        private final Map<String, String> valores;

        Filtros(Map<String, String> valores) {
            this.valores = valores == null ? Map.of() : valores;
        }

        String texto(String clave) {
            String v = valores.get(clave);
            return v == null || v.isBlank() ? null : v.trim();
        }

        Integer entero(String clave) {
            String v = texto(clave);
            if (v == null) return null;
            try {
                return Integer.valueOf(v);
            } catch (NumberFormatException e) {
                throw new ReglaNegocioException("Filtro inválido: " + clave);
            }
        }

        LocalDate fecha(String clave, LocalDate porDefecto) {
            String v = texto(clave);
            if (v == null) return porDefecto;
            try {
                return LocalDate.parse(v);
            } catch (DateTimeParseException e) {
                throw new ReglaNegocioException("Fecha inválida en " + clave + " (use AAAA-MM-DD)");
            }
        }

        Map<String, Object> usados(String... claves) {
            return usadosCon(Map.of(), claves);
        }

        Map<String, Object> usadosCon(Map<String, Object> fijos, String... claves) {
            Map<String, Object> m = new LinkedHashMap<>(fijos);
            for (String c : claves) if (texto(c) != null) m.put(c, texto(c));
            return m;
        }
    }

    /** Exporta cualquier reporte a CSV (separador ; para Excel en español). */
    public static String aCsv(ReporteDto r) {
        Function<Object, String> celda = v -> {
            if (v == null) return "";
            String s = v instanceof LocalDate d ? d.format(Textos.FECHA) : String.valueOf(v);
            return s.contains(";") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
        };
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(celda.apply(r.codigo() + " - " + r.titulo())).append('\n');
        sb.append(r.columnas().stream().map(c -> celda.apply(c.titulo())).collect(Collectors.joining(";"))).append('\n');
        for (Map<String, Object> fila : r.filas()) {
            sb.append(r.columnas().stream().map(c -> celda.apply(fila.get(c.clave()))).collect(Collectors.joining(";"))).append('\n');
        }
        return sb.toString();
    }
}
