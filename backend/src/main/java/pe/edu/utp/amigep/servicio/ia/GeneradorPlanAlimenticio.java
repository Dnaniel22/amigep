package pe.edu.utp.amigep.servicio.ia;

import org.springframework.stereotype.Component;
import pe.edu.utp.amigep.modelo.RegistroComida;
import pe.edu.utp.amigep.modelo.enums.Objetivo;
import pe.edu.utp.amigep.modelo.enums.TipoComida;

import java.math.BigDecimal;
import java.util.*;

/**
 * Módulo inteligente - recomendaciones alimenticias (RF-13, BPM Fig. 10).
 * Calcula la meta calórica (Mifflin-St Jeor promediado, sin sexo en el modelo de datos) y la ajusta
 * según el objetivo; reparte macros y comidas, y compara el consumo registrado con la meta.
 * Las recomendaciones son orientativas (sección 8.7).
 */
@Component
public class GeneradorPlanAlimenticio {

    public static final String AVISO = "Recomendaciones orientativas; no reemplazan la evaluación de un profesional.";

    /** Reparto de calorías del día por comida. */
    private static final Map<TipoComida, Double> REPARTO = new EnumMap<>(Map.of(
            TipoComida.DESAYUNO, 0.25, TipoComida.ALMUERZO, 0.35, TipoComida.CENA, 0.25, TipoComida.REFRIGERIO, 0.15));

    private static final Map<Objetivo, String[]> MENUS = Map.of(
            Objetivo.MASA_MUSCULAR, new String[]{"Avena · Claras de huevo · Frutas", "Pollo · Arroz integral · Verduras",
                    "Salmón · Brócoli · Quinua", "Yogur griego · Frutos secos"},
            Objetivo.PERDIDA_GRASA, new String[]{"Yogur griego · Frutos rojos · Chía", "Pescado a la plancha · Quinua · Ensalada",
                    "Pechuga de pavo · Verduras salteadas", "Manzana · Almendras"},
            Objetivo.FUERZA, new String[]{"Pan integral · Huevos · Palta", "Carne magra · Papa sancochada · Verduras",
                    "Atún · Camote · Ensalada", "Batido de proteína · Plátano"},
            Objetivo.RESISTENCIA, new String[]{"Avena · Plátano · Mantequilla de maní", "Pasta integral · Pollo · Verduras",
                    "Tortilla de huevo · Arroz · Palta", "Fruta · Galletas de avena"});

    public record ResultadoPlan(int caloriasObjetivo, String ritmo, Map<String, Object> contenido) {
    }

    public record Evaluacion(boolean cumpleObjetivo, String mensaje) {
    }

