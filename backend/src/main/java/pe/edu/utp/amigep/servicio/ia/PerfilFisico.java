package pe.edu.utp.amigep.servicio.ia;

import java.math.BigDecimal;

/**
 * Datos del cliente que usa el módulo inteligente (RF-10).
 *
 * @param peso   kg (última medición de Progreso)
 * @param altura metros (Cliente.altura)
 * @param edad   años (desde Cliente.fechaNacimiento)
 */
public record PerfilFisico(BigDecimal peso, BigDecimal altura, Integer edad) {

    public double pesoKg() {
        if (peso != null) return peso.doubleValue();
        // Sin peso registrado se estima uno de referencia con IMC 22.
        double h = alturaM();
        return 22 * h * h;
    }

    public double alturaM() {
        return altura != null ? altura.doubleValue() : 1.70;
    }

    public int edadAnios() {
        return edad != null ? edad : 25;
    }

    public Double imc() {
        if (peso == null || altura == null) return null;
        double h = altura.doubleValue();
        return peso.doubleValue() / (h * h);
    }
}
