package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.amigep.modelo.enums.TipoNotificacion;

import java.time.LocalDateTime;

/** Mensaje enviado a un cliente, opcionalmente originado por una acción de retención. */
@Entity
@Table(name = "Notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idAccion")
    private AccionRetencion accion;

    private String titulo;
    private String mensaje;
    private LocalDateTime fechaEnvio;
    private boolean leido;

    @Enumerated(EnumType.STRING)
    private TipoNotificacion tipo;
}
