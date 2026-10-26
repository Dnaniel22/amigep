package pe.edu.utp.amigep.servicio;

import org.springframework.stereotype.Component;
import pe.edu.utp.amigep.dto.AdminDtos.TrabajadorDto;
import pe.edu.utp.amigep.dto.ComunDtos.*;
import pe.edu.utp.amigep.modelo.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Convierte entidades en DTOs y concentra los cálculos derivados (estado de membresía, avance de rutina). */
@Component
public class Mapeador {

    /** Días de anticipación para considerar una membresía "por vencer" (RP-02). */
    public static final int DIAS_POR_VENCER = 7;

    public PlanDto plan(Plan p) {
        return new PlanDto(p.getIdPlan(), p.getNombre(), p.getDuracionDias(), p.getPrecio(), p.getDescripcion());
    }

    public static String estadoMembresia(Suscripcion s, LocalDate hoy, int diasAnticipacion) {
        if (!s.isActivo()) return "SUSPENDIDA";
        if (hoy.isBefore(s.getFechaInicio())) return "PROGRAMADA";
        if (hoy.isAfter(s.getFechaCaducidad())) return "VENCIDA";
        if (ChronoUnit.DAYS.between(hoy, s.getFechaCaducidad()) <= diasAnticipacion) return "POR_VENCER";
        return "VIGENTE";
    }

    public MembresiaDto membresia(Suscripcion s, LocalDate hoy) {
        if (s == null) return null;
        long dias = Math.max(0, ChronoUnit.DAYS.between(hoy, s.getFechaCaducidad()));
        return new MembresiaDto(s.getIdSuscripcion(), s.getPlan().getIdPlan(), s.getPlan().getNombre(),
                s.getPlan().getPrecio(), s.getFechaInicio(), s.getFechaCaducidad(), s.isActivo(),
                estadoMembresia(s, hoy, DIAS_POR_VENCER), dias);
    }

    public PagoDto pago(Pago p) {
        Suscripcion s = p.getSuscripcion();
        Cliente c = s.getCliente();
        return new PagoDto(p.getIdPago(), s.getIdSuscripcion(), c.getIdCliente(), c.getNombreCompleto(),
                s.getPlan().getNombre(), p.getMonto(), p.getFechaPago(), p.getMetodoPago().name(), p.getEstado().name());
    }

    public AccesoDto acceso(Acceso a) {
        Cliente c = a.getCliente();
        return new AccesoDto(a.getIdAcceso(), c.getIdCliente(), c.getCodigo(), c.getNombreCompleto(),
                a.getFechaHora(), a.getTipo().name());
    }

    public NotificacionDto notificacion(Notificacion n) {
        Cliente c = n.getCliente();
        return new NotificacionDto(n.getIdNotificacion(), c.getIdCliente(), c.getNombreCompleto(), n.getTitulo(),
                n.getMensaje(), n.getFechaEnvio(), n.isLeido(), n.getTipo().name());
    }

    public EntrenadorResumenDto entrenadorResumen(Trabajador t) {
        if (t == null) return null;
        return new EntrenadorResumenDto(t.getIdTrabajador(), t.getNombreCompleto(),
                Textos.iniciales(t.getNombre(), t.getApellido()), t.getEspecialidad());
    }

    public TrabajadorDto trabajador(Trabajador t, long clientesAsignados) {
        return new TrabajadorDto(t.getIdTrabajador(), t.getNombre(), t.getApellido(),
                Textos.iniciales(t.getNombre(), t.getApellido()), t.getDni(), t.getEmail(), t.getRol().name(),
                t.getEspecialidad(), t.isActivo(), clientesAsignados);
    }

    public ProgresoDto progreso(Progreso p) {
        return new ProgresoDto(p.getIdProgreso(), p.getFecha(), p.getPeso(), p.getGrasaCorporal(), p.getMedidas(),
                p.getObservaciones());
    }

    public SesionDto sesion(SesionEntrenamiento s) {
        return new SesionDto(s.getIdSesion(), s.getFecha(), s.isCompletada(), s.getObservaciones());
    }

