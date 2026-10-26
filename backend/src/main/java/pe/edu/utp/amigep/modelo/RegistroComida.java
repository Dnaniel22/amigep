package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.amigep.modelo.enums.TipoComida;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Comida consumida por el cliente, con calorías y macronutrientes. */
@Entity
@Table(name = "RegistroComida")
@Getter
@Setter
@NoArgsConstructor
public class RegistroComida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRegistro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    private TipoComida tipoComida;

    private String descripcion;
    private Integer calorias;
    private BigDecimal proteinas;
    private BigDecimal carbohidratos;
    private BigDecimal grasas;
}