    public ResultadoPlan generar(Objetivo objetivo, PerfilFisico perfil, int sesionesPorSemana) {
        double peso = perfil.pesoKg();
        double alturaCm = perfil.alturaM() * 100;
        double tmb = 10 * peso + 6.25 * alturaCm - 5 * perfil.edadAnios() - 78;
        double factor = sesionesPorSemana <= 3 ? 1.375 : sesionesPorSemana <= 5 ? 1.55 : 1.725;
        int ajuste;
        double metaSemanal;
        double proteinaPorKg;
        switch (objetivo) {
            case MASA_MUSCULAR -> { ajuste = 300; metaSemanal = 0.3; proteinaPorKg = 2.0; }
            case PERDIDA_GRASA -> { ajuste = -500; metaSemanal = -0.5; proteinaPorKg = 2.2; }
            case FUERZA -> { ajuste = 200; metaSemanal = 0.2; proteinaPorKg = 1.8; }
            default -> { ajuste = 0; metaSemanal = 0.0; proteinaPorKg = 1.6; }
        }
        int calorias = (int) (Math.round((tmb * factor + ajuste) / 50.0) * 50);
        calorias = Math.max(1200, calorias);

        double proteinas = proteinaPorKg * peso;
        double grasas = calorias * 0.25 / 9;
        double carbohidratos = Math.max(0, (calorias - proteinas * 4 - grasas * 9) / 4);

        List<Map<String, Object>> comidas = new ArrayList<>();
        String[] menu = MENUS.get(objetivo);
        TipoComida[] tipos = {TipoComida.DESAYUNO, TipoComida.ALMUERZO, TipoComida.CENA, TipoComida.REFRIGERIO};
        for (int i = 0; i < tipos.length; i++) {
            double parte = REPARTO.get(tipos[i]);
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("tipo", tipos[i].name());
            c.put("nombre", nombreComida(tipos[i]));
            c.put("descripcion", menu[i]);
            c.put("calorias", (int) Math.round(calorias * parte / 10.0) * 10);
            c.put("proteinas", Math.round(proteinas * parte));
            c.put("carbohidratos", Math.round(carbohidratos * parte));
            c.put("grasas", Math.round(grasas * parte));
            comidas.add(c);
        }

        Map<String, Object> macros = new LinkedHashMap<>();
        macros.put("proteinas", Math.round(proteinas));
        macros.put("carbohidratos", Math.round(carbohidratos));
        macros.put("grasas", Math.round(grasas));
        macros.put("pctProteinas", (int) Math.round(proteinas * 4 * 100 / calorias));
        macros.put("pctCarbohidratos", (int) Math.round(carbohidratos * 4 * 100 / calorias));
        macros.put("pctGrasas", 25);

        Map<String, Object> contenido = new LinkedHashMap<>();
        contenido.put("objetivoEtiqueta", etiquetaObjetivo(objetivo));
        contenido.put("comidas", comidas);
        contenido.put("macros", macros);
        contenido.put("metaSemanalKg", metaSemanal);
        contenido.put("aguaLitros", Math.round(peso * 0.035 * 10) / 10.0);
        contenido.put("recomendaciones", recomendaciones(objetivo, peso));
        contenido.put("aviso", AVISO);
        contenido.put("generadoPor", "Módulo inteligente AMIGEP v1 (Mifflin-St Jeor + reglas por objetivo)");

        String ritmo = sesionesPorSemana + " sesiones/semana · meta " + (metaSemanal > 0 ? "+" : "") + metaSemanal + " kg/semana";
        return new ResultadoPlan(calorias, ritmo, contenido);
    }

    /**
     * Compara lo consumido hoy con la meta acumulada de las comidas ya registradas (±15 %).
     */
    public Evaluacion evaluar(Objetivo objetivo, int caloriasObjetivo, Map<String, Object> contenido,
                              List<RegistroComida> registrosDelDia) {
        if (registrosDelDia.isEmpty()) {
            return new Evaluacion(true, sugerenciaInicial(objetivo));
        }
        Set<TipoComida> tipos = EnumSet.noneOf(TipoComida.class);
        double kcal = 0, prot = 0;
        for (RegistroComida r : registrosDelDia) {
            tipos.add(r.getTipoComida());
            kcal += r.getCalorias() == null ? 0 : r.getCalorias();
            prot += valor(r.getProteinas());
        }
        double fraccion = tipos.stream().mapToDouble(REPARTO::get).sum();
        double metaKcal = caloriasObjetivo * fraccion;
        double metaProt = proteinaObjetivo(contenido) * fraccion;

        double desvioKcal = metaKcal == 0 ? 0 : (kcal - metaKcal) / metaKcal;
        boolean proteinaBaja = metaProt > 0 && prot < metaProt * 0.8;

        if (desvioKcal > 0.15) {
            String ajuste = objetivo == Objetivo.PERDIDA_GRASA
                    ? "reduce los carbohidratos de tu próxima comida y prioriza verduras"
                    : "elige una próxima comida más ligera para mantenerte en tu meta";
            return new Evaluacion(false, String.format(Locale.US,
                    "Alerta: vas %.0f%% por encima de tu meta acumulada (%d de %d kcal). Sugerencia: %s.",
                    desvioKcal * 100, Math.round(kcal), Math.round(metaKcal), ajuste));
        }
        if (desvioKcal < -0.15 && objetivo != Objetivo.PERDIDA_GRASA) {
            return new Evaluacion(false, String.format(Locale.US,
                    "Alerta: llevas %d de %d kcal previstas. Agrega una porción de carbohidratos complejos y proteína para no frenar tu avance.",
                    Math.round(kcal), Math.round(metaKcal)));
        }
        if (proteinaBaja) {
            return new Evaluacion(false, String.format(Locale.US,
                    "Sugerencia IA: incrementa la proteína en tu próxima comida (llevas %d g de %d g). Opciones: huevo, pollo, atún o yogur griego.",
                    Math.round(prot), Math.round(metaProt)));
        }
        return new Evaluacion(true, "¡Vas bien! Tu consumo está dentro de la meta de hoy. Mantén la hidratación ("
                + contenido.getOrDefault("aguaLitros", 2.5) + " L) y respeta tus horarios de comida.");
    }

