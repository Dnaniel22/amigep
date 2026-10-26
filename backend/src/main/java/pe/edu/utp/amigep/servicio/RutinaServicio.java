package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.ClienteAppDtos.*;
import pe.edu.utp.amigep.dto.ComunDtos.RutinaDto;
import pe.edu.utp.amigep.dto.ComunDtos.SesionDto;
import pe.edu.utp.amigep.dto.EntrenadorDtos.GuardarRutinaRequest;
import pe.edu.utp.amigep.excepcion.NoEncontradoException;
import pe.edu.utp.amigep.excepcion.ReglaNegocioException;
import pe.edu.utp.amigep.modelo.*;
import pe.edu.utp.amigep.modelo.enums.Nivel;
import pe.edu.utp.amigep.modelo.enums.Objetivo;
import pe.edu.utp.amigep.modelo.enums.OrigenRutina;
import pe.edu.utp.amigep.modelo.enums.TipoNotificacion;
import pe.edu.utp.amigep.repositorio.*;
import pe.edu.utp.amigep.servicio.ia.GeneradorRutinas;
import pe.edu.utp.amigep.servicio.ia.PerfilFisico;

import java.time.LocalDate;
import java.util.*;

/** RF-08, RF-09 y RF-11: rutinas del entrenador (BPM Fig. 8) y rutinas generadas por IA (BPM Fig. 9). */
@Service
@RequiredArgsConstructor
public class RutinaServicio {

    private final RutinaRepositorio rutinaRepo;
    private final SesionEntrenamientoRepositorio sesionRepo;
    private final ProgresoRepositorio progresoRepo;
    private final TrabajadorRepositorio trabajadorRepo;
    private final ConsultaClienteServicio consulta;
    private final NotificacionServicio notificaciones;
    private final GeneradorRutinas generador;
    private final Mapeador mapeador;

    // ------------------------------------------------------------------ Cliente: IA

    @Transactional(readOnly = true)
    public VistaPreviaRutinaDto vistaPrevia(Integer idCliente, String objetivoTexto, Nivel nivel, Integer sesiones) {
        Cliente c = consulta.cliente(idCliente);
        Objetivo objetivo = Objetivo.desde(objetivoTexto);
        Nivel n = nivel != null ? nivel : c.getNivel() != null ? c.getNivel() : Nivel.PRINCIPIANTE;
        int s = sesiones != null ? sesiones : sesionesSugeridas(idCliente);
        Map<String, Object> contenido = generador.generar(objetivo, n, s, consulta.perfilFisico(c));
        return new VistaPreviaRutinaDto(contenido.get("nombre").toString(), objetivo.getEtiqueta(), n.name(),
                s + " sesiones/semana", contenido);
    }

    /** BPM Fig. 9: recopila datos → ¿suficientes? → genera → registra como activa. */
    @Transactional
    public RutinaDto generarConIa(Integer idCliente, GenerarRutinaRequest req) {
        Cliente c = consulta.cliente(idCliente);
        LocalDate hoy = consulta.hoy();
        if (req.nivel() != null) c.setNivel(req.nivel());
        if (req.altura() != null) c.setAltura(req.altura());
        if (req.peso() != null) registrarPeso(c, req.peso(), hoy);

        Map<String, String> faltantes = new LinkedHashMap<>();
        if (c.getNivel() == null) faltantes.put("nivel", "Seleccione su nivel");
        if (c.getAltura() == null) faltantes.put("altura", "Ingrese su altura");
        if (req.peso() == null && consulta.pesoActual(idCliente).isEmpty()) faltantes.put("peso", "Ingrese su peso");
        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("Completa tus datos (nivel, peso y altura) para generar la rutina", faltantes);
        }

        Objetivo objetivo = Objetivo.desde(req.objetivo());
        int sesiones = req.sesionesPorSemana() != null ? req.sesionesPorSemana() : sesionesSugeridas(idCliente);
        PerfilFisico perfil = new PerfilFisico(req.peso() != null ? req.peso() : consulta.pesoActual(idCliente).orElse(null),
                c.getAltura(), consulta.edad(c));
        Map<String, Object> contenido = generador.generar(objetivo, c.getNivel(), sesiones, perfil);

