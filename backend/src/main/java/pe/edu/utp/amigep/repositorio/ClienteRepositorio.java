package pe.edu.utp.amigep.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.amigep.modelo.Cliente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClienteRepositorio extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    Optional<Cliente> findByCodigoQR(String codigoQR);

    Optional<Cliente> findByDni(String dni);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDni(String dni);

    List<Cliente> findByActivoTrue();

    long countByActivoTrue();

    long countByActivoTrueAndFechaRegistroGreaterThanEqual(LocalDate desde);

    long countByEntrenadorIdTrabajadorAndActivoTrue(Integer idEntrenador);

    @Query("""
            select c from Cliente c left join fetch c.entrenador
            where c.entrenador.idTrabajador = :idEntrenador and c.activo = true
            order by c.nombre, c.apellido""")
    List<Cliente> asignadosA(@Param("idEntrenador") Integer idEntrenador);

    /** Búsqueda por nombre, código (CL-0001 o 1), DNI o correo. q = "" devuelve todos. */
    @Query("""
            select c from Cliente c left join fetch c.entrenador
            where (:q = ''
                   or lower(concat(c.nombre, ' ', c.apellido)) like lower(concat('%', :q, '%'))
                   or c.dni like concat('%', :q, '%')
                   or lower(c.email) like lower(concat('%', :q, '%'))
                   or str(c.idCliente) = :idTexto)
            order by c.idCliente desc""")
    List<Cliente> buscar(@Param("q") String q, @Param("idTexto") String idTexto);
}
