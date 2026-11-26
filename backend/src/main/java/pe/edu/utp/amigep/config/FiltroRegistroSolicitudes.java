package pe.edu.utp.amigep.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Plan de monitoreo: registra cada solicitud a la API en el logger "amigep.solicitudes" con el método,
 * la ruta, el estado HTTP, la duración y la IP de origen. Se ejecuta antes de Spring Security para registrar
 * también los rechazos 401 y 403. No registra parámetros ni cuerpos, para no guardar datos personales.
 * <ul>
 *   <li>INFO: solicitudes normales.</li>
 *   <li>WARN: accesos rechazados (401, 403) y solicitudes lentas (2 s o más, RNF-06).</li>
 *   <li>ERROR: errores del servidor (5xx).</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroRegistroSolicitudes extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("amigep.solicitudes");
    private static final long LENTA_MS = 2000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.startsWith("/api/") || uri.startsWith("/actuator"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        boolean fallo = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            fallo = true;
            throw e;
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            int estado = fallo ? 500 : response.getStatus();
            String linea = String.format("%s %s -> %d (%d ms) ip=%s",
                    request.getMethod(), request.getRequestURI(), estado, ms, request.getRemoteAddr());
            if (estado >= 500) {
                log.error(linea);
            } else if (estado == 401 || estado == 403 || ms >= LENTA_MS) {
                log.warn(linea);
            } else {
                log.info(linea);
            }
        }
    }
}
