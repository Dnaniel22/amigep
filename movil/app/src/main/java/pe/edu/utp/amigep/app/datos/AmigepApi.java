package pe.edu.utp.amigep.app.datos;

import java.util.List;
import java.util.Map;

import pe.edu.utp.amigep.app.datos.Modelos.*;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/** Endpoints de la API REST de AMIGEP (backend Spring Boot) que consume la app móvil. */
public interface AmigepApi {

    // ---------------------------------------------------------------- Autenticación (RF-01)
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest req);

    @POST("api/auth/recuperar")
    Call<RecuperarResponse> recuperar(@Body RecuperarRequest req);

    @POST("api/auth/restablecer")
    Call<Mensaje> restablecer(@Body RestablecerRequest req);

    @PUT("api/auth/contrasena")
    Call<Mensaje> cambiarContrasena(@Body CambiarContrasenaRequest req);

    // ---------------------------------------------------------------- Cliente
    @GET("api/cliente/inicio")
    Call<Inicio> inicio();

    @GET("api/cliente/perfil")
    Call<PerfilCliente> perfil();

    @PUT("api/cliente/perfil")
    Call<PerfilCliente> actualizarPerfil(@Body ActualizarPerfilRequest req);

    @GET("api/cliente/membresia")
    Call<MembresiaCliente> membresia();

    @GET("api/cliente/rutinas")
    Call<RutinasCliente> rutinas();

    @GET("api/cliente/rutinas/vista-previa")
    Call<VistaPrevia> vistaPrevia(@Query("objetivo") String objetivo, @Query("sesionesPorSemana") Integer sesiones);

    @POST("api/cliente/rutinas/generar")
    Call<Rutina> generarRutina(@Body GenerarRutinaRequest req);

    @POST("api/cliente/rutinas/{idRutina}/sesiones")
    Call<Sesion> registrarSesion(@Path("idRutina") int idRutina, @Body RegistrarSesionRequest req);

    @GET("api/cliente/alimentacion")
    Call<Alimentacion> alimentacion();

    @POST("api/cliente/alimentacion/generar")
    Call<Alimentacion> generarPlanAlimenticio(@Body Map<String, String> req);

    @POST("api/cliente/alimentacion/comidas")
    Call<EvaluacionComida> registrarComida(@Body RegistrarComidaRequest req);

    @GET("api/cliente/progreso")
    Call<Estadisticas> progreso();

    @POST("api/cliente/progreso")
    Call<Estadisticas> registrarProgreso(@Body RegistrarProgresoRequest req);

    @GET("api/cliente/notificaciones")
    Call<List<Notificacion>> notificaciones();

    @PATCH("api/cliente/notificaciones/{id}/leida")
    Call<Notificacion> marcarLeida(@Path("id") int idNotificacion);

    // ---------------------------------------------------------------- Entrenador
    @GET("api/entrenador/clientes")
    Call<ResumenClientes> misClientes(@Query("q") String q, @Query("estado") String estado);

    @GET("api/entrenador/clientes/{id}")
    Call<DetalleCliente> detalleCliente(@Path("id") int idCliente);

    @POST("api/entrenador/clientes/{id}/rutinas")
    Call<Rutina> crearRutina(@Path("id") int idCliente, @Body GuardarRutinaRequest req);

    @PUT("api/entrenador/clientes/{id}/rutinas/{idRutina}")
    Call<Rutina> actualizarRutina(@Path("id") int idCliente, @Path("idRutina") int idRutina, @Body GuardarRutinaRequest req);

    @GET("api/entrenador/clientes/{id}/alimentacion")
    Call<AlimentacionCliente> alimentacionCliente(@Path("id") int idCliente);

    @GET("api/entrenador/rutinas")
    Call<List<Rutina>> rutinasEntrenador();

    @GET("api/entrenador/agenda")
    Call<List<Acceso>> agenda(@Query("desde") String desde, @Query("hasta") String hasta);

    @GET("api/entrenador/perfil")
    Call<PerfilEntrenador> perfilEntrenador();

    // ---------------------------------------------------------------- Reportes (RF-20)
    @GET("api/reportes/{codigo}")
    Call<Reporte> reporte(@Path("codigo") String codigo, @QueryMap Map<String, String> filtros);
}
