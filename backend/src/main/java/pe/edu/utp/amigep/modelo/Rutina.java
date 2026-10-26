package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.edu.utp.amigep.modelo.enums.Nivel;
import pe.edu.utp.amigep.modelo.enums.OrigenRutina;

import java.time.LocalDate;
import java.util.Map;

/**
 * Plan de entrenamiento del cliente, generado por el módulo inteligente o asignado por un entrenador.
 * El campo contenido (JSONB) guarda los días, ejercicios, series y repeticiones.
 */
@Entity
@Table(name = "Rutina")
@Getter
@Setter
@NoArgsConstructor
public class Rutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRutina;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    /** Null cuando la rutina la generó la IA. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idTrabajador")
    private Trabajador trabajador;

    private String objetivo;

    @Enumerated(EnumType.STRING)
    private Nivel nivel;

    private String ritmo;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> contenido;

    @Enumerated(EnumType.STRING)
    private OrigenRutina origen;

    private LocalDate fechaCreacion;
    private LocalDate fechaFinalizacion;
    private boolean activo = true;
}
