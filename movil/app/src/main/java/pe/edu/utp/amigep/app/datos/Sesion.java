package pe.edu.utp.amigep.app.datos;

import android.content.Context;
import android.content.SharedPreferences;

import pe.edu.utp.amigep.app.BuildConfig;

/** Datos de la sesión iniciada (token JWT y rol) guardados en SharedPreferences. */
public final class Sesion {

    public static final String CLIENTE = "CLIENTE";
    public static final String ENTRENADOR = "ENTRENADOR";

    private static final String ARCHIVO = "amigep_sesion";
    private final SharedPreferences prefs;

    private Sesion(Context contexto) {
        prefs = contexto.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public static Sesion de(Context contexto) {
        return new Sesion(contexto);
    }

    public void guardar(Modelos.LoginResponse r) {
        prefs.edit()
                .putString("token", r.token)
                .putString("rol", r.rol)
                .putInt("id", r.id)
                .putString("nombre", r.nombre)
                .putString("apellido", r.apellido)
                .putString("correo", r.correo)
                .putString("iniciales", r.iniciales)
                .putLong("expira", System.currentTimeMillis() + r.expiraEnSegundos * 1000)
                .apply();
    }

    public boolean activa() {
        return token() != null && System.currentTimeMillis() < prefs.getLong("expira", 0);
    }

    public void cerrar() {
        String servidor = servidor();
        String ultimoCorreo = prefs.getString("ultimoCorreo", null);
        prefs.edit().clear().putString("servidor", servidor).putString("ultimoCorreo", ultimoCorreo).apply();
    }

    public String token() {
        return prefs.getString("token", null);
    }

    public String rol() {
        return prefs.getString("rol", null);
    }

    public String nombre() {
        return prefs.getString("nombre", "");
    }

    public String apellido() {
        return prefs.getString("apellido", "");
    }

    public String correo() {
        return prefs.getString("correo", "");
    }

    public String iniciales() {
        return prefs.getString("iniciales", "");
    }

    /** URL del backend; por defecto la de BuildConfig, configurable desde el login para pruebas en celular. */
    public String servidor() {
        return prefs.getString("servidor", BuildConfig.API_URL);
    }

    public void guardarServidor(String url) {
        String u = url.trim();
        if (!u.endsWith("/")) u = u + "/";
        prefs.edit().putString("servidor", u).apply();
        ApiCliente.reiniciar();
    }

    public String ultimoCorreo() {
        return prefs.getString("ultimoCorreo", "");
    }

    public void guardarUltimoCorreo(String correo) {
        prefs.edit().putString("ultimoCorreo", correo).apply();
    }
}
