package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.amigep.modelo.PlanAlimenticio;

import java.util.List;
import java.util.Optional;

public interface PlanAlimenticioRepositorio extends JpaRepository<PlanAlimenticio, Integer> {

    Optional<PlanAlimenticio> findFirstByClienteIdClienteAndActivoTrueOrderByFechaCreacionDescIdPlanAlimenticioDesc(Integer idCliente);

    List<PlanAlimenticio> findByClienteIdClienteAndActivoTrue(Integer idCliente);
}
