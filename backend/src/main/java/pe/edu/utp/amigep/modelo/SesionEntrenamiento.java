package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Cada sesión realizada de una rutina; permite medir el cumplimiento. */
@Entity
@Table(name = "SesionEntrenamiento")
@Getter
@Setter
@NoArgsConstructor
public class SesionEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idSesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idRutina")
    private Rutina rutina;

    private LocalDate fecha;
    private boolean completada;
    private String observaciones;
}
