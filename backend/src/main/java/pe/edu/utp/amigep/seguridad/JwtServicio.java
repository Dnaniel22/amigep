package pe.edu.utp.amigep.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Emite y valida los tokens JWT que usan la app móvil y el panel web. */
@Service
public class JwtServicio {

    private final SecretKey clave;
    private final Duration expiracion;

    public JwtServicio(@Value("${amigep.jwt.secreto}") String secreto,
                       @Value("${amigep.jwt.expiracion-horas}") long horas) {
        if (secreto.length() < 32) {
            throw new IllegalStateException("amigep.jwt.secreto debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracion = Duration.ofHours(horas);
    }

    public String generar(UsuarioAutenticado usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.id()))
                .claim("rol", usuario.rol())
                .claim("nombre", usuario.nombre())
                .claim("email", usuario.email())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(clave)
                .compact();
    }

    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return Optional.of(new UsuarioAutenticado(Integer.valueOf(c.getSubject()),
                    c.get("rol", String.class), c.get("nombre", String.class), c.get("email", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpiracionSegundos() {
        return expiracion.toSeconds();
    }
}
