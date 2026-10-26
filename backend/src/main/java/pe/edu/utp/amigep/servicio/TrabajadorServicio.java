package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.AdminDtos.GuardarTrabajadorRequest;
import pe.edu.utp.amigep.dto.AdminDtos.TrabajadorDto;
import pe.edu.utp.amigep.excepcion.NoEncontradoException;
import pe.edu.utp.amigep.excepcion.ReglaNegocioException;
import pe.edu.utp.amigep.modelo.Trabajador;
import pe.edu.utp.amigep.modelo.enums.Rol;
import pe.edu.utp.amigep.repositorio.ClienteRepositorio;
import pe.edu.utp.amigep.repositorio.TrabajadorRepositorio;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/** RF-02 (usuarios y roles) y RF-07 (entrenadores) - BPM Fig. 3 y Fig. 7. */
@Service
@RequiredArgsConstructor
public class TrabajadorServicio {

    private final TrabajadorRepositorio repo;
    private final ClienteRepositorio clienteRepo;
    private final PasswordEncoder encoder;
    private final Mapeador mapeador;

    @Transactional(readOnly = true)
    public List<TrabajadorDto> listar(Rol rol) {
        List<Trabajador> lista = rol == null ? repo.findAllByOrderByRolAscApellidoAsc() : repo.findByRolOrderByApellidoAscNombreAsc(rol);
        return lista.stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public TrabajadorDto obtener(Integer id) {
        return dto(buscar(id));
    }

    @Transactional
    public TrabajadorDto crear(GuardarTrabajadorRequest req) {
        if (req.contrasena() == null || req.contrasena().isBlank()) {
            throw new ReglaNegocioException("Revise los datos ingresados", Map.of("contrasena", "Ingrese una contraseña"));
        }
        validarUnicos(req, null);
        Trabajador t = new Trabajador();
        aplicar(t, req);
        t.setContrasenaHash(encoder.encode(req.contrasena()));
        t.setActivo(true);
        return dto(repo.save(t));
    }

    @Transactional
    public TrabajadorDto actualizar(Integer id, GuardarTrabajadorRequest req, Integer idUsuarioActual) {
        Trabajador t = buscar(id);
        validarUnicos(req, t);
        if (t.getIdTrabajador().equals(idUsuarioActual) && req.rol() != Rol.ADMINISTRADOR) {
            throw new ReglaNegocioException("No puede quitarse a sí mismo el rol de administrador");
        }
        aplicar(t, req);
        if (req.contrasena() != null && !req.contrasena().isBlank()) t.setContrasenaHash(encoder.encode(req.contrasena()));
        return dto(t);
    }

    @Transactional
    public TrabajadorDto cambiarEstado(Integer id, boolean activo, Integer idUsuarioActual) {
        Trabajador t = buscar(id);
        if (!activo && t.getIdTrabajador().equals(idUsuarioActual)) {
            throw new ReglaNegocioException("No puede desactivar su propio usuario");
        }
        if (!activo && t.getRol() == Rol.ADMINISTRADOR && repo.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1) {
            throw new ReglaNegocioException("Debe existir al menos un administrador activo");
        }
        t.setActivo(activo);
        return dto(t);
    }

    /** Genera una contraseña temporal que el administrador entrega al trabajador. */
    @Transactional
    public Map<String, String> restablecerContrasena(Integer id) {
        Trabajador t = buscar(id);
        String temporal = contrasenaTemporal();
        t.setContrasenaHash(encoder.encode(temporal));
        return Map.of("contrasenaTemporal", temporal, "mensaje", "Entregue esta contraseña temporal a " + t.getNombreCompleto());
    }

    static String contrasenaTemporal() {
        String letras = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder("Amg-");
        for (int i = 0; i < 8; i++) sb.append(letras.charAt(r.nextInt(letras.length())));
        return sb.toString();
    }

    private void aplicar(Trabajador t, GuardarTrabajadorRequest req) {
        t.setNombre(req.nombre().trim());
        t.setApellido(req.apellido().trim());
        t.setDni(req.dni().trim());
        t.setEmail(req.email().trim().toLowerCase());
        t.setRol(req.rol());
        t.setEspecialidad(Textos.vacioANull(req.especialidad()));
    }

    private void validarUnicos(GuardarTrabajadorRequest req, Trabajador actual) {
        repo.findByEmailIgnoreCase(req.email().trim()).filter(o -> actual == null || !o.getIdTrabajador().equals(actual.getIdTrabajador()))
                .ifPresent(o -> {
                    throw new ReglaNegocioException("Revise los datos ingresados", Map.of("email", "El correo ya está registrado"));
                });
        if ((actual == null || !actual.getDni().equals(req.dni().trim())) && repo.existsByDni(req.dni().trim())) {
            throw new ReglaNegocioException("Revise los datos ingresados", Map.of("dni", "El DNI ya está registrado"));
        }
    }

    private Trabajador buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NoEncontradoException("Trabajador no encontrado"));
    }

    private TrabajadorDto dto(Trabajador t) {
        long asignados = t.getRol() == Rol.ENTRENADOR ? clienteRepo.countByEntrenadorIdTrabajadorAndActivoTrue(t.getIdTrabajador()) : 0;
        return mapeador.trabajador(t, asignados);
    }
}
