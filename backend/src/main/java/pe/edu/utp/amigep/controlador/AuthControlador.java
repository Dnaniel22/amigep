package pe.edu.utp.amigep.controlador;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.amigep.dto.AuthDtos.*;
import pe.edu.utp.amigep.dto.ComunDtos.MensajeDto;
import pe.edu.utp.amigep.dto.ComunDtos.PlanDto;
import pe.edu.utp.amigep.seguridad.UsuarioAutenticado;
import pe.edu.utp.amigep.servicio.AuthServicio;
import pe.edu.utp.amigep.servicio.MembresiaServicio;

import java.util.List;
import java.util.Map;

@Tag(name = "Autenticación", description = "RF-01 · común a la app y al panel web")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthControlador {

    private final AuthServicio authServicio;
    private final MembresiaServicio membresiaServicio;

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authServicio.login(req);
    }

    @PostMapping("/auth/recuperar")
    public Map<String, Object> recuperar(@Valid @RequestBody RecuperarRequest req) {
        return authServicio.recuperar(req);
    }

    @PostMapping("/auth/restablecer")
    public MensajeDto restablecer(@Valid @RequestBody RestablecerRequest req) {
        authServicio.restablecer(req);
        return new MensajeDto("Contraseña actualizada. Ya puede iniciar sesión.");
    }

    @GetMapping("/auth/yo")
    public UsuarioActualDto yo(@AuthenticationPrincipal UsuarioAutenticado u) {
        return new UsuarioActualDto(u.id(), u.rol(), u.nombre(), u.email());
    }

    @PutMapping("/auth/contrasena")
    public MensajeDto cambiarContrasena(@AuthenticationPrincipal UsuarioAutenticado u,
                                        @Valid @RequestBody CambiarContrasenaRequest req) {
        authServicio.cambiarContrasena(u, req);
        return new MensajeDto("Contraseña actualizada");
    }

    /** Catálogo de planes (público: se muestra también antes de iniciar sesión). */
    @GetMapping("/planes")
    public List<PlanDto> planes() {
        return membresiaServicio.planes();
    }
}
