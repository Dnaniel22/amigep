package pe.edu.utp.amigep.servicio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.amigep.dto.ClienteAppDtos.*;
import pe.edu.utp.amigep.dto.ComunDtos.RegistroComidaDto;
import pe.edu.utp.amigep.dto.ComunDtos.ResumenNutricionalDto;
import pe.edu.utp.amigep.dto.EntrenadorDtos.AlimentacionClienteDto;
import pe.edu.utp.amigep.modelo.Cliente;
import pe.edu.utp.amigep.modelo.PlanAlimenticio;
import pe.edu.utp.amigep.modelo.RegistroComida;
import pe.edu.utp.amigep.modelo.enums.Objetivo;
import pe.edu.utp.amigep.repositorio.PlanAlimenticioRepositorio;
import pe.edu.utp.amigep.repositorio.RegistroComidaRepositorio;
import pe.edu.utp.amigep.servicio.ia.GeneradorPlanAlimenticio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** RF-12 y RF-13: registro de comidas y recomendaciones alimenticias (BPM Fig. 10, pantalla C5). */
@Service
@RequiredArgsConstructor
public class AlimentacionServicio {

    private final PlanAlimenticioRepositorio planRepo;
    private final RegistroComidaRepositorio comidaRepo;
    private final ConsultaClienteServicio consulta;
    private final GeneradorPlanAlimenticio generador;
    private final Mapeador mapeador;

    @Transactional(readOnly = true)
    public AlimentacionDto alimentacion(Integer idCliente) {
        Cliente c = consulta.cliente(idCliente);
        LocalDate hoy = consulta.hoy();
        PlanAlimenticio plan = planActivo(idCliente);
        List<RegistroComida> registros = comidaRepo.findByClienteIdClienteAndFechaOrderByIdRegistroAsc(idCliente, hoy);
        ResumenNutricionalDto resumen = resumen(hoy, registros, plan);
        String sugerencia = plan == null
                ? "Genera tu plan nutricional con IA para recibir recomendaciones según tu objetivo."
                : generador.evaluar(Objetivo.desde(plan.getObjetivo()), plan.getCaloriasObjetivo(), plan.getContenido(), registros).mensaje();
        return new AlimentacionDto(mapeador.planAlimenticio(plan), resumen, sugerencia,
                registros.stream().map(mapeador::registroComida).toList(), GeneradorPlanAlimenticio.AVISO);
    }

    @Transactional
    public AlimentacionDto generarPlan(Integer idCliente, String objetivoTexto) {
        Cliente c = consulta.cliente(idCliente);
        LocalDate hoy = consulta.hoy();
        Objetivo objetivo = Objetivo.desde(objetivoTexto != null && !objetivoTexto.isBlank() ? objetivoTexto : c.getObjetivo());
        int sesiones = consulta.rutinaActiva(idCliente).map(Mapeador::sesionesPorSemana).orElse(3);
        var resultado = generador.generar(objetivo, consulta.perfilFisico(c), sesiones);
        for (PlanAlimenticio anterior : planRepo.findByClienteIdClienteAndActivoTrue(idCliente)) {
            anterior.setActivo(false);
            anterior.setFechaFinalizacion(hoy);
        }
        PlanAlimenticio p = new PlanAlimenticio();
        p.setCliente(c);
        p.setObjetivo(objetivo.getEtiqueta());
        p.setRitmo(resultado.ritmo());
        p.setCaloriasObjetivo(resultado.caloriasObjetivo());
        p.setContenido(resultado.contenido());
        p.setFechaCreacion(hoy);
        p.setActivo(true);
        planRepo.save(p);
        return alimentacion(idCliente);
    }

    /** Registra la comida y la compara con la meta del plan (¿cumple el objetivo?). */
    @Transactional
    public EvaluacionComidaDto registrarComida(Integer idCliente, RegistrarComidaRequest req) {
        Cliente c = consulta.cliente(idCliente);
        LocalDate fecha = req.fecha() != null ? req.fecha() : consulta.hoy();
        RegistroComida r = new RegistroComida();
        r.setCliente(c);
        r.setFecha(fecha);
        r.setTipoComida(req.tipoComida());
        r.setDescripcion(req.descripcion().trim());
        r.setCalorias(req.calorias());
        r.setProteinas(req.proteinas());
        r.setCarbohidratos(req.carbohidratos());
        r.setGrasas(req.grasas());
        r = comidaRepo.save(r);

        PlanAlimenticio plan = planActivo(idCliente);
        List<RegistroComida> delDia = comidaRepo.findByClienteIdClienteAndFechaOrderByIdRegistroAsc(idCliente, fecha);
        ResumenNutricionalDto resumen = resumen(fecha, delDia, plan);
        if (plan == null) {
            return new EvaluacionComidaDto(mapeador.registroComida(r), true,
                    "Comida registrada. Genera tu plan con IA para comparar tu consumo con una meta.", resumen);
        }
        var evaluacion = generador.evaluar(Objetivo.desde(plan.getObjetivo()), plan.getCaloriasObjetivo(), plan.getContenido(), delDia);
        return new EvaluacionComidaDto(mapeador.registroComida(r), evaluacion.cumpleObjetivo(), evaluacion.mensaje(), resumen);
    }

    @Transactional(readOnly = true)
    public AlimentacionClienteDto paraEntrenador(Integer idEntrenador, Integer idCliente) {
        consulta.clienteDeEntrenador(idCliente, idEntrenador);
        LocalDate hoy = consulta.hoy();
        List<RegistroComidaDto> semana = comidaRepo
                .findByClienteIdClienteAndFechaBetweenOrderByFechaDescIdRegistroDesc(idCliente, hoy.minusDays(6), hoy)
                .stream().map(mapeador::registroComida).toList();
        int dias = (int) semana.stream().map(RegistroComidaDto::fecha).distinct().count();
        return new AlimentacionClienteDto(alimentacion(idCliente), semana, dias);
    }

    public PlanAlimenticio planActivo(Integer idCliente) {
        return planRepo.findFirstByClienteIdClienteAndActivoTrueOrderByFechaCreacionDescIdPlanAlimenticioDesc(idCliente).orElse(null);
    }

    public static ResumenNutricionalDto resumen(LocalDate fecha, List<RegistroComida> registros, PlanAlimenticio plan) {
        int kcal = 0;
        double p = 0, c = 0, g = 0;
        for (RegistroComida r : registros) {
            kcal += r.getCalorias() == null ? 0 : r.getCalorias();
            p += v(r.getProteinas());
            c += v(r.getCarbohidratos());
            g += v(r.getGrasas());
        }
        double kcalMacros = p * 4 + c * 4 + g * 9;
        int pp = 0, pc = 0, pg = 0;
        if (kcalMacros > 0) {
            pp = (int) Math.round(p * 4 * 100 / kcalMacros);
            pg = (int) Math.round(g * 9 * 100 / kcalMacros);
            pc = 100 - pp - pg;
        }
        Integer objetivo = plan == null ? null : plan.getCaloriasObjetivo();
        int pctMeta = objetivo == null || objetivo == 0 ? 0 : (int) Math.round(kcal * 100.0 / objetivo);
        return new ResumenNutricionalDto(fecha, kcal, objetivo, pctMeta, Textos.redondearD(p, 1), Textos.redondearD(c, 1),
                Textos.redondearD(g, 1), pp, pc, pg, registros.size());
    }

    private static double v(BigDecimal b) {
        return b == null ? 0 : b.doubleValue();
    }
}
