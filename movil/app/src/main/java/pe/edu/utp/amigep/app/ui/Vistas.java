package pe.edu.utp.amigep.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import pe.edu.utp.amigep.app.R;

/** Componentes visuales reutilizables: insignias de estado, avatares y chips. */
public final class Vistas {

    private static final int[] COLORES_AVATAR = {
            0xFFE3B34C, 0xFF5B7FD6, 0xFFD9744F, 0xFF8B5CC9, 0xFF3FB58E, 0xFFC9A43A, 0xFFC9567A, 0xFF4FA3C9};

    private Vistas() {
    }

    public static int dp(Context c, float dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    /** Avatar circular con iniciales y color estable según el nombre. */
    public static void avatar(TextView vista, String iniciales, String semilla) {
        vista.setText(iniciales);
        int h = 0;
        String s = semilla == null ? iniciales : semilla;
        for (int i = 0; i < s.length(); i++) h = h * 31 + s.charAt(i);
        vista.setBackgroundTintList(ColorStateList.valueOf(COLORES_AVATAR[Math.abs(h) % COLORES_AVATAR.length]));
    }

    public enum Tono { VERDE, DORADO, ROJO, GRIS }

    public static void insignia(TextView vista, String texto, Tono tono) {
        Context c = vista.getContext();
        int texto_color;
        int fondo;
        switch (tono) {
            case VERDE:
                texto_color = ContextCompat.getColor(c, R.color.turquesa);
                fondo = ContextCompat.getColor(c, R.color.turquesa_fondo);
                break;
            case DORADO:
                texto_color = ContextCompat.getColor(c, R.color.dorado);
                fondo = ContextCompat.getColor(c, R.color.dorado_fondo);
                break;
            case ROJO:
                texto_color = ContextCompat.getColor(c, R.color.rojo);
                fondo = ContextCompat.getColor(c, R.color.rojo_fondo);
                break;
            default:
                texto_color = ContextCompat.getColor(c, R.color.texto_2);
                fondo = ContextCompat.getColor(c, R.color.gris_fondo);
        }
        vista.setText(texto);
        vista.setTextColor(texto_color);
        vista.setBackgroundResource(R.drawable.bg_insignia);
        vista.setBackgroundTintList(ColorStateList.valueOf(fondo));
        vista.setVisibility(View.VISIBLE);
    }

    /** Insignia según los estados que devuelve la API. */
    public static void insigniaEstado(TextView vista, String estado) {
        if (estado == null) {
            vista.setVisibility(View.GONE);
            return;
        }
        switch (estado) {
            case "VIGENTE": insignia(vista, "Vigente", Tono.VERDE); break;
            case "ACTIVA": insignia(vista, "Activa", Tono.VERDE); break;
            case "POR_VENCER": insignia(vista, "Por vencer", Tono.DORADO); break;
            case "POR_FINALIZAR": insignia(vista, "Por finalizar", Tono.DORADO); break;
            case "PENDIENTE": insignia(vista, "Pendiente", Tono.GRIS); break;
            case "VENCIDA": insignia(vista, "Vencida", Tono.ROJO); break;
            case "SUSPENDIDA": insignia(vista, "Suspendida", Tono.GRIS); break;
            case "PROGRAMADA": insignia(vista, "Programada", Tono.GRIS); break;
            case "FINALIZADA": insignia(vista, "Finalizada", Tono.GRIS); break;
            case "INACTIVA": insignia(vista, "Inactiva", Tono.GRIS); break;
            case "VALIDADO": insignia(vista, "Validado", Tono.VERDE); break;
            case "RECHAZADO": insignia(vista, "Rechazado", Tono.ROJO); break;
            default: insignia(vista, Formato.capital(estado), Tono.GRIS);
        }
    }

    /** Chip de filtro (activo = turquesa). */
    public static TextView chip(Context c, ViewGroup padre, String texto, boolean activo, View.OnClickListener clic) {
        TextView chip = (TextView) LayoutInflater.from(c).inflate(R.layout.item_chip, padre, false);
        chip.setText(texto);
        marcarChip(chip, activo);
        chip.setOnClickListener(clic);
        padre.addView(chip);
        return chip;
    }

    public static void marcarChip(TextView chip, boolean activo) {
        chip.setBackgroundResource(activo ? R.drawable.bg_chip_activo : R.drawable.bg_chip);
        chip.setTextColor(ContextCompat.getColor(chip.getContext(), activo ? R.color.turquesa : R.color.texto_2));
    }

    public static int color(Context c, int idColor) {
        return ContextCompat.getColor(c, idColor);
    }

    public static int colorRiesgo(String estado) {
        if ("ALTO".equals(estado)) return Color.parseColor("#E8687A");
        if ("MEDIO".equals(estado)) return Color.parseColor("#E3B34C");
        return Color.parseColor("#2BD9A3");
    }
}
