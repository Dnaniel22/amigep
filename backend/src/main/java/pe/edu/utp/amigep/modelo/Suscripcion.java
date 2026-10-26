package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Membresía del cliente: vínculo entre un cliente y un plan, con vigencia. */
@Entity
@Table(name = "Suscripcion")
@Getter
@Setter
@NoArgsConstructor
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSuscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idPlan")
    private Plan plan;

    private boolean activo = true;
    private LocalDate fechaInicio;
    private LocalDate fechaCaducidad;

    /** Vigente en la fecha indicada: activa y dentro del rango [inicio, caducidad]. */
    public boolean esVigente(LocalDate fecha) {
        return activo && !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaCaducidad);
    }
}
