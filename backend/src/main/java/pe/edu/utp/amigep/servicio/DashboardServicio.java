package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.AdminDtos.*;
import pe.edu.utp.amigep.modelo.Cliente;
import pe.edu.utp.amigep.modelo.Rutina;
import pe.edu.utp.amigep.modelo.enums.EstadoPago;
import pe.edu.utp.amigep.repositorio.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** RF-15 / A1: indicadores del gimnasio para el administrador (aplica reglas de RP-01 a RP-03). */
@Service
@RequiredArgsConstructor
public class DashboardServicio {

    private final ClienteRepositorio clienteRepo;
    private final SuscripcionRepositorio suscripcionRepo;
    private final PagoRepositorio pagoRepo;
    private final AccesoRepositorio accesoRepo;
    private final RutinaRepositorio rutinaRepo;
    private final MembresiaServicio membresiaServicio;
    private final ConsultaClienteServicio consulta;

    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        LocalDate hoy = consulta.hoy();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        long clientesActivos = suscripcionRepo.contarClientesConMembresiaVigente(hoy);
        long nuevos = clienteRepo.countByActivoTrueAndFechaRegistroGreaterThanEqual(inicioMes);

        var porVencer = suscripcionRepo.porVencer(hoy, hoy.plusDays(Mapeador.DIAS_POR_VENCER));
        List<VencimientoDto> proximas = porVencer.stream().limit(8).map(s -> new VencimientoDto(
                s.getCliente().getIdCliente(), s.getIdSuscripcion(), s.getCliente().getNombreCompleto(),
                s.getPlan().getNombre(), s.getFechaCaducidad(), ChronoUnit.DAYS.between(hoy, s.getFechaCaducidad()))).toList();

        BigDecimal pagosMes = pagoRepo.sumar(EstadoPago.VALIDADO, inicioMes, hoy);
        LocalDate inicioAnterior = inicioMes.minusMonths(1);
        LocalDate mismoDiaAnterior = inicioAnterior.plusDays(Math.min(hoy.getDayOfMonth(), inicioAnterior.lengthOfMonth()) - 1);
        BigDecimal pagosAnterior = pagoRepo.sumar(EstadoPago.VALIDADO, inicioAnterior, mismoDiaAnterior);
        Double variacion = pagosAnterior.signum() == 0 ? null
                : pagosMes.subtract(pagosAnterior).multiply(BigDecimal.valueOf(100)).divide(pagosAnterior, 1, RoundingMode.HALF_UP).doubleValue();

        long asistenciasHoy = accesoRepo.contarClientesDistintos(hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay());

        Map<String, BigDecimal> porMetodo = membresiaServicio.totalesPorMetodo(inicioMes, hoy);
        BigDecimal total = porMetodo.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PagoMetodoDto> metodos = new ArrayList<>();
        for (String m : List.of("YAPE_PLIN", "TARJETA", "EFECTIVO")) {
            BigDecimal monto = porMetodo.getOrDefault(m, BigDecimal.ZERO);
            int pct = total.signum() == 0 ? 0 : monto.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
            metodos.add(new PagoMetodoDto(m, Textos.etiquetaMetodo(m), monto, pct));
        }

        return new DashboardDto(clientesActivos, nuevos, porVencer.size(), pagosMes, variacion, asistenciasHoy,
                clientesActivos, proximas, metodos, rutinasPorEstado(hoy));
    }

    /** Rutinas por estado (RP-08, vista administrador). */
    public List<ConteoDto> rutinasPorEstado(LocalDate hoy) {
        Map<Integer, Rutina> activas = new HashMap<>();
        for (Rutina r : rutinaRepo.todasConDetalle()) {
            if (r.isActivo()) activas.putIfAbsent(r.getCliente().getIdCliente(), r);
        }
        long vigentes = 0, porFinalizar = 0, sinRutina = 0;
        for (Cliente c : clienteRepo.findByActivoTrue()) {
            Rutina r = activas.get(c.getIdCliente());
            String estado = r == null ? "PENDIENTE" : Mapeador.estadoRutina(r, hoy);
            switch (estado) {
                case "VIGENTE" -> vigentes++;
                case "POR_FINALIZAR" -> porFinalizar++;
                default -> sinRutina++;
            }
        }
        return List.of(new ConteoDto("VIGENTE", "Vigentes", vigentes),
                new ConteoDto("POR_FINALIZAR", "Por finalizar", porFinalizar),
                new ConteoDto("PENDIENTE", "Sin rutina", sinRutina));
    }
}
