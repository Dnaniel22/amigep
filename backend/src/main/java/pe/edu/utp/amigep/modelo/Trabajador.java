package pe.edu.utp.amigep.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.utp.amigep.modelo.enums.Rol;

/** Personal del gimnasio: administradores y entrenadores, diferenciados por su rol. */
@Entity
@Table(name = "Trabajador")
@Getter
@Setter
@NoArgsConstructor
public class Trabajador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTrabajador;

    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    private String especialidad;
    private boolean activo = true;

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
