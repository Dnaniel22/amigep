package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.amigep.modelo.SesionEntrenamiento;

import java.time.LocalDate;
import java.util.List;

public interface SesionEntrenamientoRepositorio extends JpaRepository<SesionEntrenamiento, Integer> {

    List<SesionEntrenamiento> findByRutinaIdRutinaOrderByFechaDescIdSesionDesc(Integer idRutina);

    long countByRutinaIdRutinaAndCompletadaTrue(Integer idRutina);

    List<SesionEntrenamiento> findByRutinaIdRutinaAndFecha(Integer idRutina, LocalDate fecha);

    List<SesionEntrenamiento> findByRutinaClienteIdClienteAndFechaBetween(Integer idCliente, LocalDate desde, LocalDate hasta);
}
