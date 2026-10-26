package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Acción que ejecuta un trabajador en respuesta a una predicción de riesgo. */
@Entity
@Table(name = "AccionRetencion")
@Getter
@Setter
@NoArgsConstructor
public class AccionRetencion {

    /** Valores usados en el campo resultado. */
    public static final String PENDIENTE = "PENDIENTE";
    public static final String ENVIADA = "ENVIADA";
    public static final String RETENIDO = "RETENIDO";
    public static final String NO_RETENIDO = "NO_RETENIDO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idPrediccion")
    private Prediccion prediccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idTrabajador")
    private Trabajador trabajador;

    private String tipoAccion;
    private String descripcion;
    private LocalDate fechaAccion;
    private String resultado;
}