    private String sugerenciaInicial(Objetivo objetivo) {
        return switch (objetivo) {
            case MASA_MUSCULAR -> "Sugerencia IA: incrementa la proteína en el desayuno para optimizar tu recuperación muscular tras el entrenamiento.";
            case PERDIDA_GRASA -> "Sugerencia IA: empieza el día con proteína y fibra; te ayudará a controlar el apetito.";
            case FUERZA -> "Sugerencia IA: consume carbohidratos 1-2 horas antes de entrenar para rendir en tus series pesadas.";
            case RESISTENCIA -> "Sugerencia IA: prioriza carbohidratos complejos e hidrátate bien antes y después del cardio.";
        };
    }

    private List<String> recomendaciones(Objetivo objetivo, double peso) {
        List<String> r = new ArrayList<>();
        r.add(String.format(Locale.US, "Bebe al menos %.1f L de agua al día.", peso * 0.035));
        switch (objetivo) {
            case MASA_MUSCULAR -> {
                r.add("Incluye proteína en cada comida y un refrigerio después de entrenar.");
                r.add("No te saltes comidas: el superávit calórico es necesario para ganar masa.");
            }
            case PERDIDA_GRASA -> {
                r.add("Llena la mitad del plato con verduras y evita bebidas azucaradas.");
                r.add("Mantén la proteína alta para conservar masa muscular.");
            }
            case FUERZA -> {
                r.add("Distribuye los carbohidratos alrededor del entrenamiento.");
                r.add("Duerme 7-8 horas para recuperar el sistema nervioso.");
            }
            case RESISTENCIA -> {
                r.add("Los carbohidratos complejos son tu principal combustible.");
                r.add("Repón electrolitos en sesiones de cardio largas.");
            }
        }
        return r;
    }

    private static double proteinaObjetivo(Map<String, Object> contenido) {
        if (contenido != null && contenido.get("macros") instanceof Map<?, ?> m && m.get("proteinas") instanceof Number n) {
            return n.doubleValue();
        }
        return 0;
    }

    private static double valor(BigDecimal b) {
        return b == null ? 0 : b.doubleValue();
    }

    private static String nombreComida(TipoComida tipo) {
        return switch (tipo) {
            case DESAYUNO -> "Desayuno";
            case ALMUERZO -> "Almuerzo";
            case CENA -> "Cena";
            case REFRIGERIO -> "Refrigerio";
        };
    }

    public static String etiquetaObjetivo(Objetivo objetivo) {
        return switch (objetivo) {
            case MASA_MUSCULAR -> "Ganar masa muscular";
            case PERDIDA_GRASA -> "Perder grasa";
            case FUERZA -> "Ganar fuerza";
            case RESISTENCIA -> "Mejorar resistencia";
        };
    }
}
