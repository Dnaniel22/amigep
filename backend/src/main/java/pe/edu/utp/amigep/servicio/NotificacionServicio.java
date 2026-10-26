package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.ComunDtos.NotificacionDto;
import pe.edu.utp.amigep.excepcion.AccesoDenegadoException;
import pe.edu.utp.amigep.excepcion.NoEncontradoException;
import pe.edu.utp.amigep.modelo.AccionRetencion;
import pe.edu.utp.amigep.modelo.Cliente;
import pe.edu.utp.amigep.modelo.Notificacion;
import pe.edu.utp.amigep.modelo.enums.TipoNotificacion;
import pe.edu.utp.amigep.repositorio.NotificacionRepositorio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** RF-19: notificaciones a los clientes (vencimientos, rutinas asignadas y retención) con estado de lectura. */
@Service
@RequiredArgsConstructor
public class NotificacionServicio {

    private final NotificacionRepositorio repo;
    private final Mapeador mapeador;
    private final Clock reloj;

    @Transactional
    public Notificacion enviar(Cliente cliente, TipoNotificacion tipo, String titulo, String mensaje, AccionRetencion accion) {
        Notificacion n = new Notificacion();
        n.setCliente(cliente);
        n.setTipo(tipo);
        n.setTitulo(titulo.length() > 100 ? titulo.substring(0, 100) : titulo);
        n.setMensaje(mensaje);
        n.setAccion(accion);
        n.setFechaEnvio(LocalDateTime.now(reloj));
        n.setLeido(false);
        return repo.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificacionDto> deCliente(Integer idCliente) {
        return repo.findTop50ByClienteIdClienteOrderByFechaEnvioDesc(idCliente).stream().map(mapeador::notificacion).toList();
    }

    @Transactional(readOnly = true)
    public long noLeidas(Integer idCliente) {
        return repo.countByClienteIdClienteAndLeidoFalse(idCliente);
    }

    @Transactional
    public NotificacionDto marcarLeida(Integer idNotificacion, Integer idCliente) {
        Notificacion n = repo.findById(idNotificacion)
                .orElseThrow(() -> new NoEncontradoException("Notificación no encontrada"));
        if (!n.getCliente().getIdCliente().equals(idCliente)) {
            throw new AccesoDenegadoException("La notificación no le pertenece");
        }
        n.setLeido(true);
        return mapeador.notificacion(n);
    }

    @Transactional(readOnly = true)
    public List<NotificacionDto> recientes() {
        return repo.recientes().stream().map(mapeador::notificacion).toList();
    }

    @Transactional(readOnly = true)
    public boolean enviadaRecientemente(Integer idCliente, TipoNotificacion tipo, int dias) {
        return repo.existsByClienteIdClienteAndTipoAndFechaEnvioAfter(idCliente, tipo, LocalDateTime.now(reloj).minusDays(dias));
    }
}
