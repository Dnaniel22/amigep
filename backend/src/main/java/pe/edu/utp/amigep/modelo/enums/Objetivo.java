package pe.edu.utp.amigep.modelo.enums;

import java.text.Normalizer;

/**
 * Objetivos de entrenamiento que ofrece la app (pantalla C3).
 * En la BD se guarda la etiqueta legible (Cliente.objetivo, Rutina.objetivo).
 */
public enum Objetivo {
    MASA_MUSCULAR("Masa muscular"),
    PERDIDA_GRASA("Pérdida de grasa"),
    FUERZA("Fuerza"),
    RESISTENCIA("Resistencia");

    private final String etiqueta;

    Objetivo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Acepta el nombre del enum o la etiqueta, con o sin tildes ("Ganar masa muscular" también). */
    public static Objetivo desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return MASA_MUSCULAR;
        }
        String t = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase().replace(' ', '_');
        if (t.contains("GRASA") || t.contains("PESO")) return PERDIDA_GRASA;
        if (t.contains("FUERZA")) return FUERZA;
        if (t.contains("RESIST")) return RESISTENCIA;
        return MASA_MUSCULAR;
    }
}
