package pe.edu.utp.amigep.excepcion;

/** El usuario está autenticado pero no puede ver ese dato (HTTP 403), p. ej. un cliente no asignado. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
