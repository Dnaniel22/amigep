package pe.edu.utp.amigep.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class ConfiguracionGeneral {

    /** Reloj con la zona horaria del gimnasio; los servicios lo usan para "hoy". */
    @Bean
    public Clock reloj(@Value("${amigep.zona-horaria}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Bean
    public OpenAPI documentacionApi() {
        return new OpenAPI()
                .info(new Info().title("AMIGEP API REST").version("1.0")
                        .description("API compartida por la app móvil (cliente y entrenador) y el panel web del administrador."))
                .components(new Components().addSecuritySchemes("jwt",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("jwt"));
    }
}
