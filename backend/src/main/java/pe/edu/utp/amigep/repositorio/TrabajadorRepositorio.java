package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.amigep.modelo.Trabajador;
import pe.edu.utp.amigep.modelo.enums.Rol;

import java.util.List;
import java.util.Optional;

public interface TrabajadorRepositorio extends JpaRepository<Trabajador, Integer> {

    Optional<Trabajador> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDni(String dni);

    List<Trabajador> findByRolOrderByApellidoAscNombreAsc(Rol rol);

    List<Trabajador> findAllByOrderByRolAscApellidoAsc();

    long countByRolAndActivoTrue(Rol rol);
}
