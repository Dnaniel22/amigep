package pe.edu.utp.amigep.app.datos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Modelos JSON de la API REST de AMIGEP (mismos contratos que usa el panel web).
 * Las fechas llegan como texto ISO ("2026-10-24" o "2026-10-24T07:05:00") y se formatean en la vista.
 */
public final class Modelos {

    private Modelos() {
    }

    // ------------------------------------------------------------------ Autenticación (RF-01)

    public static class LoginRequest {
        public String correo;
        public String contrasena;
        public String rol;

        public LoginRequest(String correo, String contrasena, String rol) {
            this.correo = correo;
            this.contrasena = contrasena;
            this.rol = rol;
        }
    }

    public static class LoginResponse {
        public String token;
        public long expiraEnSegundos;
        public String rol;
        public int id;
        public String nombre;
        public String apellido;
        public String correo;
        public String iniciales;
    }

    public static class RecuperarRequest {
        public String correo;
        public String rol;

        public RecuperarRequest(String correo, String rol) {
            this.correo = correo;
            this.rol = rol;
        }
    }

    public static class RecuperarResponse {
        public String mensaje;
        public String codigoDesarrollo;
    }

    public static class RestablecerRequest {
        public String correo;
        public String rol;
        public String codigo;
        public String nuevaContrasena;

        public RestablecerRequest(String correo, String rol, String codigo, String nuevaContrasena) {
            this.correo = correo;
            this.rol = rol;
            this.codigo = codigo;
            this.nuevaContrasena = nuevaContrasena;
        }
    }

    public static class CambiarContrasenaRequest {
        public String actual;
        public String nueva;

        public CambiarContrasenaRequest(String actual, String nueva) {
            this.actual = actual;
            this.nueva = nueva;
        }
    }

    public static class Mensaje {
        public String mensaje;
    }

    /** Cuerpo de error del backend: {mensaje, campos}. */
    public static class ErrorApi {
        public String mensaje;
        public Map<String, String> campos;
    }

    // ------------------------------------------------------------------ Comunes

    public static class Plan {
        public int idPlan;
        public String nombre;
        public int duracionDias;
        public double precio;
        public String descripcion;
    }

    /** estado: VIGENTE, POR_VENCER, VENCIDA, PROGRAMADA o SUSPENDIDA */
    public static class Membresia {
        public int idSuscripcion;
        public int idPlan;
        public String plan;
        public double precio;
        public String fechaInicio;
        public String fechaCaducidad;
        public boolean activo;
        public String estado;
        public long diasRestantes;
    }

    public static class Pago {
        public int idPago;
        public int idSuscripcion;
        public int idCliente;
        public String cliente;
        public String plan;
        public double monto;
        public String fechaPago;
        public String metodoPago;
        public String estado;
    }

    public static class Acceso {
        public int idAcceso;
        public int idCliente;
        public String codigoCliente;
        public String cliente;
        public String fechaHora;
        public String tipo;
    }

    public static class Notificacion {
        public int idNotificacion;
        public String titulo;
        public String mensaje;
        public String fechaEnvio;
        public boolean leido;
        public String tipo;
    }

    public static class EntrenadorResumen {
        public int idTrabajador;
        public String nombre;
        public String iniciales;
        public String especialidad;
    }

    public static class Ejercicio {
        public String nombre;
        public int series;
        public String repeticiones;
        public Integer descansoSeg;

        public Ejercicio() {
        }

        public Ejercicio(String nombre, int series, String repeticiones, Integer descansoSeg) {
            this.nombre = nombre;
            this.series = series;
            this.repeticiones = repeticiones;
            this.descansoSeg = descansoSeg;
        }
    }

    public static class DiaRutina {
        public int dia;
        public String nombre;
        public String enfoque;
        public List<Ejercicio> ejercicios = new ArrayList<>();
    }

    /** Contenido JSONB de la rutina: días, ejercicios, series y repeticiones. */
    public static class ContenidoRutina {
        public String nombre;
        public Integer semanas;
        public Integer sesionesPorSemana;
        public Integer duracionMinutos;
        public List<DiaRutina> dias = new ArrayList<>();
        public List<String> notas = new ArrayList<>();
    }

