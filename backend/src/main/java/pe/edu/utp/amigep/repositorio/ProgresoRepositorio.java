package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.amigep.modelo.Progreso;

import java.util.List;
import java.util.Optional;

public interface ProgresoRepositorio extends JpaRepository<Progreso, Integer> {

    List<Progreso> findByClienteIdClienteOrderByFechaAscIdProgresoAsc(Integer idCliente);

    Optional<Progreso> findFirstByClienteIdClienteAndPesoNotNullOrderByFechaDescIdProgresoDesc(Integer idCliente);
}
