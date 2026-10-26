package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.amigep.modelo.Plan;

import java.util.List;

public interface PlanRepositorio extends JpaRepository<Plan, Integer> {

    List<Plan> findAllByOrderByPrecioAsc();
}
