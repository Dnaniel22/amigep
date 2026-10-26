package pe.edu.utp.amigep.app.ui;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Formatos de fechas, montos y textos (las fechas de la API llegan como texto ISO). */
public final class Formato {

    public static final Locale PERU = new Locale("es", "PE");
    private static final String[] MESES = {"ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sept.", "oct.", "nov.", "dic."};

    private Formato() {
    }

    private static Date parsear(String iso) {
        if (iso == null || iso.length() < 10) return null;
        try {
            String patron = iso.length() >= 16 ? "yyyy-MM-dd'T'HH:mm" : "yyyy-MM-dd";
            return new SimpleDateFormat(patron, Locale.US).parse(iso.length() >= 16 ? iso.substring(0, 16) : iso.substring(0, 10));
        } catch (ParseException e) {
            return null;
        }
    }

    /** "2026-12-12" → "12/12/2026" */
    public static String fecha(String iso, String vacio) {
        Date d = parsear(iso);
        return d == null ? vacio : new SimpleDateFormat("dd/MM/yyyy", PERU).format(d);
    }

    public static String fecha(String iso) {
        return fecha(iso, "—");
    }

    /** "2026-09-24T07:05" → "24 sept." */
    public static String diaMes(String iso) {
        Date d = parsear(iso);
        if (d == null) return "—";
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_MONTH) + " " + MESES[c.get(Calendar.MONTH)];
    }

    /** "2026-08-05" → "ago. 2026" */
    public static String mesAnio(String iso) {
        Date d = parsear(iso);
        if (d == null) return "—";
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return MESES[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
    }

    /** "2026-09-24T07:05:00" → "7:05 a.m." */
    public static String hora(String iso) {
        Date d = parsear(iso);
        if (d == null) return "";
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int h = c.get(Calendar.HOUR);
        return (h == 0 ? 12 : h) + ":" + String.format(Locale.US, "%02d", c.get(Calendar.MINUTE))
                + (c.get(Calendar.AM_PM) == Calendar.AM ? " a.m." : " p.m.");
    }

    public static String hoyIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String haceDiasIso(int dias) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -dias);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    public static String soles(double monto) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.US);
        f.setMaximumFractionDigits(monto % 1 == 0 ? 0 : 2);
        f.setMinimumFractionDigits(monto % 1 == 0 ? 0 : 2);
        return "S/ " + f.format(monto);
    }

    public static String numero(double n) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.US);
        f.setMaximumFractionDigits(1);
        return f.format(n);
    }

    public static String porcentajeConSigno(Double pct) {
        if (pct == null) return "—";
        return (pct > 0 ? "+" : "") + numero(pct) + "%";
    }

    public static String nivel(String nivel) {
        if (nivel == null) return "Sin definir";
        switch (nivel) {
            case "PRINCIPIANTE": return "Principiante";
            case "INTERMEDIO": return "Intermedio";
            case "AVANZADO": return "Avanzado";
            default: return capital(nivel);
        }
    }

    public static String nivelCorto(String nivel) {
        if ("INTERMEDIO".equals(nivel)) return "Nivel interm.";
        return "Nivel " + nivel(nivel).toLowerCase(PERU);
    }

    public static String metodo(String metodo) {
        if ("YAPE_PLIN".equals(metodo)) return "Yape";
        if ("TARJETA".equals(metodo)) return "tarjeta";
        if ("EFECTIVO".equals(metodo)) return "efectivo";
        return metodo;
    }

    public static String capital(String s) {
        if (s == null || s.isEmpty()) return "";
        String t = s.replace('_', ' ').toLowerCase(PERU);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    public static String iniciales(String nombre, String apellido) {
        String a = nombre == null || nombre.trim().isEmpty() ? "" : nombre.trim().substring(0, 1);
        String b = apellido == null || apellido.trim().isEmpty() ? "" : apellido.trim().substring(0, 1);
        return (a + b).toUpperCase(PERU);
    }

    /** "Luis Fernando Atocsa Tapia" → "LA" */
    public static String inicialesDe(String completo) {
        if (completo == null || completo.trim().isEmpty()) return "?";
        String[] p = completo.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, 1).toUpperCase(PERU);
        int i = p.length == 3 ? 1 : (int) Math.ceil(p.length / 2.0);
        return (p[0].substring(0, 1) + p[Math.min(i, p.length - 1)].substring(0, 1)).toUpperCase(PERU);
    }

    /** Nombre + primer apellido: "Luis Atocsa" */
    public static String nombreCorto(String nombre, String apellido) {
        String n = nombre == null ? "" : nombre.trim().split("\\s+")[0];
        String a = apellido == null ? "" : apellido.trim().split("\\s+")[0];
        return (n + " " + a).trim();
    }

    public static Double aDouble(String texto) {
        try {
            return texto == null || texto.trim().isEmpty() ? null : Double.valueOf(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer aEntero(String texto) {
        try {
            return texto == null || texto.trim().isEmpty() ? null : Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
