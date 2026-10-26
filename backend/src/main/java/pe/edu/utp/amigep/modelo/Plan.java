package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Catálogo de planes de membresía (básico, estándar, VIP). */
@Entity
@Table(name = "Plan")
@Getter
@Setter
@NoArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPlan;

    private String nombre;
    private Integer duracionDias;
    private BigDecimal precio;
    private String descripcion;
}
