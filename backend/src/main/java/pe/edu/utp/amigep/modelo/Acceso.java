package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.amigep.modelo.enums.TipoAcceso;

import java.time.LocalDateTime;

/** Ingreso del cliente al gimnasio (asistencia). */
@Entity
@Table(name = "Acceso")
@Getter
@Setter
@NoArgsConstructor
public class Acceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAcceso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idCliente")
    private Cliente cliente;

    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    private TipoAcceso tipo;
}
