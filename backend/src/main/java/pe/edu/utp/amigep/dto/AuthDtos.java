package pe.edu.utp.amigep.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** RF-01: inicio de sesión con selector de rol y recuperación de contraseña. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Ingrese su correo") @Email(message = "Correo no válido") String correo,
            @NotBlank(message = "Ingrese su contraseña") String contrasena,
            @NotBlank(message = "Seleccione un rol")
            @Pattern(regexp = "CLIENTE|ENTRENADOR|ADMINISTRADOR", message = "Rol no válido") String rol) {
    }

    public record LoginResponse(String token, long expiraEnSegundos, String rol, Integer id,
                                String nombre, String apellido, String correo, String iniciales) {
    }

    public record RecuperarRequest(
            @NotBlank(message = "Ingrese su correo") @Email(message = "Correo no válido") String correo,
            @NotBlank @Pattern(regexp = "CLIENTE|ENTRENADOR|ADMINISTRADOR", message = "Rol no válido") String rol) {
    }

    public record RestablecerRequest(
            @NotBlank @Email String correo,
            @NotBlank @Pattern(regexp = "CLIENTE|ENTRENADOR|ADMINISTRADOR") String rol,
            @NotBlank(message = "Ingrese el código recibido") String codigo,
            @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String nuevaContrasena) {
    }

    public record CambiarContrasenaRequest(
            @NotBlank(message = "Ingrese su contraseña actual") String actual,
            @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String nueva) {
    }

    public record UsuarioActualDto(Integer id, String rol, String nombre, String correo) {
    }
}
