package pe.edu.utp.amigep.seguridad;

/**
 * Usuario de la petición actual, reconstruido desde el token JWT.
 *
 * @param id  idCliente o idTrabajador según el rol
 * @param rol CLIENTE, ENTRENADOR o ADMINISTRADOR
 */
public record UsuarioAutenticado(Integer id, String rol, String nombre, String email) {

    public static final String CLIENTE = "CLIENTE";
    public static final String ENTRENADOR = "ENTRENADOR";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    public boolean esCliente() {
        return CLIENTE.equals(rol);
    }

    public boolean esEntrenador() {
        return ENTRENADOR.equals(rol);
    }

    public boolean esAdministrador() {
        return ADMINISTRADOR.equals(rol);
    }
}
