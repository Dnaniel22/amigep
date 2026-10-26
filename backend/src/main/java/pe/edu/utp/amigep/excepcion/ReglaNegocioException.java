package pe.edu.utp.amigep.excepcion;

import java.util.Map;

/** Operación inválida según las reglas del negocio (HTTP 400). Puede detallar campos. */
public class ReglaNegocioException extends RuntimeException {

    private final Map<String, String> campos;

    public ReglaNegocioException(String mensaje) {
        this(mensaje, Map.of());
    }

    public ReglaNegocioException(String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.campos = campos;
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
