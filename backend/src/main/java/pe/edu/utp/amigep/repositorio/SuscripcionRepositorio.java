package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.amigep.modelo.Suscripcion;

import java.time.LocalDate;
import java.util.List;

public interface SuscripcionRepositorio extends JpaRepository<Suscripcion, Integer> {

    @Query("""
            select s from Suscripcion s join fetch s.plan
            where s.cliente.idCliente = :idCliente
            order by s.fechaInicio desc, s.idSuscripcion desc""")
    List<Suscripcion> deCliente(@Param("idCliente") Integer idCliente);

    /** Suscripciones vigentes de un cliente en una fecha (la de mayor caducidad primero). */
    @Query("""
            select s from Suscripcion s join fetch s.plan
            where s.cliente.idCliente = :idCliente and s.activo = true
              and s.fechaInicio <= :fecha and s.fechaCaducidad >= :fecha
            order by s.fechaCaducidad desc""")
    List<Suscripcion> vigentesDeCliente(@Param("idCliente") Integer idCliente, @Param("fecha") LocalDate fecha);

    @Query("""
            select s from Suscripcion s join fetch s.plan join fetch s.cliente c left join fetch c.entrenador
            order by s.fechaCaducidad desc""")
    List<Suscripcion> todasConDetalle();

    /**
     * Membresías que vencen entre hoy y el límite y que el cliente aún no ha renovado
     * (no existe otra suscripción activa que termine después).
     */
    @Query("""
            select s from Suscripcion s join fetch s.plan join fetch s.cliente c
            where s.activo = true and c.activo = true
              and s.fechaInicio <= :hoy and s.fechaCaducidad between :hoy and :limite
              and not exists (select s2 from Suscripcion s2
                              where s2.cliente = s.cliente and s2.activo = true
                                and s2.fechaCaducidad > s.fechaCaducidad)
            order by s.fechaCaducidad asc""")
    List<Suscripcion> porVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);

    @Query("""
            select count(distinct s.cliente.idCliente) from Suscripcion s
            where s.activo = true and s.cliente.activo = true
              and s.fechaInicio <= :fecha and s.fechaCaducidad >= :fecha""")
    long contarClientesConMembresiaVigente(@Param("fecha") LocalDate fecha);

    /** [idCliente, fechaInicio, fechaCaducidad, activo] para el modelo de abandono. */
    @Query("select s.cliente.idCliente, s.fechaInicio, s.fechaCaducidad, s.activo from Suscripcion s")
    List<Object[]> resumenParaModelo();
}
