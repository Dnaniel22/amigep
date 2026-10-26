package pe.edu.utp.amigep.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.edu.utp.amigep.seguridad.FiltroJwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Control de acceso por roles (RNF-02, RC-03): cada rol solo llega a su grupo de endpoints.
 * /api/cliente/** → app (cliente) · /api/entrenador/** → app (entrenador) · /api/admin/** → panel web.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class ConfiguracionSeguridad {

    private final FiltroJwt filtroJwt;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(cors()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/recuperar", "/api/auth/restablecer").permitAll()
                        .requestMatchers("/api/planes").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/entrenador/**").hasRole("ENTRENADOR")
                        .requestMatchers("/api/cliente/**").hasRole("CLIENTE")
                        .requestMatchers("/api/**").authenticated()
                        // Panel web (archivos estáticos): la sesión se valida en el navegador con el token
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escribirError(res, 401, "Debe iniciar sesión"))
                        .accessDeniedHandler((req, res, ex) -> escribirError(res, 403, "No tiene permisos para esta operación")))
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void escribirError(jakarta.servlet.http.HttpServletResponse res, int estado, String mensaje)
            throws java.io.IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), Map.of("mensaje", mensaje, "fecha", LocalDateTime.now().toString()));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Contraseñas almacenadas como hash (sección 14.2)
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource cors() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
