package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Medición física periódica del cliente. */
@Entity
@Table(name = "Progreso")
@Getter
@Setter
@NoArgsConstructor
public class Progreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idProgreso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    private LocalDate fecha;
    private BigDecimal peso;
    private BigDecimal grasaCorporal;

    /** Medidas corporales en cm, p. ej. {"cintura": 82, "pecho": 101} */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> medidas;

    private String observaciones;
}
