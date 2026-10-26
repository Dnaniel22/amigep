package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.amigep.modelo.Pago;
import pe.edu.utp.amigep.modelo.enums.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagoRepositorio extends JpaRepository<Pago, Integer> {

    @Query("""
            select p from Pago p join fetch p.suscripcion s join fetch s.plan join fetch s.cliente
            where p.fechaPago between :desde and :hasta
            order by p.fechaPago desc, p.idPago desc""")
    List<Pago> entreFechas(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            select p from Pago p join fetch p.suscripcion s join fetch s.plan
            where s.cliente.idCliente = :idCliente
            order by p.fechaPago desc, p.idPago desc""")
    List<Pago> deCliente(@Param("idCliente") Integer idCliente);

    @Query("""
            select coalesce(sum(p.monto), 0) from Pago p
            where p.estado = :estado and p.fechaPago between :desde and :hasta""")
    BigDecimal sumar(@Param("estado") EstadoPago estado, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    List<Pago> findBySuscripcionIdSuscripcion(Integer idSuscripcion);

    /** [idCliente, fechaPago, estado] para el modelo de abandono. */
    @Query("select p.suscripcion.cliente.idCliente, p.fechaPago, p.estado from Pago p")
    List<Object[]> resumenParaModelo();
}
