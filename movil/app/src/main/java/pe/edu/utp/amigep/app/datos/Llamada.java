package pe.edu.utp.amigep.app.datos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.util.Collections;
import java.util.Map;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.ui.LoginActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Ejecuta llamadas a la API en segundo plano y entrega el resultado en el hilo principal.
 * Interpreta el cuerpo de error del backend ({mensaje, campos}) y cierra la sesión si el token expiró (401).
 */
public final class Llamada {

    public interface Exito<T> {
        void ok(T datos);
    }

    public interface Fallo {
        void error(String mensaje, Map<String, String> campos);
    }

    /** Permite ignorar respuestas que llegan cuando la pantalla ya se cerró. */
    public interface Vigente {
        boolean sigueVisible();
    }

    private Llamada() {
    }

    public static <T> void ejecutar(Context ctx, Call<T> call, Exito<T> exito) {
        ejecutar(ctx, call, exito, null, null);
    }

    public static <T> void ejecutar(Context ctx, Call<T> call, Exito<T> exito, Fallo fallo) {
        ejecutar(ctx, call, exito, fallo, null);
    }

    public static <T> void ejecutar(Context ctx, Call<T> call, Exito<T> exito, Fallo fallo, Vigente vigente) {
        Context app = ctx.getApplicationContext();
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> c, Response<T> resp) {
                if (vigente != null && !vigente.sigueVisible()) return;
                if (resp.isSuccessful()) {
                    exito.ok(resp.body());
                    return;
                }
                Modelos.ErrorApi error = leerError(resp);
                if (resp.code() == 401 && Sesion.de(app).token() != null) {
                    Sesion.de(app).cerrar();
                    Toast.makeText(app, "Su sesión expiró. Inicie sesión nuevamente.", Toast.LENGTH_LONG).show();
                    Intent i = new Intent(app, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    app.startActivity(i);
                    return;
                }
                String mensaje = error.mensaje != null ? error.mensaje : "Error " + resp.code();
                Map<String, String> campos = error.campos != null ? error.campos : Collections.emptyMap();
                if (fallo != null) fallo.error(mensaje, campos);
                else Toast.makeText(app, mensaje, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<T> c, Throwable t) {
                if (c.isCanceled() || (vigente != null && !vigente.sigueVisible())) return;
                String mensaje = app.getString(R.string.error_conexion) + " (" + Sesion.de(app).servidor() + ")";
                if (fallo != null) fallo.error(mensaje, Collections.emptyMap());
                else Toast.makeText(app, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static Modelos.ErrorApi leerError(Response<?> resp) {
        try {
            if (resp.errorBody() != null) {
                Modelos.ErrorApi e = ApiCliente.gson().fromJson(resp.errorBody().charStream(), Modelos.ErrorApi.class);
                if (e != null) return e;
            }
        } catch (RuntimeException ignorada) {
            // cuerpo no JSON
        }
        return new Modelos.ErrorApi();
    }

    public static Vigente mientrasViva(Activity a) {
        return () -> !a.isFinishing() && !a.isDestroyed();
    }
}
