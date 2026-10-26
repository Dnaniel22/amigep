package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.amigep.modelo.Rutina;

import java.util.List;
import java.util.Optional;

public interface RutinaRepositorio extends JpaRepository<Rutina, Integer> {

    Optional<Rutina> findFirstByClienteIdClienteAndActivoTrueOrderByFechaCreacionDescIdRutinaDesc(Integer idCliente);

    List<Rutina> findByClienteIdClienteOrderByFechaCreacionDescIdRutinaDesc(Integer idCliente);

    List<Rutina> findByClienteIdClienteAndActivoTrue(Integer idCliente);

    @Query("""
            select r from Rutina r join fetch r.cliente c left join fetch r.trabajador
            order by r.fechaCreacion desc, r.idRutina desc""")
    List<Rutina> todasConDetalle();

    @Query("""
            select r from Rutina r join fetch r.cliente c left join fetch r.trabajador
            where c.entrenador.idTrabajador = :idEntrenador
            order by r.activo desc, r.fechaCreacion desc""")
    List<Rutina> deClientesDeEntrenador(@Param("idEntrenador") Integer idEntrenador);

    /** [idCliente, fechaCreacion, fechaFinalizacion, activo] para el modelo de abandono. */
    @Query("select r.cliente.idCliente, r.fechaCreacion, r.fechaFinalizacion, r.activo from Rutina r")
    List<Object[]> resumenParaModelo();
}
