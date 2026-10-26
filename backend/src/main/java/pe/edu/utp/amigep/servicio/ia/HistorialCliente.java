package pe.edu.utp.amigep.servicio.ia;

import pe.edu.utp.amigep.modelo.enums.EstadoPago;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Historial de un cliente (asistencias, suscripciones, pagos y rutinas) a partir del cual se calculan
 * las características del modelo de abandono en cualquier fecha de corte. Así se usa el mismo cálculo
 * para predecir hoy y para construir los ejemplos de entrenamiento del pasado.
 */
public record HistorialCliente(Integer idCliente, LocalDate fechaRegistro, List<LocalDateTime> accesos,
                               List<PeriodoSuscripcion> suscripciones, List<PagoHist> pagos,
                               List<PeriodoRutina> rutinas) {

    public record PeriodoSuscripcion(LocalDate inicio, LocalDate fin, boolean activo) {
    }

    public record PagoHist(LocalDate fecha, EstadoPago estado) {
    }

    public record PeriodoRutina(LocalDate inicio, LocalDate fin, boolean activo) {
    }

    /** Vector de características (todas normalizadas a [0, 1]) y su explicación legible. */
    public record Caracteristicas(double[] x, List<String> factores) {
    }

    public static final String[] NOMBRES = {
            "diasSinAsistir", "caidaAsistencia", "membresiaVencida", "porVencer",
            "sinRutinaActiva", "pagosRechazados", "bajaFrecuencia"};

    public Caracteristicas caracteristicas(LocalDate corte) {
        LocalDateTime finCorte = corte.plusDays(1).atStartOfDay();
        LocalDateTime hace30 = finCorte.minusDays(30);
        LocalDateTime hace60 = finCorte.minusDays(60);

        LocalDateTime ultimo = null;
        int asist30 = 0, asistPrev30 = 0;
        for (LocalDateTime a : accesos) {
            if (!a.isBefore(finCorte)) continue;
            if (ultimo == null || a.isAfter(ultimo)) ultimo = a;
            if (!a.isBefore(hace30)) asist30++;
            else if (!a.isBefore(hace60)) asistPrev30++;
        }
        LocalDate referencia = ultimo != null ? ultimo.toLocalDate() : fechaRegistro;
        long diasSinAsistir = Math.max(0, ChronoUnit.DAYS.between(referencia, corte));
        double caida = asistPrev30 > 0 ? Math.max(0, (asistPrev30 - asist30) / (double) asistPrev30) : 0;

        // Membresía: vigente en la fecha de corte y días hasta el fin de la cobertura (incluye renovaciones)
        LocalDate finCobertura = null;
        boolean tuvoMembresia = false;
        for (PeriodoSuscripcion s : suscripciones) {
            if (!s.activo() || s.inicio().isAfter(corte)) continue;
            tuvoMembresia = true;
            if (!s.fin().isBefore(corte) && (finCobertura == null || s.fin().isAfter(finCobertura))) finCobertura = s.fin();
        }
        if (finCobertura != null) {
            // Si hay una suscripción posterior que empieza justo después, la cobertura continúa
            boolean extendida = true;
            while (extendida) {
                extendida = false;
                for (PeriodoSuscripcion s : suscripciones) {
                    if (s.activo() && !s.inicio().isAfter(finCobertura.plusDays(1)) && s.fin().isAfter(finCobertura)) {
                        finCobertura = s.fin();
                        extendida = true;
                    }
                }
            }
        }
        boolean vencida = finCobertura == null;
        long diasParaVencer = vencida ? 0 : ChronoUnit.DAYS.between(corte, finCobertura);
        boolean porVencer = !vencida && diasParaVencer <= 7;

        boolean conRutina = rutinas.stream().anyMatch(r -> !r.inicio().isAfter(corte)
                && (r.fin() == null || !r.fin().isBefore(corte)));
        long rechazos = pagos.stream().filter(p -> p.estado() == EstadoPago.RECHAZADO
                && !p.fecha().isAfter(corte) && p.fecha().isAfter(corte.minusDays(90))).count();

        double[] x = {
                Math.min(diasSinAsistir, 30) / 30.0,
                caida,
                vencida ? 1 : 0,
                porVencer ? 1 : 0,
                conRutina ? 0 : 1,
                Math.min(rechazos, 2) / 2.0,
                asist30 < 4 ? 1 : 0};

        List<String> factores = new ArrayList<>();
        if (diasSinAsistir >= 7) factores.add(diasSinAsistir + " días sin asistir");
        if (caida >= 0.3) factores.add(String.format(Locale.US, "Asistencia -%.0f%%", caida * 100));
        if (vencida) factores.add(tuvoMembresia ? "plan vencido" : "sin membresía");
        if (porVencer) factores.add(diasParaVencer == 0 ? "vence hoy"
                : "por vencer en " + diasParaVencer + (diasParaVencer == 1 ? " día" : " días"));
        if (!conRutina) factores.add("sin rutina activa");
        if (rechazos > 0) factores.add(rechazos == 1 ? "1 pago rechazado" : rechazos + " pagos rechazados");
        if (asist30 < 4 && asist30 > 0 && diasSinAsistir < 7) factores.add("baja frecuencia (" + asist30 + " visitas/mes)");
        return new Caracteristicas(x, factores);
    }

    /**
     * Etiqueta para entrenamiento: 1 si en los 30 días posteriores al corte el cliente no volvió a asistir
     * y terminó sin membresía vigente (abandono), 0 en caso contrario.
     */
    public int etiquetaAbandono(LocalDate corte) {
        LocalDateTime desde = corte.plusDays(1).atStartOfDay();
        LocalDateTime hasta = desde.plusDays(30);
        boolean asistio = accesos.stream().anyMatch(a -> !a.isBefore(desde) && a.isBefore(hasta));
        LocalDate dia30 = corte.plusDays(30);
        boolean vigente = suscripciones.stream().anyMatch(s -> s.activo()
                && !s.inicio().isAfter(dia30) && !s.fin().isBefore(dia30));
        return !asistio && !vigente ? 1 : 0;
    }

    /** El cliente ya era cliente (tenía o había tenido membresía) en la fecha de corte. */
    public boolean eraCliente(LocalDate corte) {
        return !fechaRegistro.isAfter(corte.minusDays(14))
                && suscripciones.stream().anyMatch(s -> !s.inicio().isAfter(corte));
    }
}
