package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.ClienteAppDtos.*;
import pe.edu.utp.amigep.dto.ComunDtos.EstadisticasProgresoDto;
import pe.edu.utp.amigep.modelo.Cliente;
import pe.edu.utp.amigep.modelo.Progreso;
import pe.edu.utp.amigep.repositorio.PagoRepositorio;
import pe.edu.utp.amigep.repositorio.ProgresoRepositorio;

import java.time.LocalDate;

/** Pantallas del cliente: C2 Inicio, C4 Membresías y pagos, C6 Perfil y progreso. */
@Service
@RequiredArgsConstructor
public class ClienteAppServicio {

    private final ConsultaClienteServicio consulta;
    private final RutinaServicio rutinaServicio;
    private final MembresiaServicio membresiaServicio;
    private final NotificacionServicio notificaciones;
    private final PagoRepositorio pagoRepo;
    private final ProgresoRepositorio progresoRepo;
    private final Mapeador mapeador;

    @Transactional(readOnly = true)
    public InicioClienteDto inicio(Integer idCliente) {
        Cliente c = consulta.cliente(idCliente);
        LocalDate hoy = consulta.hoy();
        return new InicioClienteDto(c.getIdCliente(), c.getNombre(), c.getApellido(),
                Textos.iniciales(c.getNombre(), c.getApellido()), c.getCodigoQR(),
                consulta.membresiaActual(idCliente).map(s -> mapeador.membresia(s, hoy)).orElse(null),
                rutinaServicio.sesionDeHoy(idCliente), notificaciones.noLeidas(idCliente),
                consulta.asistenciasEntre(idCliente, hoy.withDayOfMonth(1), hoy));
    }

    @Transactional(readOnly = true)
    public PerfilClienteDto perfil(Integer idCliente) {
        Cliente c = consulta.cliente(idCliente);
        EstadisticasProgresoDto est = consulta.estadisticas(c);
        String plan = consulta.membresiaActual(idCliente).map(s -> s.getPlan().getNombre()).orElse(null);
        return new PerfilClienteDto(c.getIdCliente(), c.getCodigo(), c.getNombre(), c.getApellido(),
                Textos.iniciales(c.getNombre(), c.getApellido()), c.getEmail(), c.getTelefono(), c.getDni(),
                c.getObjetivo(), c.getNivel() == null ? null : c.getNivel().name(), c.getAltura(), est.pesoActual(),
                c.getFechaNacimiento(), c.getFechaRegistro(), plan, mapeador.entrenadorResumen(c.getEntrenador()), est);
    }

    /** Editar datos (RF-03 / RF-10): datos que usa la personalización. */
    @Transactional
    public PerfilClienteDto actualizarPerfil(Integer idCliente, ActualizarPerfilRequest req) {
        Cliente c = consulta.cliente(idCliente);
        if (req.telefono() != null) c.setTelefono(Textos.vacioANull(req.telefono()));
        if (req.objetivo() != null) c.setObjetivo(Textos.vacioANull(req.objetivo()));
        if (req.nivel() != null) c.setNivel(req.nivel());
        if (req.altura() != null) c.setAltura(req.altura());
        if (req.fechaNacimiento() != null) c.setFechaNacimiento(req.fechaNacimiento());
        if (req.peso() != null && consulta.pesoActual(idCliente).map(p -> p.compareTo(req.peso()) != 0).orElse(true)) {
            Progreso p = new Progreso();
            p.setCliente(c);
            p.setFecha(consulta.hoy());
            p.setPeso(req.peso());
            progresoRepo.save(p);
        }
        return perfil(idCliente);
    }

    @Transactional(readOnly = true)
    public MembresiaClienteDto membresia(Integer idCliente) {
        LocalDate hoy = consulta.hoy();
        return new MembresiaClienteDto(
                consulta.membresiaActual(idCliente).map(s -> mapeador.membresia(s, hoy)).orElse(null),
                consulta.membresiaProgramada(idCliente).map(s -> mapeador.membresia(s, hoy)).orElse(null),
                membresiaServicio.planes(),
                pagoRepo.deCliente(idCliente).stream().map(mapeador::pago).toList(),
                "Pago en recepción · Efectivo, tarjeta o Yape/Plin");
    }

    @Transactional(readOnly = true)
    public EstadisticasProgresoDto progreso(Integer idCliente) {
        return consulta.estadisticas(consulta.cliente(idCliente));
    }

    @Transactional
    public EstadisticasProgresoDto registrarProgreso(Integer idCliente, RegistrarProgresoRequest req) {
        Cliente c = consulta.cliente(idCliente);
        Progreso p = new Progreso();
        p.setCliente(c);
        p.setFecha(consulta.hoy());
        p.setPeso(req.peso());
        p.setGrasaCorporal(req.grasaCorporal());
        p.setMedidas(req.medidas() == null || req.medidas().isEmpty() ? null : req.medidas());
        p.setObservaciones(Textos.vacioANull(req.observaciones()));
        progresoRepo.save(p);
        return consulta.estadisticas(c);
    }
}
