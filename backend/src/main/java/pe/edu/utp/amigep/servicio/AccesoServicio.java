package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.AdminDtos.ResultadoAccesoDto;
import pe.edu.utp.amigep.dto.ComunDtos.AccesoDto;
import pe.edu.utp.amigep.dto.ComunDtos.MembresiaDto;
import pe.edu.utp.amigep.modelo.Acceso;
import pe.edu.utp.amigep.modelo.Cliente;
import pe.edu.utp.amigep.modelo.Suscripcion;
import pe.edu.utp.amigep.modelo.enums.TipoAcceso;
import pe.edu.utp.amigep.repositorio.AccesoRepositorio;
import pe.edu.utp.amigep.repositorio.ClienteRepositorio;
import pe.edu.utp.amigep.repositorio.SuscripcionRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * RF-06 y RF-17: control de acceso por código QR (BPM Fig. 6).
 * QR identificado → valida membresía vigente → registra la asistencia con fecha y hora.
 * QR no identificado → el personal hace el registro manual por DNI.
 */
@Service
@RequiredArgsConstructor
public class AccesoServicio {

    /** Evita contar dos veces el mismo ingreso si el QR se escanea repetidamente. */
    private static final int MINUTOS_DUPLICADO = 60;

    private final ClienteRepositorio clienteRepo;
    private final AccesoRepositorio accesoRepo;
    private final SuscripcionRepositorio suscripcionRepo;
    private final ConsultaClienteServicio consulta;
    private final Mapeador mapeador;
    private final Clock reloj;

    @Transactional
    public ResultadoAccesoDto registrarPorQr(String codigoQR) {
        Optional<Cliente> cliente = clienteRepo.findByCodigoQR(codigoQR.trim());
        if (cliente.isEmpty()) {
            return new ResultadoAccesoDto(false, "NO_IDENTIFICADO",
                    "Código QR no identificado. Realice el registro manual por DNI.", null, null, null, null, null, null);
        }
        return registrar(cliente.get(), TipoAcceso.QR);
    }

    @Transactional
    public ResultadoAccesoDto registrarManual(String dni) {
        Optional<Cliente> cliente = clienteRepo.findByDni(dni.trim());
        if (cliente.isEmpty()) {
            return new ResultadoAccesoDto(false, "NO_IDENTIFICADO",
                    "No existe un cliente con el DNI " + dni + ".", null, null, null, null, null, null);
        }
        return registrar(cliente.get(), TipoAcceso.MANUAL);
    }

    private ResultadoAccesoDto registrar(Cliente c, TipoAcceso tipo) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        String iniciales = Textos.iniciales(c.getNombre(), c.getApellido());
        if (!c.isActivo()) {
            return resultado(false, "CLIENTE_INACTIVO", "La cuenta del cliente está desactivada.", c, iniciales, null, null);
        }
        List<Suscripcion> vigentes = suscripcionRepo.vigentesDeCliente(c.getIdCliente(), hoy);
        if (vigentes.isEmpty()) {
            MembresiaDto ultima = consulta.membresiaActual(c.getIdCliente()).map(s -> mapeador.membresia(s, hoy)).orElse(null);
            return resultado(false, "SIN_MEMBRESIA",
                    "Acceso no autorizado: membresía vencida o inexistente. Solicite renovar la membresía.", c, iniciales, ultima, null);
        }
        MembresiaDto membresia = mapeador.membresia(vigentes.get(0), hoy);
        Optional<Acceso> ultimo = accesoRepo.findFirstByClienteIdClienteOrderByFechaHoraDesc(c.getIdCliente());
        if (ultimo.isPresent() && ultimo.get().getFechaHora().isAfter(ahora.minusMinutes(MINUTOS_DUPLICADO))) {
            return resultado(true, "YA_REGISTRADO", "El ingreso ya fue registrado a las "
                    + ultimo.get().getFechaHora().format(Textos.HORA) + ".", c, iniciales, membresia, mapeador.acceso(ultimo.get()));
        }
        Acceso a = new Acceso();
        a.setCliente(c);
        a.setFechaHora(ahora.withNano(0));
        a.setTipo(tipo);
        a = accesoRepo.save(a);
        return resultado(true, "OK", "Ingreso autorizado. ¡Buen entrenamiento, " + c.getNombre().split(" ")[0] + "!",
                c, iniciales, membresia, mapeador.acceso(a));
    }

    private ResultadoAccesoDto resultado(boolean permitido, String motivo, String mensaje, Cliente c, String iniciales,
                                         MembresiaDto m, AccesoDto acceso) {
        return new ResultadoAccesoDto(permitido, motivo, mensaje, c.getIdCliente(), c.getCodigo(), c.getNombreCompleto(),
                iniciales, m, acceso);
    }

    @Transactional(readOnly = true)
    public List<AccesoDto> delDia(LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : consulta.hoy();
        return accesoRepo.entre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay()).stream().map(mapeador::acceso).toList();
    }
}
