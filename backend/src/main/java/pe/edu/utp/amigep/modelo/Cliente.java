package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.amigep.modelo.enums.Nivel;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Persona registrada en el gimnasio. Su código QR personal valida el ingreso (RF-17). */
@Entity
@Table(name = "Cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCliente;

    /** Entrenador asignado (0..1). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idEntrenador")
    private Trabajador entrenador;

    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private String contrasenaHash;
    private String telefono;
    private String codigoQR;
    private String objetivo;

    @Enumerated(EnumType.STRING)
    private Nivel nivel;

    /** En metros, p. ej. 1.75 */
    private BigDecimal altura;
    private LocalDate fechaNacimiento;
    private LocalDate fechaRegistro;
    private boolean activo = true;

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    /** Código visible en el panel web, p. ej. CL-1042. */
    public String getCodigo() {
        return String.format("CL-%04d", idCliente);
    }
}
