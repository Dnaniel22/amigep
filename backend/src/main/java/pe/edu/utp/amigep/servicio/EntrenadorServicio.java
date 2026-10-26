package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.ComunDtos.AccesoDto;
import pe.edu.utp.amigep.dto.ComunDtos.EstadisticasProgresoDto;
import pe.edu.utp.amigep.dto.EntrenadorDtos.*;
import pe.edu.utp.amigep.excepcion.NoEncontradoException;
import pe.edu.utp.amigep.modelo.Cliente;
import pe.edu.utp.amigep.modelo.Rutina;
import pe.edu.utp.amigep.modelo.Trabajador;
import pe.edu.utp.amigep.repositorio.AccesoRepositorio;
import pe.edu.utp.amigep.repositorio.ClienteRepositorio;
import pe.edu.utp.amigep.repositorio.TrabajadorRepositorio;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Pantallas del entrenador: E1 Mis clientes, E2 Detalle, Rutinas, Agenda y Perfil. */
@Service
@RequiredArgsConstructor
public class EntrenadorServicio {

    private final ClienteRepositorio clienteRepo;
    private final TrabajadorRepositorio trabajadorRepo;
    private final AccesoRepositorio accesoRepo;
    private final ConsultaClienteServicio consulta;
    private final RutinaServicio rutinaServicio;
    private final Mapeador mapeador;

    @Transactional(readOnly = true)
    public ResumenClientesDto clientes(Integer idEntrenador, String q, String estado) {
        LocalDate hoy = consulta.hoy();
        List<ClienteAsignadoDto> todos = clienteRepo.asignadosA(idEntrenador).stream().map(c -> {
            Optional<Rutina> r = consulta.rutinaActiva(c.getIdCliente());
            String estadoRutina = r.map(x -> Mapeador.estadoRutina(x, hoy)).orElse("PENDIENTE");
            if (estadoRutina.equals("FINALIZADA")) estadoRutina = "PENDIENTE";
            String detalle = r.isEmpty() || estadoRutina.equals("PENDIENTE") ? "Sin rutina asignada"
                    : Textos.capitalizar(r.get().getObjetivo()) + " · Semana " + Mapeador.semanaActual(r.get(), hoy)
                    + "/" + Mapeador.semanasTotales(r.get());
            return new ClienteAsignadoDto(c.getIdCliente(), c.getNombre(), c.getApellido(),
                    Textos.iniciales(c.getNombre(), c.getApellido()), c.getObjetivo(),
                    c.getNivel() == null ? null : c.getNivel().name(), r.map(Mapeador::nombreRutina).orElse(null),
                    r.map(x -> Mapeador.semanaActual(x, hoy)).orElse(0), r.map(Mapeador::semanasTotales).orElse(0),
                    estadoRutina, detalle);
        }).toList();
        Map<String, Long> conteo = todos.stream().collect(Collectors.groupingBy(ClienteAsignadoDto::estadoRutina, Collectors.counting()));
        String texto = q == null ? "" : q.trim().toLowerCase();
        List<ClienteAsignadoDto> filtrados = todos.stream()
                .filter(c -> texto.isEmpty() || (c.nombre() + " " + c.apellido()).toLowerCase().contains(texto))
                .filter(c -> estado == null || estado.isBlank() || estado.equals(c.estadoRutina()))
                .toList();
        return new ResumenClientesDto(todos.size(), conteo.getOrDefault("VIGENTE", 0L).intValue(),
                conteo.getOrDefault("POR_FINALIZAR", 0L).intValue(), conteo.getOrDefault("PENDIENTE", 0L).intValue(), filtrados);
    }

    @Transactional(readOnly = true)
    public DetalleClienteDto detalle(Integer idEntrenador, Integer idCliente) {
        Cliente c = consulta.clienteDeEntrenador(idCliente, idEntrenador);
        LocalDate hoy = consulta.hoy();
        EstadisticasProgresoDto est = consulta.estadisticas(c);
        Optional<Rutina> rutina = consulta.rutinaActiva(idCliente);
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        int asistenciasMes = consulta.asistenciasEntre(idCliente, inicioMes, hoy);
        int sesionesSemana = rutina.map(Mapeador::sesionesPorSemana).orElse(3);
        int diasMes = Math.max(1, (int) Math.round(sesionesSemana * hoy.getDayOfMonth() / 7.0));
        List<AccesoDto> ultimas = accesoRepo.findTop10ByClienteIdClienteOrderByFechaHoraDesc(idCliente).stream()
                .limit(5).map(mapeador::acceso).toList();
        return new DetalleClienteDto(c.getIdCliente(), c.getNombre(), c.getApellido(),
                Textos.iniciales(c.getNombre(), c.getApellido()), c.getEmail(), c.getTelefono(), c.getObjetivo(),
                c.getNivel() == null ? null : c.getNivel().name(), c.getAltura(), consulta.edad(c),
                consulta.membresiaActual(idCliente).map(s -> mapeador.membresia(s, hoy)).orElse(null),
                rutina.map(r -> mapeador.rutina(r, consulta.sesionesCompletadas(r), hoy)).orElse(null),
                est.pesoActual(), est.variacionPesoPct(), asistenciasMes, diasMes, ultimas, est);
    }

    /** Agenda: calendario de asistencias de los clientes asignados. */
    @Transactional(readOnly = true)
    public List<AccesoDto> agenda(Integer idEntrenador, LocalDate desde, LocalDate hasta) {
        LocalDate hoy = consulta.hoy();
        LocalDate d = desde != null ? desde : hoy.minusDays(6);
        LocalDate h = hasta != null ? hasta : hoy;
        Set<Integer> asignados = clienteRepo.asignadosA(idEntrenador).stream().map(Cliente::getIdCliente).collect(Collectors.toSet());
        return accesoRepo.entre(d.atStartOfDay(), h.plusDays(1).atStartOfDay()).stream()
                .filter(a -> asignados.contains(a.getCliente().getIdCliente()))
                .map(mapeador::acceso).toList();
    }

    @Transactional(readOnly = true)
    public PerfilEntrenadorDto perfil(Integer idEntrenador) {
        Trabajador t = trabajadorRepo.findById(idEntrenador).orElseThrow(() -> new NoEncontradoException("Entrenador no encontrado"));
        int asignados = (int) clienteRepo.countByEntrenadorIdTrabajadorAndActivoTrue(idEntrenador);
        int activas = (int) rutinaServicio.rutinasDeEntrenador(idEntrenador).stream().filter(r -> r.activo()).count();
        return new PerfilEntrenadorDto(t.getIdTrabajador(), t.getNombre(), t.getApellido(),
                Textos.iniciales(t.getNombre(), t.getApellido()), t.getEmail(), t.getEspecialidad(), asignados, activas);
    }
}
