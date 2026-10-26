package pe.edu.utp.amigep.excepcion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Convierte las excepciones en respuestas JSON uniformes: {mensaje, campos, fecha}. */
@Slf4j
@RestControllerAdvice
public class ManejadorErrores {

    public record RespuestaError(String mensaje, Map<String, String> campos, LocalDateTime fecha) {
        static RespuestaError de(String mensaje, Map<String, String> campos) {
            return new RespuestaError(mensaje, campos == null || campos.isEmpty() ? null : campos, LocalDateTime.now());
        }
    }

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(NoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(RespuestaError.de(e.getMessage(), null));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<RespuestaError> reglaNegocio(ReglaNegocioException e) {
        return ResponseEntity.badRequest().body(RespuestaError.de(e.getMessage(), e.getCampos()));
    }

    @ExceptionHandler({AccesoDenegadoException.class, AccessDeniedException.class})
    public ResponseEntity<RespuestaError> denegado(RuntimeException e) {
        String mensaje = e instanceof AccesoDenegadoException ? e.getMessage() : "No tiene permisos para esta operación";
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(RespuestaError.de(mensaje, null));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RespuestaError> credenciales(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RespuestaError.de(e.getMessage(), null));
    }

    /** Validaciones de formularios (RNF-05). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(RespuestaError.de("Revise los datos ingresados", campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<RespuestaError> formato(Exception e) {
        return ResponseEntity.badRequest().body(RespuestaError.de("Formato de datos inválido", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(RespuestaError.de("La operación entra en conflicto con datos existentes", null));
    }
}
