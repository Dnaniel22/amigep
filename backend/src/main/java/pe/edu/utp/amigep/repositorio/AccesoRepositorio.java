package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.amigep.modelo.Acceso;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccesoRepositorio extends JpaRepository<Acceso, Integer> {

    @Query("""
            select a from Acceso a join fetch a.cliente
            where a.fechaHora >= :desde and a.fechaHora < :hasta
            order by a.fechaHora desc""")
    List<Acceso> entre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            select a from Acceso a
            where a.cliente.idCliente = :idCliente and a.fechaHora >= :desde and a.fechaHora < :hasta
            order by a.fechaHora desc""")
    List<Acceso> deClienteEntre(@Param("idCliente") Integer idCliente,
                                @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    List<Acceso> findTop10ByClienteIdClienteOrderByFechaHoraDesc(Integer idCliente);

    Optional<Acceso> findFirstByClienteIdClienteOrderByFechaHoraDesc(Integer idCliente);

    @Query("select count(distinct a.cliente.idCliente) from Acceso a where a.fechaHora >= :desde and a.fechaHora < :hasta")
    long contarClientesDistintos(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** [idCliente, fechaHora] de todos los accesos, para el modelo de abandono. */
    @Query("select a.cliente.idCliente, a.fechaHora from Acceso a")
    List<Object[]> resumenParaModelo();
}
