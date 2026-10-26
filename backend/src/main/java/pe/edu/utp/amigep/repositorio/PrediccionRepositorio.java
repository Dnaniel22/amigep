package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.utp.amigep.modelo.Prediccion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrediccionRepositorio extends JpaRepository<Prediccion, Integer> {

    /** Última predicción de cada cliente activo, de mayor a menor probabilidad. */
    @Query("""
            select p from Prediccion p join fetch p.cliente c
            where c.activo = true
              and p.idPrediccion = (select max(p2.idPrediccion) from Prediccion p2 where p2.cliente = p.cliente)
            order by p.probabilidad desc""")
    List<Prediccion> ultimasPorCliente();

    Optional<Prediccion> findFirstByClienteIdClienteOrderByIdPrediccionDesc(Integer idCliente);

    List<Prediccion> findByFechaPrediccion(LocalDate fecha);
}
