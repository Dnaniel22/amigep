package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.ComunDtos.EstadisticasProgresoDto;
import pe.edu.utp.amigep.excepcion.AccesoDenegadoException;
import pe.edu.utp.amigep.excepcion.NoEncontradoException;
import pe.edu.utp.amigep.modelo.*;
import pe.edu.utp.amigep.repositorio.*;
import pe.edu.utp.amigep.servicio.ia.PerfilFisico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

/** Consultas sobre un cliente que reutilizan la app, el panel y los reportes. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaClienteServicio {

    private final ClienteRepositorio clienteRepo;
    private final SuscripcionRepositorio suscripcionRepo;
    private final ProgresoRepositorio progresoRepo;
    private final AccesoRepositorio accesoRepo;
    private final RutinaRepositorio rutinaRepo;
    private final SesionEntrenamientoRepositorio sesionRepo;
    private final Mapeador mapeador;
    private final Clock reloj;

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    public Cliente cliente(Integer idCliente) {
        return clienteRepo.findById(idCliente)
                .orElseThrow(() -> new NoEncontradoException("Cliente no encontrado"));
    }

    /** El entrenador solo accede a los clientes que tiene asignados (RF-16, sección 13.1). */
    public Cliente clienteDeEntrenador(Integer idCliente, Integer idEntrenador) {
        Cliente c = cliente(idCliente);
        if (c.getEntrenador() == null || !c.getEntrenador().getIdTrabajador().equals(idEntrenador)) {
            throw new AccesoDenegadoException("Este cliente no está asignado a usted");
        }
        return c;
    }

    /** Membresía vigente con mayor caducidad; si no hay, la más reciente (para mostrarla como vencida). */
    public Optional<Suscripcion> membresiaActual(Integer idCliente) {
        LocalDate hoy = hoy();
        List<Suscripcion> vigentes = suscripcionRepo.vigentesDeCliente(idCliente, hoy);
        if (!vigentes.isEmpty()) return Optional.of(vigentes.get(0));
        return suscripcionRepo.deCliente(idCliente).stream()
                .filter(s -> !s.getFechaInicio().isAfter(hoy))
                .findFirst();
    }

    /** Renovación ya pagada que empieza en el futuro. */
    public Optional<Suscripcion> membresiaProgramada(Integer idCliente) {
        LocalDate hoy = hoy();
        return suscripcionRepo.deCliente(idCliente).stream()
                .filter(s -> s.isActivo() && s.getFechaInicio().isAfter(hoy))
                .reduce((a, b) -> b);
    }

    public Optional<Rutina> rutinaActiva(Integer idCliente) {
        return rutinaRepo.findFirstByClienteIdClienteAndActivoTrueOrderByFechaCreacionDescIdRutinaDesc(idCliente);
    }

    public long sesionesCompletadas(Rutina r) {
        return sesionRepo.countByRutinaIdRutinaAndCompletadaTrue(r.getIdRutina());
    }

    public Optional<BigDecimal> pesoActual(Integer idCliente) {
        return progresoRepo.findFirstByClienteIdClienteAndPesoNotNullOrderByFechaDescIdProgresoDesc(idCliente)
                .map(Progreso::getPeso);
    }

    public Integer edad(Cliente c) {
        return c.getFechaNacimiento() == null ? null : Period.between(c.getFechaNacimiento(), hoy()).getYears();
    }

    public PerfilFisico perfilFisico(Cliente c) {
        return new PerfilFisico(pesoActual(c.getIdCliente()).orElse(null), c.getAltura(), edad(c));
    }

    public int asistenciasEntre(Integer idCliente, LocalDate desde, LocalDate hasta) {
        return accesoRepo.deClienteEntre(idCliente, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()).size();
    }

    /** RF-14: progreso integrando peso, asistencia y cumplimiento de rutinas. */
    public EstadisticasProgresoDto estadisticas(Cliente c) {
        LocalDate hoy = hoy();
        List<Progreso> mediciones = progresoRepo.findByClienteIdClienteOrderByFechaAscIdProgresoAsc(c.getIdCliente());
        List<Progreso> conPeso = mediciones.stream().filter(p -> p.getPeso() != null).toList();
        BigDecimal pesoInicial = conPeso.isEmpty() ? null : conPeso.get(0).getPeso();
        BigDecimal pesoActual = conPeso.isEmpty() ? null : conPeso.get(conPeso.size() - 1).getPeso();
        Double variacion = null;
        // Solo con al menos dos mediciones en fechas distintas (RP-06)
        if (conPeso.size() >= 2 && !conPeso.get(0).getFecha().equals(conPeso.get(conPeso.size() - 1).getFecha())) {
            variacion = pesoActual.subtract(pesoInicial).multiply(BigDecimal.valueOf(100))
                    .divide(pesoInicial, 1, RoundingMode.HALF_UP).doubleValue();
        }

        int asistencias30 = asistenciasEntre(c.getIdCliente(), hoy.minusDays(29), hoy);
        Optional<Rutina> rutina = rutinaActiva(c.getIdCliente());
        int sesionesSemana = rutina.map(Mapeador::sesionesPorSemana).orElse(3);
        int esperadas30 = (int) Math.round(sesionesSemana * 30 / 7.0);
        int asistenciaPct = (int) Math.min(100, Math.round(asistencias30 * 100.0 / Math.max(1, esperadas30)));

        long completadas = rutina.map(this::sesionesCompletadas).orElse(0L);
        int cumplimiento = rutina.map(r -> Mapeador.cumplimiento(r, completadas, hoy)).orElse(0);

        List<pe.edu.utp.amigep.dto.ComunDtos.ProgresoDto> lista = mediciones.stream().map(mapeador::progreso).toList();
        return new EstadisticasProgresoDto(pesoActual, pesoInicial, variacion, asistencias30, asistenciaPct,
                completadas, cumplimiento, lista);
    }
}
