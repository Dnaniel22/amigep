package pe.edu.utp.amigep.app.datos;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import pe.edu.utp.amigep.app.BuildConfig;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Crea el cliente Retrofit con el token JWT de la sesión en cada petición. */
public final class ApiCliente {

    private static AmigepApi api;
    private static final Gson GSON = new GsonBuilder().create();

    private ApiCliente() {
    }

    public static synchronized AmigepApi de(Context contexto) {
        if (api == null) {
            Context app = contexto.getApplicationContext();
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        String token = Sesion.de(app).token();
                        Request.Builder peticion = chain.request().newBuilder();
                        if (token != null) peticion.header("Authorization", "Bearer " + token);
                        return chain.proceed(peticion.build());
                    })
                    .addInterceptor(log)
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl(Sesion.de(app).servidor())
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create(GSON))
                    .build()
                    .create(AmigepApi.class);
        }
        return api;
    }

    /** Se llama al cambiar la URL del servidor. */
    public static synchronized void reiniciar() {
        api = null;
    }

    public static Gson gson() {
        return GSON;
    }
}
