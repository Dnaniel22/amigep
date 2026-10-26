package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.modelo.Suscripcion;
import pe.edu.utp.amigep.modelo.enums.TipoNotificacion;
import pe.edu.utp.amigep.repositorio.SuscripcionRepositorio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Ejecución periódica del módulo inteligente (RF-18) y avisos de vencimiento (RF-19). */
@Slf4j
@Component
@RequiredArgsConstructor
public class TareasProgramadas {

    private final RetencionServicio retencion;
    private final SuscripcionRepositorio suscripcionRepo;
    private final NotificacionServicio notificaciones;
    private final ConsultaClienteServicio consulta;

    @Scheduled(cron = "${amigep.ia.cron-predicciones}", zone = "${amigep.zona-horaria}")
    public void predicciones() {
        retencion.actualizarPredicciones();
    }

    @Scheduled(cron = "${amigep.ia.cron-vencimientos}", zone = "${amigep.zona-horaria}")
    @Transactional
    public void avisosDeVencimiento() {
        LocalDate hoy = consulta.hoy();
        int enviados = 0;
        for (Suscripcion s : suscripcionRepo.porVencer(hoy, hoy.plusDays(3))) {
            if (notificaciones.enviadaRecientemente(s.getCliente().getIdCliente(), TipoNotificacion.VENCIMIENTO, 3)) continue;
            long dias = ChronoUnit.DAYS.between(hoy, s.getFechaCaducidad());
            notificaciones.enviar(s.getCliente(), TipoNotificacion.VENCIMIENTO,
                    dias == 0 ? "Tu membresía vence hoy" : "Tu membresía vence en " + dias + (dias == 1 ? " día" : " días"),
                    "Tu plan " + s.getPlan().getNombre() + " vence el " + s.getFechaCaducidad().format(Textos.FECHA)
                            + ". Renueva en recepción para no perder tu progreso.", null);
            enviados++;
        }
        log.info("Avisos de vencimiento enviados: {}", enviados);
    }
}
