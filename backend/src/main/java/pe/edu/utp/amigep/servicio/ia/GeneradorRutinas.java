package pe.edu.utp.amigep.servicio.ia;

import org.springframework.stereotype.Component;
import pe.edu.utp.amigep.modelo.enums.Nivel;
import pe.edu.utp.amigep.modelo.enums.Objetivo;

import java.util.*;

/**
 * Módulo inteligente - generación de rutinas personalizadas (RF-11, BPM Fig. 9).
 * Reglas parametrizadas por objetivo, nivel y ritmo (sesiones por semana), ajustadas con el perfil físico.
 * El resultado se guarda tal cual en Rutina.contenido (JSONB).
 */
@Component
public class GeneradorRutinas {

    /** Parámetros de carga por objetivo. */
    private record Carga(int series, String repeticiones, int descansoSeg, int cardioMin, String nombre) {
    }

    private static final Map<Objetivo, Carga> CARGAS = Map.of(
            Objetivo.MASA_MUSCULAR, new Carga(4, "10", 90, 0, "Hipertrofia"),
            Objetivo.PERDIDA_GRASA, new Carga(3, "15", 45, 20, "Pérdida de grasa"),
            Objetivo.FUERZA, new Carga(5, "5", 150, 0, "Fuerza"),
            Objetivo.RESISTENCIA, new Carga(3, "20", 30, 30, "Resistencia"));

    /** Catálogo por grupo muscular. Los primeros de cada lista son los básicos aptos para principiantes. */
    private static final Map<String, List<String>> CATALOGO = Map.of(
            "pecho", List.of("Press banca", "Press inclinado con mancuernas", "Aperturas con mancuernas", "Fondos en paralelas"),
            "espalda", List.of("Jalón al pecho", "Remo con barra", "Remo con mancuerna", "Dominadas"),
            "pierna", List.of("Sentadilla", "Prensa de piernas", "Peso muerto rumano", "Zancadas", "Curl femoral", "Elevación de talones"),
            "hombro", List.of("Press militar", "Elevaciones laterales", "Face pull"),
            "biceps", List.of("Curl bíceps", "Curl martillo"),
            "triceps", List.of("Extensión de tríceps en polea", "Press francés"),
            "core", List.of("Plancha", "Crunch abdominal", "Elevación de piernas"),
            "fuerza", List.of("Sentadilla", "Press banca", "Peso muerto", "Press militar", "Remo con barra"));

    private static final List<String> CARDIO_ALTO_IMPACTO = List.of("Saltar la cuerda", "Burpees", "HIIT en bicicleta");
    private static final List<String> CARDIO_BAJO_IMPACTO = List.of("Bicicleta estática", "Elíptica", "Caminadora inclinada");