    /** estado: VIGENTE, POR_FINALIZAR, FINALIZADA o INACTIVA · origen: IA o ENTRENADOR */
    public static class Rutina {
        public int idRutina;
        public int idCliente;
        public String cliente;
        public String nombre;
        public String objetivo;
        public String nivel;
        public String ritmo;
        public String origen;
        public String entrenador;
        public String fechaCreacion;
        public String fechaFinalizacion;
        public boolean activo;
        public int semanaActual;
        public int semanasTotales;
        public long sesionesCompletadas;
        public int cumplimientoPct;
        public String estado;
        public ContenidoRutina contenido;
    }

    public static class Sesion {
        public int idSesion;
        public String fecha;
        public boolean completada;
        public String observaciones;
    }

    public static class Medicion {
        public int idProgreso;
        public String fecha;
        public Double peso;
        public Double grasaCorporal;
        public Map<String, Object> medidas;
        public String observaciones;
    }

    /** Estadísticas de progreso (RF-14). */
    public static class Estadisticas {
        public Double pesoActual;
        public Double pesoInicial;
        public Double variacionPesoPct;
        public int asistencias30;
        public int asistencia30Pct;
        public long sesionesCompletadas;
        public int cumplimientoRutinasPct;
        public List<Medicion> mediciones = new ArrayList<>();
    }

    public static class RegistroComida {
        public int idRegistro;
        public String fecha;
        public String tipoComida;
        public String descripcion;
        public Integer calorias;
        public Double proteinas;
        public Double carbohidratos;
        public Double grasas;
    }

    public static class ComidaPlan {
        public String tipo;
        public String nombre;
        public String descripcion;
        public int calorias;
        public double proteinas;
        public double carbohidratos;
        public double grasas;
    }

    public static class Macros {
        public double proteinas;
        public double carbohidratos;
        public double grasas;
        public int pctProteinas;
        public int pctCarbohidratos;
        public int pctGrasas;
    }

    public static class ContenidoPlan {
        public String objetivoEtiqueta;
        public List<ComidaPlan> comidas = new ArrayList<>();
        public Macros macros;
        public Double metaSemanalKg;
        public Double aguaLitros;
        public List<String> recomendaciones = new ArrayList<>();
        public String aviso;
    }

    public static class PlanAlimenticio {
        public int idPlanAlimenticio;
        public String objetivo;
        public String ritmo;
        public Integer caloriasObjetivo;
        public ContenidoPlan contenido;
        public String fechaCreacion;
    }

    public static class ResumenNutricional {
        public String fecha;
        public int calorias;
        public Integer caloriasObjetivo;
        public int pctMetaCalorica;
        public double proteinas;
        public double carbohidratos;
        public double grasas;
        public int pctProteinas;
        public int pctCarbohidratos;
        public int pctGrasas;
        public int comidasRegistradas;
    }

    // ------------------------------------------------------------------ Cliente (C2 - C6)

    public static class SesionHoy {
        public int idRutina;
        public int dia;
        public String titulo;
        public String detalle;
        public String enfoque;
        public List<Ejercicio> ejercicios = new ArrayList<>();
        public boolean completada;
    }

    public static class Inicio {
        public int idCliente;
        public String nombre;
        public String apellido;
        public String iniciales;
        public String codigoQR;
        public Membresia membresia;
        public SesionHoy sesionHoy;
        public long notificacionesNoLeidas;
        public int asistenciasMes;
    }

    public static class PerfilCliente {
        public int idCliente;
        public String codigo;
        public String nombre;
        public String apellido;
        public String iniciales;
        public String email;
        public String telefono;
        public String dni;
        public String objetivo;
        public String nivel;
        public Double altura;
        public Double peso;
        public String fechaNacimiento;
        public String fechaRegistro;
        public String plan;
        public EntrenadorResumen entrenador;
        public Estadisticas estadisticas;
    }

    public static class ActualizarPerfilRequest {
        public String telefono;
        public String objetivo;
        public String nivel;
        public Double altura;
        public String fechaNacimiento;
        public Double peso;
    }

