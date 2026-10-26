package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.Map;

/** Plan nutricional y recomendaciones generadas por el módulo inteligente. */
@Entity
@Table(name = "PlanAlimenticio")
@Getter
@Setter
@NoArgsConstructor
public class PlanAlimenticio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPlanAlimenticio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    private String objetivo;
    private String ritmo;
    private Integer caloriasObjetivo;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> contenido;

    private LocalDate fechaCreacion;
    private LocalDate fechaFinalizacion;
    private boolean activo = true;
}
