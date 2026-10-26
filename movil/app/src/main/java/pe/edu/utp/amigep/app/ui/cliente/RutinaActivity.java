package pe.edu.utp.amigep.app.ui.cliente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.Vistas;

/** Rutina activa completa: días, ejercicios, series y repeticiones; el cliente marca su sesión (RF-09). */
public class RutinaActivity extends AppCompatActivity {

    private Modelos.Rutina rutina;
    private int diaDeHoy = -1;
    private boolean hoyCompletada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rutina);
        ((TextView) findViewById(R.id.tituloBarra)).setText("Mi rutina");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        findViewById(R.id.btnCompletar).setOnClickListener(v -> completarSesion());
        cargar();
    }

    private void cargar() {
        Llamada.ejecutar(this, ApiCliente.de(this).inicio(), inicio -> {
            if (inicio.sesionHoy != null) {
                diaDeHoy = inicio.sesionHoy.dia;
                hoyCompletada = inicio.sesionHoy.completada;
            }
            Llamada.ejecutar(this, ApiCliente.de(this).rutinas(), r -> mostrar(r.activa), null, Llamada.mientrasViva(this));
        }, null, Llamada.mientrasViva(this));
    }

    private void mostrar(Modelos.Rutina r) {
        rutina = r;
        Button completar = findViewById(R.id.btnCompletar);
        if (r == null) {
            ((TextView) findViewById(R.id.rNombre)).setText("Sin rutina activa");
            ((TextView) findViewById(R.id.rDetalle)).setText("Genera una rutina con IA desde la pestaña Entreno.");
            findViewById(R.id.rBarra).setVisibility(View.GONE);
            completar.setVisibility(View.GONE);
            return;
        }
        ((TextView) findViewById(R.id.rNombre)).setText(r.nombre);
        Vistas.insigniaEstado(findViewById(R.id.rEstado), r.estado);
        ((TextView) findViewById(R.id.rDetalle)).setText("Semana " + r.semanaActual + " de " + r.semanasTotales + " · "
                + Formato.nivel(r.nivel) + " · " + r.ritmo + "\n"
                + ("IA".equals(r.origen) ? "Generada por el módulo inteligente" : "Asignada por " + r.entrenador));
        ProgressBar barra = findViewById(R.id.rBarra);
        barra.setProgress(r.cumplimientoPct);
        ((TextView) findViewById(R.id.rCumplimiento)).setText("Cumplimiento " + r.cumplimientoPct + "% · "
                + r.sesionesCompletadas + " sesiones completadas");

        LinearLayout dias = findViewById(R.id.listaDias);
        dias.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (r.contenido != null) {
            for (Modelos.DiaRutina d : r.contenido.dias) {
                View bloque = inflater.inflate(R.layout.item_dia_rutina, dias, false);
                ((TextView) bloque.findViewById(R.id.diaTitulo)).setText("Día " + d.dia + " · " + d.nombre
                        + (d.enfoque == null || d.enfoque.isEmpty() ? "" : " — " + d.enfoque));
                if (d.dia == diaDeHoy) {
                    Vistas.insignia(bloque.findViewById(R.id.diaHoy), hoyCompletada ? "Hecho hoy" : "Hoy", Vistas.Tono.VERDE);
                }
                LinearLayout lista = bloque.findViewById(R.id.diaEjercicios);
                for (Modelos.Ejercicio e : d.ejercicios) {
                    View fila = inflater.inflate(R.layout.item_ejercicio, lista, false);
                    ((TextView) fila.findViewById(R.id.ejNombre)).setText(e.nombre);
                    ((TextView) fila.findViewById(R.id.ejSeries)).setText(e.series + " × " + e.repeticiones);
                    if (e.descansoSeg != null && e.descansoSeg > 0) {
                        TextView det = fila.findViewById(R.id.ejDetalle);
                        det.setVisibility(View.VISIBLE);
                        det.setText("Descanso " + e.descansoSeg + " s");
                    }
                    lista.addView(fila);
                }
                dias.addView(bloque);
            }
            StringBuilder notas = new StringBuilder();
            for (String n : r.contenido.notas) notas.append("• ").append(n).append('\n');
            ((TextView) findViewById(R.id.rNotas)).setText(notas.toString().trim());
        }
        completar.setVisibility(View.VISIBLE);
        completar.setEnabled(!hoyCompletada);
        completar.setAlpha(hoyCompletada ? 0.5f : 1f);
        completar.setText(hoyCompletada ? "Sesión de hoy completada" : "Marcar sesión de hoy como completada");
    }

    private void completarSesion() {
        if (rutina == null) return;
        Llamada.ejecutar(this, ApiCliente.de(this).registrarSesion(rutina.idRutina, new Modelos.RegistrarSesionRequest(true, null)), s -> {
            Toast.makeText(this, "¡Sesión registrada! Sigue así 💪", Toast.LENGTH_SHORT).show();
            cargar();
        }, null, Llamada.mientrasViva(this));
    }
}
