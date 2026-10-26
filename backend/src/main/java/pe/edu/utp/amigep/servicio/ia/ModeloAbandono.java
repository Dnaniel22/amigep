package pe.edu.utp.amigep.servicio.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.utp.amigep.modelo.enums.EstadoRiesgo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Módulo inteligente - predicción de abandono (RF-18).
 * Modelo de clasificación por regresión logística: p = 1 / (1 + e^-(b + w·x)).
 * <ul>
 *   <li>Arranca con pesos iniciales definidos con criterio experto (útiles mientras no hay historial).</li>
 *   <li>{@link #entrenar} ajusta los pesos con descenso de gradiente sobre el historial de asistencias,
 *       pagos y suscripciones, y los guarda en disco para los siguientes reinicios.</li>
 * </ul>
 */
@Slf4j
@Component
public class ModeloAbandono {

    public static final double UMBRAL_ALTO = 0.70;
    public static final double UMBRAL_MEDIO = 0.40;

    /** Pesos iniciales en el orden de {@link HistorialCliente#NOMBRES}. */
    private static final double SESGO_INICIAL = -2.2;
    private static final double[] PESOS_INICIALES = {2.5, 1.5, 2.0, 1.2, 0.9, 1.0, 0.6};

    private final Path archivo;
    private final ObjectMapper mapper;

    private double sesgo = SESGO_INICIAL;
    private double[] pesos = PESOS_INICIALES.clone();
    private boolean entrenado;
    private int muestras;
    private Double exactitud;
    private Double sensibilidad;
    private String fechaEntrenamiento;

    /**
     * @param exactitud    aciertos / total en el 20 % de validación
     * @param sensibilidad abandonos detectados / abandonos reales en validación (lo más importante para retener)
     */
    public record ResultadoEntrenamiento(boolean entrenado, int muestras, int positivos, Double exactitud,
                                         Double sensibilidad, String mensaje) {
    }

    public ModeloAbandono(@Value("${amigep.ia.modelo-abandono}") String ruta, ObjectMapper mapper) {
        this.archivo = Path.of(ruta);
        this.mapper = mapper;
        cargar();
    }

    public synchronized double predecir(double[] x) {
        double z = sesgo;
        for (int i = 0; i < pesos.length; i++) z += pesos[i] * x[i];
        return 1 / (1 + Math.exp(-z));
    }

    public static EstadoRiesgo clasificar(double probabilidad) {
        if (probabilidad >= UMBRAL_ALTO) return EstadoRiesgo.ALTO;
        if (probabilidad >= UMBRAL_MEDIO) return EstadoRiesgo.MEDIO;
        return EstadoRiesgo.BAJO;
    }

    /**
     * Entrena con los ejemplos dados (80 % entrenamiento / 20 % validación para medir exactitud y luego
     * ajuste final con todos). Requiere al menos 30 ejemplos y casos de ambas clases.
     */
    public synchronized ResultadoEntrenamiento entrenar(List<double[]> xs, List<Integer> ys) {
        int positivos = (int) ys.stream().filter(y -> y == 1).count();
        if (xs.size() < 30 || positivos < 3 || positivos == xs.size()) {
            return new ResultadoEntrenamiento(false, xs.size(), positivos, exactitud, sensibilidad,
                    "Historial insuficiente para entrenar (se necesitan 30 ejemplos con casos de abandono y de permanencia). Se mantienen los pesos actuales.");
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < xs.size(); i++) indices.add(i);
        Collections.shuffle(indices, new Random(42));
        int corte = (int) (indices.size() * 0.8);
        List<Integer> entrenamiento = indices.subList(0, corte);
        List<Integer> validacion = indices.subList(corte, indices.size());

        double[] parcial = ajustar(xs, ys, entrenamiento);
        int aciertos = 0, abandonosValidacion = 0, detectados = 0;
        for (int i : validacion) {
            int prediccion = sigmoide(parcial, xs.get(i)) >= 0.5 ? 1 : 0;
            if (prediccion == ys.get(i)) aciertos++;
            if (ys.get(i) == 1) {
                abandonosValidacion++;
                if (prediccion == 1) detectados++;
            }
        }
        double[] finales = ajustar(xs, ys, indices);
        this.sesgo = finales[0];
        this.pesos = Arrays.copyOfRange(finales, 1, finales.length);
        this.entrenado = true;
        this.muestras = xs.size();
        this.exactitud = validacion.isEmpty() ? null : Math.round(aciertos * 1000.0 / validacion.size()) / 1000.0;
        this.sensibilidad = abandonosValidacion == 0 ? null : Math.round(detectados * 1000.0 / abandonosValidacion) / 1000.0;
        this.fechaEntrenamiento = LocalDateTime.now().withNano(0).toString();
        guardar();
        log.info("Modelo de abandono entrenado: {} ejemplos, {} abandonos, exactitud {}, sensibilidad {}",
                muestras, positivos, exactitud, sensibilidad);
        return new ResultadoEntrenamiento(true, muestras, positivos, exactitud, sensibilidad,
                "Modelo entrenado con el historial del gimnasio.");
    }

    /**
     * Descenso de gradiente con regularización L2 y clases balanceadas: los abandonos son pocos, así que
     * cada caso de abandono pesa más para que el modelo no aprenda a decir siempre "no abandona".
     * Devuelve [sesgo, pesos...].
     */
    private double[] ajustar(List<double[]> xs, List<Integer> ys, List<Integer> indices) {
        int n = PESOS_INICIALES.length;
        long pos = indices.stream().filter(i -> ys.get(i) == 1).count();
        double pesoPositivo = pos == 0 ? 1 : indices.size() / (2.0 * pos);
        double pesoNegativo = pos == indices.size() ? 1 : indices.size() / (2.0 * (indices.size() - pos));
        double[] w = new double[n + 1];
        w[0] = SESGO_INICIAL;
        System.arraycopy(PESOS_INICIALES, 0, w, 1, n);
        double tasa = 0.5, lambda = 0.01;
        for (int iter = 0; iter < 3000; iter++) {
            double[] grad = new double[n + 1];
            for (int i : indices) {
                double error = (sigmoide(w, xs.get(i)) - ys.get(i)) * (ys.get(i) == 1 ? pesoPositivo : pesoNegativo);
                grad[0] += error;
                for (int j = 0; j < n; j++) grad[j + 1] += error * xs.get(i)[j];
            }
            w[0] -= tasa * grad[0] / indices.size();
            for (int j = 1; j <= n; j++) w[j] -= tasa * (grad[j] / indices.size() + lambda * w[j]);
        }
        return w;
    }

    private static double sigmoide(double[] w, double[] x) {
        double z = w[0];
        for (int j = 0; j < x.length; j++) z += w[j + 1] * x[j];
        return 1 / (1 + Math.exp(-z));
    }

    public synchronized Map<String, Double> pesosActuales() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("sesgo", Math.round(sesgo * 1000) / 1000.0);
        for (int i = 0; i < pesos.length; i++) m.put(HistorialCliente.NOMBRES[i], Math.round(pesos[i] * 1000) / 1000.0);
        return m;
    }

    public synchronized boolean isEntrenado() {
        return entrenado;
    }

    public synchronized int getMuestras() {
        return muestras;
    }

    public synchronized Double getExactitud() {
        return exactitud;
    }

    public synchronized Double getSensibilidad() {
        return sensibilidad;
    }

    public synchronized String getFechaEntrenamiento() {
        return fechaEntrenamiento;
    }

    private void guardar() {
        try {
            if (archivo.getParent() != null) Files.createDirectories(archivo.getParent());
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("sesgo", sesgo);
            datos.put("pesos", pesos);
            datos.put("muestras", muestras);
            datos.put("exactitud", exactitud);
            datos.put("sensibilidad", sensibilidad);
            datos.put("fechaEntrenamiento", fechaEntrenamiento);
            mapper.writerWithDefaultPrettyPrinter().writeValue(archivo.toFile(), datos);
        } catch (IOException e) {
            log.warn("No se pudo guardar el modelo en {}: {}", archivo, e.getMessage());
        }
    }

    private void cargar() {
        if (!Files.exists(archivo)) return;
        try {
            var nodo = mapper.readTree(archivo.toFile());
            double[] leidos = mapper.convertValue(nodo.get("pesos"), double[].class);
            if (leidos == null || leidos.length != PESOS_INICIALES.length) return;
            this.sesgo = nodo.get("sesgo").asDouble();
            this.pesos = leidos;
            this.muestras = nodo.path("muestras").asInt();
            this.exactitud = nodo.hasNonNull("exactitud") ? nodo.get("exactitud").asDouble() : null;
            this.sensibilidad = nodo.hasNonNull("sensibilidad") ? nodo.get("sensibilidad").asDouble() : null;
            this.fechaEntrenamiento = nodo.path("fechaEntrenamiento").asText(null);
            this.entrenado = true;
            log.info("Modelo de abandono cargado desde {}", archivo);
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo leer el modelo {}: se usan los pesos iniciales", archivo);
        }
    }
}