    /** Distribución semanal de grupos musculares según las sesiones por semana. */
    private static List<String[]> division(Objetivo objetivo, int sesiones) {
        if (objetivo == Objetivo.FUERZA) {
            List<String[]> dias = new ArrayList<>();
            String[][] base = {
                    {"Sentadilla y empuje", "Pierna · Pecho", "fuerza:0", "fuerza:1", "triceps", "core"},
                    {"Peso muerto y tirón", "Espalda · Femoral", "fuerza:2", "fuerza:4", "biceps", "core"},
                    {"Press y sentadilla", "Hombro · Pierna", "fuerza:3", "fuerza:0", "pecho", "core"},
                    {"Tirón y accesorios", "Espalda · Brazos", "fuerza:4", "espalda", "biceps", "core"},
                    {"Full body pesado", "Cuerpo completo", "fuerza:0", "fuerza:1", "fuerza:2", "core"},
                    {"Técnica y movilidad", "Cuerpo completo", "pierna", "pecho", "espalda", "core"}};
            for (int i = 0; i < sesiones; i++) dias.add(base[i % base.length]);
            return dias;
        }
        String[][] plantilla = switch (sesiones) {
            case 2 -> new String[][]{
                    {"Full body A", "Pierna · Pecho · Espalda", "pierna", "pecho", "espalda", "hombro", "core", "pierna"},
                    {"Full body B", "Pierna · Espalda · Brazos", "pierna", "espalda", "pecho", "biceps", "triceps", "core"}};
            case 3 -> new String[][]{
                    {"Empuje", "Pecho · Hombro · Tríceps", "pecho", "pecho", "hombro", "hombro", "triceps", "core"},
                    {"Tirón", "Espalda · Bíceps", "espalda", "espalda", "espalda", "biceps", "biceps", "core"},
                    {"Pierna", "Cuádriceps · Femoral · Glúteo", "pierna", "pierna", "pierna", "pierna", "pierna", "core"}};
            case 4 -> new String[][]{
                    {"Torso A", "Pecho · Espalda · Hombro", "pecho", "espalda", "hombro", "pecho", "espalda", "triceps"},
                    {"Pierna A", "Cuádriceps · Glúteo", "pierna", "pierna", "pierna", "pierna", "core", "core"},
                    {"Torso B", "Espalda · Pecho · Brazos", "espalda", "pecho", "espalda", "hombro", "biceps", "triceps"},
                    {"Pierna B", "Femoral · Pantorrilla", "pierna", "pierna", "pierna", "pierna", "core", "core"}};
            default -> new String[][]{
                    {"Pecho y tríceps", "Pecho · Tríceps", "pecho", "pecho", "pecho", "triceps", "triceps", "core"},
                    {"Espalda y bíceps", "Espalda · Bíceps", "espalda", "espalda", "espalda", "biceps", "biceps", "core"},
                    {"Pierna", "Cuádriceps · Femoral · Glúteo", "pierna", "pierna", "pierna", "pierna", "pierna", "core"},
                    {"Hombro y core", "Hombro · Abdomen", "hombro", "hombro", "hombro", "core", "core", "core"},
                    {"Full body", "Cuerpo completo", "pierna", "pecho", "espalda", "hombro", "biceps", "triceps"},
                    {"Pierna y glúteo", "Glúteo · Femoral", "pierna", "pierna", "pierna", "pierna", "core", "core"}};
        };
        List<String[]> dias = new ArrayList<>();
        for (int i = 0; i < sesiones; i++) dias.add(plantilla[i % plantilla.length]);
        return dias;
    }

