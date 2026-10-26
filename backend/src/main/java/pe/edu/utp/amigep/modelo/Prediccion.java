package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.edu.utp.amigep.modelo.enums.EstadoRiesgo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Resultado del módulo inteligente que estima el riesgo de abandono de un cliente. */
@Entity
@Table(name = "Prediccion")
@Getter
@Setter
@NoArgsConstructor
public class Prediccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPrediccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    private LocalDate fechaPrediccion;
    private BigDecimal probabilidad;

    /** Lista legible, p. ej. ["12 días sin asistir", "plan vencido"] */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> factoresRiesgo;

    @Enumerated(EnumType.STRING)
    private EstadoRiesgo estado;
}
