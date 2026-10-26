package pe.edu.utp.amigep.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Utilidades de formato compartidas por los servicios. */
public final class Textos {

    public static final Locale PERU = Locale.forLanguageTag("es-PE");
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d MMM", PERU);
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", PERU);

    private Textos() {
    }

    public static String iniciales(String nombre, String apellido) {
        String a = nombre == null || nombre.isBlank() ? "" : nombre.trim().substring(0, 1);
        String b = apellido == null || apellido.isBlank() ? "" : apellido.trim().substring(0, 1);
        return (a + b).toUpperCase(PERU);
    }

    /** "Fabricio Hernández A." → nombre + primer apellido + inicial del segundo. */
    public static String nombreCorto(String nombre, String apellido) {
        String primerNombre = nombre.trim().split("\\s+")[0];
        String[] apellidos = apellido.trim().split("\\s+");
        return apellidos.length > 1
                ? primerNombre + " " + apellidos[0] + " " + apellidos[1].charAt(0) + "."
                : primerNombre + " " + apellidos[0];
    }

    public static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static BigDecimal redondear(double valor, int decimales) {
        return BigDecimal.valueOf(valor).setScale(decimales, RoundingMode.HALF_UP);
    }

    public static double redondearD(double valor, int decimales) {
        return redondear(valor, decimales).doubleValue();
    }

    public static String soles(BigDecimal monto) {
        return "S/ " + String.format(PERU, "%,.2f", monto);
    }

    public static String etiquetaMetodo(String metodo) {
        return switch (metodo) {
            case "YAPE_PLIN" -> "Yape / Plin";
            case "TARJETA" -> "Tarjeta";
            case "EFECTIVO" -> "Efectivo";
            default -> metodo;
        };
    }

    public static String capitalizar(String s) {
        if (s == null || s.isBlank()) return s;
        String t = s.toLowerCase(PERU).replace('_', ' ');
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
