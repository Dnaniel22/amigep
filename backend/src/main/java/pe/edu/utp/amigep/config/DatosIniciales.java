package pe.edu.utp.amigep.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.edu.utp.amigep.modelo.AccionRetencion;
import pe.edu.utp.amigep.modelo.Prediccion;
import pe.edu.utp.amigep.modelo.Trabajador;
import pe.edu.utp.amigep.modelo.enums.Rol;
import pe.edu.utp.amigep.modelo.enums.TipoNotificacion;
import pe.edu.utp.amigep.repositorio.AccionRetencionRepositorio;
import pe.edu.utp.amigep.repositorio.ClienteRepositorio;
import pe.edu.utp.amigep.repositorio.PrediccionRepositorio;
import pe.edu.utp.amigep.repositorio.TrabajadorRepositorio;
import pe.edu.utp.amigep.servicio.NotificacionServicio;
import pe.edu.utp.amigep.servicio.RetencionServicio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Al iniciar: crea el administrador inicial si no existe y, si está habilitado, carga los datos de demostración,
 * entrena el modelo de abandono con ese historial y calcula las predicciones del día.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatosIniciales implements ApplicationRunner {

    private final TrabajadorRepositorio trabajadorRepo;
    private final ClienteRepositorio clienteRepo;
    private final PrediccionRepositorio prediccionRepo;
    private final AccionRetencionRepositorio accionRepo;
    private final CargaDatosDemo cargaDemo;
    private final RetencionServicio retencion;
    private final NotificacionServicio notificaciones;
    private final PasswordEncoder encoder;
    private final Clock reloj;

    @Value("${amigep.admin-inicial.email}")
    private String adminEmail;
    @Value("${amigep.admin-inicial.contrasena}")
    private String adminContrasena;
    @Value("${amigep.datos-demo}")
    private boolean datosDemo;

    @Override
    public void run(ApplicationArguments args) {
        if (trabajadorRepo.countByRolAndActivoTrue(Rol.ADMINISTRADOR) == 0) {
            Trabajador admin = new Trabajador();
            admin.setNombre("Carlos");
            admin.setApellido("Mendoza Salazar");
            admin.setDni("10203040");
            admin.setEmail(adminEmail);
            admin.setContrasenaHash(encoder.encode(adminContrasena));
            admin.setRol(Rol.ADMINISTRADOR);
            admin.setEspecialidad("Administración");
            admin.setActivo(true);
            trabajadorRepo.save(admin);
            log.info("Administrador inicial creado: {}", adminEmail);
        }
        if (datosDemo && clienteRepo.count() == 0) {
            cargaDemo.cargar();
            var entrenamiento = retencion.entrenarModelo();
            log.info("Entrenamiento inicial del modelo: {} ejemplos, {} abandonos. {}", entrenamiento.muestras(),
                    entrenamiento.positivos(), entrenamiento.mensaje());
            retencion.actualizarPredicciones();
            accionesDeEjemplo();
        } else if (prediccionRepo.findByFechaPrediccion(LocalDate.now(reloj)).isEmpty() && clienteRepo.count() > 0) {
            retencion.actualizarPredicciones();
        }
    }

    /** Acciones de retención como las del prototipo A3. */
    private void accionesDeEjemplo() {
        LocalDate hoy = LocalDate.now(reloj);
        Trabajador admin = trabajadorRepo.findByEmailIgnoreCase(adminEmail).orElseThrow();
        List<Prediccion> enRiesgo = prediccionRepo.ultimasPorCliente();
        String[][] acciones = {
                {"Recordatorio de renovación", AccionRetencion.ENVIADA, "2"},
                {"Llamada de seguimiento", AccionRetencion.PENDIENTE, "3"},
                {"Oferta de nueva rutina IA", AccionRetencion.RETENIDO, "4"},
                {"Descuento en renovación", AccionRetencion.RETENIDO, "20"},
                {"Llamada de seguimiento", AccionRetencion.NO_RETENIDO, "35"}};
        for (int i = 0; i < acciones.length && i < enRiesgo.size(); i++) {
            Prediccion p = enRiesgo.get(i);
            AccionRetencion a = new AccionRetencion();
            a.setPrediccion(p);
            a.setTrabajador(admin);
            a.setTipoAccion(acciones[i][0]);
            a.setDescripcion("Factores: " + String.join(" · ", p.getFactoresRiesgo()));
            a.setFechaAccion(hoy.minusDays(Integer.parseInt(acciones[i][2])));
            a.setResultado(acciones[i][1]);
            a = accionRepo.save(a);
            if (AccionRetencion.ENVIADA.equals(acciones[i][1])) {
                notificaciones.enviar(p.getCliente(), TipoNotificacion.RETENCION, a.getTipoAccion(),
                        "Hola " + p.getCliente().getNombre().split(" ")[0]
                                + ", tu membresía necesita renovarse. Acércate a recepción y sigue entrenando con nosotros.", a);
            }
        }
    }
}
