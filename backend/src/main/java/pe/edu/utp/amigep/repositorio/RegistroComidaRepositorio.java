package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.amigep.modelo.RegistroComida;

import java.time.LocalDate;
import java.util.List;

public interface RegistroComidaRepositorio extends JpaRepository<RegistroComida, Integer> {

    List<RegistroComida> findByClienteIdClienteAndFechaOrderByIdRegistroAsc(Integer idCliente, LocalDate fecha);

    List<RegistroComida> findByClienteIdClienteAndFechaBetweenOrderByFechaDescIdRegistroDesc(Integer idCliente, LocalDate desde, LocalDate hasta);
}