    public Map<String, Object> generar(Objetivo objetivo, Nivel nivel, int sesionesPorSemana, PerfilFisico perfil) {
        Carga carga = CARGAS.get(objetivo);
        int sesiones = Math.max(2, Math.min(6, sesionesPorSemana));
        int ejerciciosPorDia = switch (nivel) {
            case PRINCIPIANTE -> 4;
            case INTERMEDIO -> 5;
            case AVANZADO -> 6;
        };
        int ajusteSeries = switch (nivel) {
            case PRINCIPIANTE -> -1;
            case INTERMEDIO -> 0;
            case AVANZADO -> 1;
        };
        // Los principiantes usan solo la primera mitad del catálogo (ejercicios guiados / básicos).
        boolean soloBasicos = nivel == Nivel.PRINCIPIANTE;
        Double imc = perfil.imc();
        boolean bajoImpacto = (imc != null && imc >= 30) || perfil.edadAnios() >= 50;

        List<Map<String, Object>> dias = new ArrayList<>();
        int numeroDia = 1;
        for (String[] plantilla : division(objetivo, sesiones)) {
            Map<String, Integer> usados = new HashMap<>();
            Set<String> nombresDia = new LinkedHashSet<>();
            List<Map<String, Object>> ejercicios = new ArrayList<>();
            for (int i = 2; i < plantilla.length && ejercicios.size() < ejerciciosPorDia; i++) {
                String grupo = plantilla[i];
                String nombre;
                if (grupo.contains(":")) {
                    String[] partes = grupo.split(":");
                    nombre = CATALOGO.get(partes[0]).get(Integer.parseInt(partes[1]));
                } else {
                    List<String> opciones = CATALOGO.get(grupo);
                    int limite = soloBasicos ? Math.max(2, (opciones.size() + 1) / 2) : opciones.size();
                    int indice = usados.merge(grupo, 1, Integer::sum) - 1 + (numeroDia % 2 == 0 && !soloBasicos ? 1 : 0);
                    nombre = opciones.get(indice % limite);
                }
                if (!nombresDia.add(nombre)) continue;
                boolean esCore = CATALOGO.get("core").contains(nombre);
                boolean esBasicoFuerza = objetivo == Objetivo.FUERZA && CATALOGO.get("fuerza").contains(nombre);
                int series = Math.max(2, (esBasicoFuerza ? carga.series() : Math.min(carga.series(), 4)) + ajusteSeries);
                String reps = esCore ? (nombre.equals("Plancha") ? "40 s" : "15")
                        : esBasicoFuerza ? carga.repeticiones()
                        : objetivo == Objetivo.FUERZA ? "8" : carga.repeticiones();
                Map<String, Object> ej = new LinkedHashMap<>();
                ej.put("nombre", nombre);
                ej.put("series", series);
                ej.put("repeticiones", reps);
                ej.put("descansoSeg", esCore ? 45 : carga.descansoSeg());
                ejercicios.add(ej);
            }
            if (carga.cardioMin() > 0) {
                List<String> cardio = bajoImpacto ? CARDIO_BAJO_IMPACTO : CARDIO_ALTO_IMPACTO;
                Map<String, Object> ej = new LinkedHashMap<>();
                ej.put("nombre", cardio.get((numeroDia - 1) % cardio.size()));
                ej.put("series", 1);
                ej.put("repeticiones", carga.cardioMin() + (nivel == Nivel.AVANZADO ? 10 : 0) + " min");
                ej.put("descansoSeg", 0);
                ejercicios.add(ej);
            }
            Map<String, Object> dia = new LinkedHashMap<>();
            dia.put("dia", numeroDia++);
            dia.put("nombre", plantilla[0]);
            dia.put("enfoque", plantilla[1]);
            dia.put("ejercicios", ejercicios);
            dias.add(dia);
        }

        int duracion = 10 + ejerciciosPorDia * (objetivo == Objetivo.FUERZA ? 12 : 8) + carga.cardioMin();
        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("nombre", carga.nombre());
        contenido.put("semanas", nivel == Nivel.PRINCIPIANTE ? 6 : 8);
        contenido.put("sesionesPorSemana", sesiones);
        contenido.put("duracionMinutos", Math.round(duracion / 5.0) * 5);
        contenido.put("dias", dias);
        contenido.put("notas", notas(objetivo, nivel, bajoImpacto, imc));
        contenido.put("generadoPor", "Módulo inteligente AMIGEP v1 (reglas por objetivo, nivel y ritmo)");
        return contenido;
    }

    private List<String> notas(Objetivo objetivo, Nivel nivel, boolean bajoImpacto, Double imc) {
        List<String> notas = new ArrayList<>();
        notas.add("Calienta 8-10 minutos antes de cada sesión.");
        switch (objetivo) {
            case MASA_MUSCULAR -> notas.add("Aumenta el peso cuando completes todas las series con buena técnica.");
            case PERDIDA_GRASA -> notas.add("Mantén descansos cortos y completa el cardio al final de la sesión.");
            case FUERZA -> notas.add("Prioriza la técnica en los básicos; descansa lo suficiente entre series pesadas.");
            case RESISTENCIA -> notas.add("Trabaja a ritmo constante; puedes hacer los ejercicios en circuito.");
        }
        if (nivel == Nivel.PRINCIPIANTE) notas.add("Pide a tu entrenador revisar tu técnica en las primeras semanas.");
        if (bajoImpacto) notas.add("Se eligió cardio de bajo impacto para cuidar tus articulaciones.");
        if (imc != null && imc < 18.5) notas.add("Tu IMC es bajo: acompaña la rutina con una alimentación suficiente.");
        return notas;
    }
}