    public RegistroComidaDto registroComida(RegistroComida r) {
        return new RegistroComidaDto(r.getIdRegistro(), r.getFecha(), r.getTipoComida().name(), r.getDescripcion(),
                r.getCalorias(), r.getProteinas(), r.getCarbohidratos(), r.getGrasas());
    }

    public PlanAlimenticioDto planAlimenticio(PlanAlimenticio p) {
        if (p == null) return null;
        return new PlanAlimenticioDto(p.getIdPlanAlimenticio(), p.getObjetivo(), p.getRitmo(), p.getCaloriasObjetivo(),
                p.getContenido(), p.getFechaCreacion());
    }

    // ------------------------------------------------------------------ Rutinas

    public static String nombreRutina(Rutina r) {
        Object nombre = r.getContenido() == null ? null : r.getContenido().get("nombre");
        return nombre != null ? nombre.toString() : r.getObjetivo();
    }

    public static int semanasTotales(Rutina r) {
        Map<String, Object> c = r.getContenido();
        if (c != null && c.get("semanas") instanceof Number n) return Math.max(1, n.intValue());
        if (r.getFechaFinalizacion() != null) {
            return (int) Math.max(1, Math.ceil(ChronoUnit.DAYS.between(r.getFechaCreacion(), r.getFechaFinalizacion()) / 7.0));
        }
        return 8;
    }

    public static int semanaActual(Rutina r, LocalDate hoy) {
        long dias = Math.max(0, ChronoUnit.DAYS.between(r.getFechaCreacion(), hoy));
        return (int) Math.min(semanasTotales(r), dias / 7 + 1);
    }

    public static int sesionesPorSemana(Rutina r) {
        Map<String, Object> c = r.getContenido();
        if (c != null && c.get("sesionesPorSemana") instanceof Number n) return Math.max(1, n.intValue());
        String digitos = r.getRitmo() == null ? "" : r.getRitmo().replaceAll("\\D+", " ").trim();
        return digitos.isEmpty() ? 3 : Integer.parseInt(digitos.split(" ")[0]);
    }

    /** Porcentaje de sesiones completadas respecto a las esperadas desde que inició la rutina. */
    public static int cumplimiento(Rutina r, long completadas, LocalDate hoy) {
        LocalDate fin = r.getFechaFinalizacion() != null && r.getFechaFinalizacion().isBefore(hoy) ? r.getFechaFinalizacion() : hoy;
        long dias = ChronoUnit.DAYS.between(r.getFechaCreacion(), fin) + 1;
        long esperadas = Math.round(sesionesPorSemana(r) * dias / 7.0);
        if (esperadas <= 0) return completadas > 0 ? 100 : 0;
        return (int) Math.min(100, Math.round(completadas * 100.0 / esperadas));
    }

    /** VIGENTE, POR_FINALIZAR (última semana), FINALIZADA o INACTIVA. */
    public static String estadoRutina(Rutina r, LocalDate hoy) {
        if (!r.isActivo()) return "INACTIVA";
        if (r.getFechaFinalizacion() == null) return "VIGENTE";
        if (hoy.isAfter(r.getFechaFinalizacion())) return "FINALIZADA";
        if (ChronoUnit.DAYS.between(hoy, r.getFechaFinalizacion()) <= 7) return "POR_FINALIZAR";
        return "VIGENTE";
    }

    public RutinaDto rutina(Rutina r, long completadas, LocalDate hoy) {
        if (r == null) return null;
        Cliente c = r.getCliente();
        return new RutinaDto(r.getIdRutina(), c.getIdCliente(), c.getNombreCompleto(), nombreRutina(r),
                r.getObjetivo(), r.getNivel().name(), r.getRitmo(), r.getOrigen().name(),
                r.getTrabajador() == null ? null : r.getTrabajador().getNombreCompleto(),
                r.getFechaCreacion(), r.getFechaFinalizacion(), r.isActivo(), semanaActual(r, hoy), semanasTotales(r),
                completadas, cumplimiento(r, completadas, hoy), estadoRutina(r, hoy), r.getContenido());
    }
}
