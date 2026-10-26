package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.AuthDtos.*;
import pe.edu.utp.amigep.excepcion.ReglaNegocioException;
import pe.edu.utp.amigep.modelo.Cliente;
import pe.edu.utp.amigep.modelo.Trabajador;
import pe.edu.utp.amigep.repositorio.ClienteRepositorio;
import pe.edu.utp.amigep.repositorio.TrabajadorRepositorio;
import pe.edu.utp.amigep.seguridad.JwtServicio;
import pe.edu.utp.amigep.seguridad.UsuarioAutenticado;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** RF-01: autenticación por rol y recuperación de contraseña (BPM Fig. 2). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServicio {

    private final ClienteRepositorio clienteRepo;
    private final TrabajadorRepositorio trabajadorRepo;
    private final PasswordEncoder encoder;
    private final JwtServicio jwt;

    /** Muestra el código de recuperación en la respuesta (solo para pruebas, sin servidor de correo). */
    @Value("${amigep.recuperacion.mostrar-codigo:true}")
    private boolean mostrarCodigo;

    private record CodigoRecuperacion(String codigo, Instant expira) {
    }

    private final Map<String, CodigoRecuperacion> codigos = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        String correo = req.correo().trim();
        BadCredentialsException error = new BadCredentialsException("Correo o contraseña incorrectos");
        if (UsuarioAutenticado.CLIENTE.equals(req.rol())) {
            Cliente c = clienteRepo.findByEmailIgnoreCase(correo).orElseThrow(() -> error);
            if (!encoder.matches(req.contrasena(), c.getContrasenaHash())) throw error;
            if (!c.isActivo()) throw new BadCredentialsException("Su cuenta está desactivada. Acérquese a recepción.");
            return respuesta(new UsuarioAutenticado(c.getIdCliente(), UsuarioAutenticado.CLIENTE, c.getNombreCompleto(), c.getEmail()),
                    c.getNombre(), c.getApellido());
        }
        Trabajador t = trabajadorRepo.findByEmailIgnoreCase(correo).orElseThrow(() -> error);
        if (!encoder.matches(req.contrasena(), t.getContrasenaHash())) throw error;
        if (!t.getRol().name().equals(req.rol())) {
            throw new BadCredentialsException(UsuarioAutenticado.ADMINISTRADOR.equals(t.getRol().name())
                    ? "El administrador ingresa desde el panel web"
                    : "Su usuario no tiene el rol seleccionado");
        }
        if (!t.isActivo()) throw new BadCredentialsException("Su usuario está desactivado");
        return respuesta(new UsuarioAutenticado(t.getIdTrabajador(), t.getRol().name(), t.getNombreCompleto(), t.getEmail()),
                t.getNombre(), t.getApellido());
    }

    private LoginResponse respuesta(UsuarioAutenticado u, String nombre, String apellido) {
        return new LoginResponse(jwt.generar(u), jwt.getExpiracionSegundos(), u.rol(), u.id(), nombre, apellido,
                u.email(), Textos.iniciales(nombre, apellido));
    }

    /** Genera un código de 6 dígitos válido 15 minutos. La respuesta no revela si el correo existe. */
    public Map<String, Object> recuperar(RecuperarRequest req) {
        String clave = req.rol() + ":" + req.correo().trim().toLowerCase();
        boolean existe = UsuarioAutenticado.CLIENTE.equals(req.rol())
                ? clienteRepo.findByEmailIgnoreCase(req.correo().trim()).isPresent()
                : trabajadorRepo.findByEmailIgnoreCase(req.correo().trim()).filter(t -> t.getRol().name().equals(req.rol())).isPresent();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "Si el correo está registrado, recibirá un código para restablecer su contraseña.");
        if (existe) {
            String codigo = String.format("%06d", random.nextInt(1_000_000));
            codigos.put(clave, new CodigoRecuperacion(codigo, Instant.now().plus(15, ChronoUnit.MINUTES)));
            // Integración con correo: reemplazar este log por el envío con JavaMailSender.
            log.info("Código de recuperación para {} ({}): {}", req.correo(), req.rol(), codigo);
            if (mostrarCodigo) respuesta.put("codigoDesarrollo", codigo);
        }
        return respuesta;
    }

    @Transactional
    public void restablecer(RestablecerRequest req) {
        String clave = req.rol() + ":" + req.correo().trim().toLowerCase();
        CodigoRecuperacion guardado = codigos.get(clave);
        if (guardado == null || guardado.expira().isBefore(Instant.now()) || !guardado.codigo().equals(req.codigo().trim())) {
            throw new ReglaNegocioException("El código no es válido o ya expiró");
        }
        String hash = encoder.encode(req.nuevaContrasena());
        if (UsuarioAutenticado.CLIENTE.equals(req.rol())) {
            clienteRepo.findByEmailIgnoreCase(req.correo().trim()).ifPresent(c -> c.setContrasenaHash(hash));
        } else {
            trabajadorRepo.findByEmailIgnoreCase(req.correo().trim()).ifPresent(t -> t.setContrasenaHash(hash));
        }
        codigos.remove(clave);
    }

    @Transactional
    public void cambiarContrasena(UsuarioAutenticado u, CambiarContrasenaRequest req) {
        if (u.esCliente()) {
            Cliente c = clienteRepo.findById(u.id()).orElseThrow();
            validarActual(req.actual(), c.getContrasenaHash());
            c.setContrasenaHash(encoder.encode(req.nueva()));
        } else {
            Trabajador t = trabajadorRepo.findById(u.id()).orElseThrow();
            validarActual(req.actual(), t.getContrasenaHash());
            t.setContrasenaHash(encoder.encode(req.nueva()));
        }
    }

    private void validarActual(String actual, String hash) {
        if (!encoder.matches(actual, hash)) {
            throw new ReglaNegocioException("La contraseña actual no es correcta", Map.of("actual", "Contraseña incorrecta"));
        }
    }
}
