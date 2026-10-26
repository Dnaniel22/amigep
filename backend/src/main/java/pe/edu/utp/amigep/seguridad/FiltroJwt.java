package pe.edu.utp.amigep.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Lee "Authorization: Bearer <token>" y registra al usuario con su rol (ROLE_CLIENTE, ROLE_ENTRENADOR...). */
@Component
@RequiredArgsConstructor
public class FiltroJwt extends OncePerRequestFilter {

    private final JwtServicio jwtServicio;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            jwtServicio.validar(cabecera.substring(7)).ifPresent(usuario -> {
                var auth = new UsernamePasswordAuthenticationToken(usuario, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
