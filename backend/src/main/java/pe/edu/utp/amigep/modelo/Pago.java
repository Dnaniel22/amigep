package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.amigep.modelo.enums.EstadoPago;
import pe.edu.utp.amigep.modelo.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago registrado en recepción sobre una suscripción. */
@Entity
@Table(name = "Pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idSuscripcion")
    private Suscripcion suscripcion;

    private BigDecimal monto;
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    private EstadoPago estado;
}
