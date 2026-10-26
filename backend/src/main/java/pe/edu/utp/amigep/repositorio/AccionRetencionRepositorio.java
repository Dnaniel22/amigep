package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.amigep.modelo.AccionRetencion;

import java.time.LocalDate;
import java.util.List;

public interface AccionRetencionRepositorio extends JpaRepository<AccionRetencion, Integer> {

    @Query("""
            select a from AccionRetencion a join fetch a.prediccion p join fetch p.cliente join fetch a.trabajador
            where a.fechaAccion >= :desde
            order by a.fechaAccion desc, a.idAccion desc""")
    List<AccionRetencion> desde(@Param("desde") LocalDate desde);

    long countByFechaAccionGreaterThanEqual(LocalDate desde);
}
