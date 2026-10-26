package pe.edu.utp.amigep;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.utp.amigep.modelo.RegistroComida;
import pe.edu.utp.amigep.modelo.enums.*;
import pe.edu.utp.amigep.servicio.ia.*;
import pe.edu.utp.amigep.servicio.ia.HistorialCliente.PeriodoSuscripcion;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del módulo inteligente (RF-11, RF-13, RF-18). No requieren base de datos. */
class ModuloInteligenteTest {

    private final PerfilFisico perfil = new PerfilFisico(new BigDecimal("90"), new BigDecimal("1.75"), 22);

    @Test
    @SuppressWarnings("unchecked")
    void generaRutinaSegunObjetivoNivelYRitmo() {
        Map<String, Object> r = new GeneradorRutinas().generar(Objetivo.MASA_MUSCULAR, Nivel.INTERMEDIO, 4, perfil);
        List<Map<String, Object>> dias = (List<Map<String, Object>>) r.get("dias");
        assertEquals("Hipertrofia", r.get("nombre"));
        assertEquals(4, dias.size(), "un día por sesión semanal");
        for (Map<String, Object> d : dias) {
            List<Map<String, Object>> ej = (List<Map<String, Object>>) d.get("ejercicios");
            assertEquals(5, ej.size(), "nivel intermedio = 5 ejercicios");
            assertEquals(ej.size(), ej.stream().map(e -> e.get("nombre")).distinct().count(), "sin ejercicios repetidos en el día");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void perdidaDeGrasaIncluyeCardioDeBajoImpactoConImcAlto() {
        PerfilFisico obeso = new PerfilFisico(new BigDecimal("110"), new BigDecimal("1.70"), 40);
        Map<String, Object> r = new GeneradorRutinas().generar(Objetivo.PERDIDA_GRASA, Nivel.PRINCIPIANTE, 3, obeso);
        List<Map<String, Object>> dias = (List<Map<String, Object>>) r.get("dias");
        List<Map<String, Object>> ej = (List<Map<String, Object>>) dias.get(0).get("ejercicios");
        String ultimo = (String) ej.get(ej.size() - 1).get("nombre");
        assertTrue(List.of("Bicicleta estática", "Elíptica", "Caminadora inclinada").contains(ultimo));
    }

    @Test
    void planAlimenticioAjustaCaloriasAlObjetivo() {
        GeneradorPlanAlimenticio g = new GeneradorPlanAlimenticio();
        int masa = g.generar(Objetivo.MASA_MUSCULAR, perfil, 4).caloriasObjetivo();
        int grasa = g.generar(Objetivo.PERDIDA_GRASA, perfil, 4).caloriasObjetivo();
        assertTrue(masa > grasa, "superávit para masa muscular y déficit para pérdida de grasa");
        assertEquals(800, masa - grasa, 50);
        assertEquals(0, masa % 50);
    }

    @Test
    void evaluacionDetectaExcesoDeCalorias() {
        GeneradorPlanAlimenticio g = new GeneradorPlanAlimenticio();
        var plan = g.generar(Objetivo.PERDIDA_GRASA, perfil, 3);
        RegistroComida desayuno = new RegistroComida();
        desayuno.setTipoComida(TipoComida.DESAYUNO);
        desayuno.setCalorias(plan.caloriasObjetivo()); // todo el día en el desayuno
        desayuno.setProteinas(new BigDecimal("30"));
        var ev = g.evaluar(Objetivo.PERDIDA_GRASA, plan.caloriasObjetivo(), plan.contenido(), List.of(desayuno));
        assertFalse(ev.cumpleObjetivo());
        assertTrue(ev.mensaje().startsWith("Alerta"));
    }

    @Test
    void caracteristicasReflejanInasistenciaYPlanVencido() {
        LocalDate hoy = LocalDate.of(2026, 10, 24);
        List<LocalDateTime> accesos = new ArrayList<>();
        for (int i = 12; i < 60; i += 2) accesos.add(hoy.minusDays(i).atTime(7, 0));
        HistorialCliente h = new HistorialCliente(1, hoy.minusDays(120), accesos,
                List.of(new PeriodoSuscripcion(hoy.minusDays(33), hoy.minusDays(3), true)), List.of(), List.of());
        var c = h.caracteristicas(hoy);
        assertEquals(12 / 30.0, c.x()[0], 1e-9);
        assertEquals(1.0, c.x()[2], "membresía vencida");
        assertTrue(c.factores().contains("12 días sin asistir"));
        assertTrue(c.factores().contains("plan vencido"));
    }

    @Test
    void modeloClasificaRiesgoYSeEntrena(@TempDir Path tmp) {
        ModeloAbandono m = new ModeloAbandono(tmp.resolve("modelo.json").toString(), new ObjectMapper());
        double[] fiel = {0.03, 0, 0, 0, 0, 0, 0};
        double[] enRiesgo = {0.4, 0.6, 1, 0, 1, 0, 1};
        assertEquals(EstadoRiesgo.BAJO, ModeloAbandono.clasificar(m.predecir(fiel)));
        assertEquals(EstadoRiesgo.ALTO, ModeloAbandono.clasificar(m.predecir(enRiesgo)));

        Random r = new Random(1);
        List<double[]> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            boolean abandona = i % 5 == 0;
            xs.add(new double[]{abandona ? 0.5 + r.nextDouble() * 0.5 : r.nextDouble() * 0.2, abandona ? r.nextDouble() : 0,
                    abandona ? 1 : 0, 0, abandona ? 1 : r.nextInt(2), 0, abandona ? 1 : 0});
            ys.add(abandona ? 1 : 0);
        }
        var res = m.entrenar(xs, ys);
        assertTrue(res.entrenado());
        assertTrue(res.exactitud() >= 0.9);
        assertTrue(m.predecir(enRiesgo) > m.predecir(fiel));
        // Se persiste y se vuelve a cargar
        ModeloAbandono recargado = new ModeloAbandono(tmp.resolve("modelo.json").toString(), new ObjectMapper());
        assertTrue(recargado.isEntrenado());
        assertEquals(m.predecir(enRiesgo), recargado.predecir(enRiesgo), 1e-9);
    }

    @Test
    void objetivoAceptaEtiquetasLibres() {
        assertEquals(Objetivo.MASA_MUSCULAR, Objetivo.desde("Ganar masa muscular"));
        assertEquals(Objetivo.PERDIDA_GRASA, Objetivo.desde("Pérdida de grasa"));
        assertEquals(Objetivo.FUERZA, Objetivo.desde("FUERZA"));
        assertEquals(Objetivo.RESISTENCIA, Objetivo.desde("resistencia"));
    }
}