        desactivarActivas(idCliente);
        Rutina r = new Rutina();
        r.setCliente(c);
        r.setTrabajador(null);
        r.setObjetivo(objetivo.getEtiqueta());
        r.setNivel(c.getNivel());
        r.setRitmo(sesiones + " sesiones/semana");
        r.setContenido(contenido);
        r.setOrigen(OrigenRutina.IA);
        r.setFechaCreacion(hoy);
        r.setFechaFinalizacion(hoy.plusWeeks(((Number) contenido.get("semanas")).intValue()));
        r.setActivo(true);
        r = rutinaRepo.save(r);
        c.setObjetivo(objetivo.getEtiqueta());
        return mapeador.rutina(r, 0, hoy);
    }

    private int sesionesSugeridas(Integer idCliente) {
        return consulta.rutinaActiva(idCliente).map(Mapeador::sesionesPorSemana).orElse(4);
    }

    private void registrarPeso(Cliente c, java.math.BigDecimal peso, LocalDate hoy) {
        Progreso p = new Progreso();
        p.setCliente(c);
        p.setFecha(hoy);
        p.setPeso(peso);
        progresoRepo.save(p);
    }

    private void desactivarActivas(Integer idCliente) {
        LocalDate hoy = consulta.hoy();
        for (Rutina anterior : rutinaRepo.findByClienteIdClienteAndActivoTrue(idCliente)) {
            anterior.setActivo(false);
            if (anterior.getFechaFinalizacion() == null || anterior.getFechaFinalizacion().isAfter(hoy)) {
                anterior.setFechaFinalizacion(hoy);
            }
        }
    }

    @Transactional(readOnly = true)
    public RutinasClienteDto rutinasDeCliente(Integer idCliente) {
        LocalDate hoy = consulta.hoy();
        List<Rutina> todas = rutinaRepo.findByClienteIdClienteOrderByFechaCreacionDescIdRutinaDesc(idCliente);
        RutinaDto activa = null;
        List<RutinaDto> anteriores = new ArrayList<>();
        for (Rutina r : todas) {
            RutinaDto dto = mapeador.rutina(r, consulta.sesionesCompletadas(r), hoy);
            if (r.isActivo() && activa == null) activa = dto;
            else anteriores.add(dto);
        }
        List<SesionDto> sesiones = activa == null ? List.of()
                : sesionRepo.findByRutinaIdRutinaOrderByFechaDescIdSesionDesc(activa.idRutina()).stream()
                .limit(10).map(mapeador::sesion).toList();
        return new RutinasClienteDto(activa, anteriores, sesiones);
    }

    /** Sesión del día: rota los días de la rutina según las sesiones ya completadas. */
    @Transactional(readOnly = true)
    public SesionHoyDto sesionDeHoy(Integer idCliente) {
        Optional<Rutina> opt = consulta.rutinaActiva(idCliente);
        if (opt.isEmpty()) return null;
        Rutina r = opt.get();
        List<Map<String, Object>> dias = dias(r);
        if (dias.isEmpty()) return null;
        long completadas = consulta.sesionesCompletadas(r);
        boolean hoyCompletada = sesionRepo.findByRutinaIdRutinaAndFecha(r.getIdRutina(), consulta.hoy()).stream()
                .anyMatch(SesionEntrenamiento::isCompletada);
        int indice = (int) ((hoyCompletada ? completadas - 1 : completadas) % dias.size());
        if (indice < 0) indice = 0;
        Map<String, Object> dia = dias.get(indice);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ejercicios = (List<Map<String, Object>>) dia.getOrDefault("ejercicios", List.of());
        Object duracion = r.getContenido().getOrDefault("duracionMinutos", 50);
        String titulo = "Rutina " + Mapeador.nombreRutina(r) + " · Día " + (indice + 1);
        String detalle = dia.getOrDefault("nombre", "") + " · " + ejercicios.size() + " ejercicios · " + duracion + " min";
        return new SesionHoyDto(r.getIdRutina(), indice + 1, titulo, detalle,
                String.valueOf(dia.getOrDefault("enfoque", "")), ejercicios, hoyCompletada);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> dias(Rutina r) {
        Object d = r.getContenido() == null ? null : r.getContenido().get("dias");
        return d instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    /** RF-09: el cliente marca su sesión del día; se usa para medir el cumplimiento. */
    @Transactional
    public SesionDto registrarSesion(Integer idCliente, Integer idRutina, RegistrarSesionRequest req) {
        Rutina r = rutinaRepo.findById(idRutina).orElseThrow(() -> new NoEncontradoException("Rutina no encontrada"));
        if (!r.getCliente().getIdCliente().equals(idCliente)) throw new NoEncontradoException("Rutina no encontrada");
        if (!r.isActivo()) throw new ReglaNegocioException("La rutina ya no está activa");
        LocalDate hoy = consulta.hoy();
        SesionHoyDto hoyDto = sesionDeHoy(idCliente);
        SesionEntrenamiento s = sesionRepo.findByRutinaIdRutinaAndFecha(idRutina, hoy).stream().findFirst()
                .orElseGet(SesionEntrenamiento::new);
        boolean completada = req == null || req.completada() == null || req.completada();
        s.setRutina(r);
        s.setFecha(hoy);
        s.setCompletada(completada);
        String obs = req != null && req.observaciones() != null && !req.observaciones().isBlank() ? req.observaciones().trim()
                : hoyDto != null ? "Día " + hoyDto.dia() + " · " + hoyDto.detalle().split(" · ")[0] : null;
        s.setObservaciones(obs);
        return mapeador.sesion(sesionRepo.save(s));
    }

    // ------------------------------------------------------------------ Entrenador

    @Transactional
    public RutinaDto guardarDeEntrenador(Integer idEntrenador, Integer idCliente, Integer idRutina, GuardarRutinaRequest req) {
        Cliente c = consulta.clienteDeEntrenador(idCliente, idEntrenador);
        Trabajador entrenador = trabajadorRepo.findById(idEntrenador).orElseThrow();
        LocalDate hoy = consulta.hoy();

        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("nombre", req.nombre().trim());
        contenido.put("semanas", req.semanas());
        contenido.put("sesionesPorSemana", req.sesionesPorSemana());
        int ejerciciosMax = 0;
        List<Map<String, Object>> dias = new ArrayList<>();
        int n = 1;
        for (var d : req.dias()) {
            List<Map<String, Object>> ejercicios = new ArrayList<>();
            for (var e : d.ejercicios()) {
                Map<String, Object> ej = new LinkedHashMap<>();
                ej.put("nombre", e.nombre().trim());
                ej.put("series", e.series());
                ej.put("repeticiones", e.repeticiones().trim());
                ej.put("descansoSeg", e.descansoSeg() != null ? e.descansoSeg() : 60);
                ejercicios.add(ej);
            }
            ejerciciosMax = Math.max(ejerciciosMax, ejercicios.size());
            Map<String, Object> dia = new LinkedHashMap<>();
            dia.put("dia", n++);
            dia.put("nombre", d.nombre().trim());
            dia.put("enfoque", d.enfoque() == null ? "" : d.enfoque().trim());
            dia.put("ejercicios", ejercicios);
            dias.add(dia);
        }
        contenido.put("duracionMinutos", Math.round((10 + ejerciciosMax * 8) / 5.0) * 5);
        contenido.put("dias", dias);
        contenido.put("notas", req.notas() == null ? List.of() : req.notas());

        Rutina r;
        boolean nueva = idRutina == null;
        if (nueva) {
            desactivarActivas(idCliente);
            r = new Rutina();
            r.setCliente(c);
            r.setFechaCreacion(hoy);
            r.setActivo(true);
        } else {
            r = rutinaRepo.findById(idRutina).orElseThrow(() -> new NoEncontradoException("Rutina no encontrada"));
            if (!r.getCliente().getIdCliente().equals(idCliente)) throw new NoEncontradoException("Rutina no encontrada");
        }
        r.setTrabajador(entrenador);
        r.setOrigen(OrigenRutina.ENTRENADOR);
        r.setObjetivo(Objetivo.desde(req.objetivo()).getEtiqueta());
        r.setNivel(req.nivel());
        r.setRitmo(req.sesionesPorSemana() + " sesiones/semana");
        r.setContenido(contenido);
        r.setFechaFinalizacion(r.getFechaCreacion().plusWeeks(req.semanas()));
        r = rutinaRepo.save(r);

        // BPM Fig. 8: rutina completa → notifica al cliente
        notificaciones.enviar(c, TipoNotificacion.RUTINA, nueva ? "Nueva rutina asignada" : "Tu rutina fue actualizada",
                entrenador.getNombreCompleto() + " " + (nueva ? "te asignó" : "actualizó") + " la rutina \"" + req.nombre().trim()
                        + "\" (" + req.sesionesPorSemana() + " sesiones/semana, " + req.semanas() + " semanas).", null);
        return mapeador.rutina(r, consulta.sesionesCompletadas(r), hoy);
    }

    @Transactional(readOnly = true)
    public List<RutinaDto> rutinasDeEntrenador(Integer idEntrenador) {
        LocalDate hoy = consulta.hoy();
        return rutinaRepo.deClientesDeEntrenador(idEntrenador).stream()
                .map(r -> mapeador.rutina(r, consulta.sesionesCompletadas(r), hoy)).toList();
    }

    @Transactional(readOnly = true)
    public List<RutinaDto> rutinasDeClienteParaEntrenador(Integer idEntrenador, Integer idCliente) {
        consulta.clienteDeEntrenador(idCliente, idEntrenador);
        LocalDate hoy = consulta.hoy();
        return rutinaRepo.findByClienteIdClienteOrderByFechaCreacionDescIdRutinaDesc(idCliente).stream()
                .map(r -> mapeador.rutina(r, consulta.sesionesCompletadas(r), hoy)).toList();
    }
}
