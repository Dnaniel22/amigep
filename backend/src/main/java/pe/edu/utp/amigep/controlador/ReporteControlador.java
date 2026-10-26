package pe.edu.utp.amigep.controlador;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.amigep.dto.ComunDtos.ReporteDto;
import pe.edu.utp.amigep.seguridad.UsuarioAutenticado;
import pe.edu.utp.amigep.servicio.ReporteServicio;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * RF-20: reportes bajo demanda para los tres roles. GET /api/reportes/RP-03?desde=2026-09-01&hasta=2026-09-30
 * Agregar formato=csv para descargar el archivo.
 */
@Tag(name = "Reportes")
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteControlador {

    private final ReporteServicio reportes;

    /** Reportes disponibles para el rol del usuario. */
    @GetMapping
    public List<Map<String, Object>> catalogo(@AuthenticationPrincipal UsuarioAutenticado u) {
        return reportes.catalogo(u);
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<?> generar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable String codigo,
                                     @RequestParam Map<String, String> filtros) {
        ReporteDto r = reportes.generar(codigo, filtros, u);
        if ("csv".equalsIgnoreCase(filtros.get("formato"))) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + r.codigo() + ".csv\"")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(ReporteServicio.aCsv(r));
        }
        return ResponseEntity.ok(r);
    }
}