    public static class MembresiaCliente {
        public Membresia actual;
        public Membresia proxima;
        public List<Plan> planes = new ArrayList<>();
        public List<Pago> historialPagos = new ArrayList<>();
        public String formaPago;
    }

    public static class GenerarRutinaRequest {
        public String objetivo;
        public String nivel;
        public Integer sesionesPorSemana;
        public Double peso;
        public Double altura;
    }

    public static class RegistrarSesionRequest {
        public Boolean completada;
        public String observaciones;

        public RegistrarSesionRequest(Boolean completada, String observaciones) {
            this.completada = completada;
            this.observaciones = observaciones;
        }
    }

    public static class RutinasCliente {
        public Rutina activa;
        public List<Rutina> anteriores = new ArrayList<>();
        public List<Sesion> sesionesRecientes = new ArrayList<>();
    }

    public static class VistaPrevia {
        public String nombre;
        public String objetivo;
        public String nivel;
        public String ritmo;
        public ContenidoRutina contenido;
    }

    public static class Alimentacion {
        public PlanAlimenticio plan;
        public ResumenNutricional hoy;
        public String sugerencia;
        public List<RegistroComida> registrosHoy = new ArrayList<>();
        public String aviso;
    }

    public static class RegistrarComidaRequest {
        public String tipoComida;
        public String descripcion;
        public Integer calorias;
        public Double proteinas;
        public Double carbohidratos;
        public Double grasas;
    }

    public static class EvaluacionComida {
        public RegistroComida registro;
        public boolean cumpleObjetivo;
        public String mensaje;
        public ResumenNutricional hoy;
    }

    public static class RegistrarProgresoRequest {
        public Double peso;
        public Double grasaCorporal;
        public Map<String, Object> medidas;
        public String observaciones;
    }

    // ------------------------------------------------------------------ Entrenador (E1, E2)

    /** estadoRutina: VIGENTE, POR_FINALIZAR o PENDIENTE */
    public static class ClienteAsignado {
        public int idCliente;
        public String nombre;
        public String apellido;
        public String iniciales;
        public String objetivo;
        public String nivel;
        public String rutina;
        public int semanaActual;
        public int semanasTotales;
        public String estadoRutina;
        public String detalle;
    }

    public static class ResumenClientes {
        public int total;
        public int vigentes;
        public int porFinalizar;
        public int pendientes;
        public List<ClienteAsignado> clientes = new ArrayList<>();
    }

    public static class DetalleCliente {
        public int idCliente;
        public String nombre;
        public String apellido;
        public String iniciales;
        public String email;
        public String telefono;
        public String objetivo;
        public String nivel;
        public Double altura;
        public Integer edad;
        public Membresia membresia;
        public Rutina rutinaActiva;
        public Double pesoActual;
        public Double variacionPesoPct;
        public int asistenciasMes;
        public int diasMes;
        public List<Acceso> ultimasAsistencias = new ArrayList<>();
        public Estadisticas estadisticas;
    }

    public static class GuardarRutinaRequest {
        public String nombre;
        public String objetivo;
        public String nivel;
        public Integer sesionesPorSemana;
        public Integer semanas;
        public List<DiaRutina> dias = new ArrayList<>();
        public List<String> notas = new ArrayList<>();
    }

    public static class AlimentacionCliente {
        public Alimentacion hoy;
        public List<RegistroComida> ultimos7Dias = new ArrayList<>();
        public int diasConRegistro;
    }

    public static class PerfilEntrenador {
        public int idTrabajador;
        public String nombre;
        public String apellido;
        public String iniciales;
        public String email;
        public String especialidad;
        public int clientesAsignados;
        public int rutinasActivas;
    }

    // ------------------------------------------------------------------ Reportes (RF-20)

    public static class Columna {
        public String clave;
        public String titulo;
        public String tipo;
    }

    public static class Reporte {
        public String codigo;
        public String titulo;
        public String descripcion;
        public String generado;
        public Map<String, Object> filtros;
        public List<Columna> columnas = new ArrayList<>();
        public List<Map<String, Object>> filas = new ArrayList<>();
        public Map<String, Object> resumen;
    }
}
