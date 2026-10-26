package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.utp.amigep.modelo.Notificacion;
import pe.edu.utp.amigep.modelo.enums.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepositorio extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findTop50ByClienteIdClienteOrderByFechaEnvioDesc(Integer idCliente);

    long countByClienteIdClienteAndLeidoFalse(Integer idCliente);

    boolean existsByClienteIdClienteAndTipoAndFechaEnvioAfter(Integer idCliente, TipoNotificacion tipo, LocalDateTime despues);

    @Query("select n from Notificacion n join fetch n.cliente order by n.fechaEnvio desc limit 100")
    List<Notificacion> recientes();
}
