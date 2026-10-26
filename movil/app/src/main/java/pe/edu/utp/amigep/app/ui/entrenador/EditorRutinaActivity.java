package pe.edu.utp.amigep.app.ui.entrenador;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.Dialogos;
import pe.edu.utp.amigep.app.ui.Formato;
import retrofit2.Call;

/**
 * Crear o actualizar la rutina de un cliente asignado (BPM Fig. 8): el entrenador define
 * días, ejercicios, series y repeticiones; al guardar el sistema la vincula y notifica al cliente.
 */
public class EditorRutinaActivity extends AppCompatActivity {

    private static final String CLIENTE = "cliente";
    private static final String[] OBJETIVOS = {"Masa muscular", "Pérdida de grasa", "Fuerza", "Resistencia"};
    private static final String[] NIVELES = {"PRINCIPIANTE", "INTERMEDIO", "AVANZADO"};
    private static final String[] NIVELES_TEXTO = {"Principiante", "Intermedio", "Avanzado"};

    private Modelos.DetalleCliente cliente;
    private LinearLayout dias;
    private EditText nombre;
    private EditText sesiones;
    private EditText semanas;
    private Spinner objetivo;
    private Spinner nivel;
    private CheckBox comoNueva;

    public static void abrir(Context c, Modelos.DetalleCliente cliente) {
        c.startActivity(new Intent(c, EditorRutinaActivity.class).putExtra(CLIENTE, ApiCliente.gson().toJson(cliente)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_rutina);
        cliente = ApiCliente.gson().fromJson(getIntent().getStringExtra(CLIENTE), Modelos.DetalleCliente.class);
        Modelos.Rutina actual = cliente.rutinaActiva;
        ((TextView) findViewById(R.id.tituloBarra)).setText(actual == null ? "Asignar rutina" : "Actualizar rutina");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.erCliente)).setText("Cliente: " + cliente.nombre + " " + cliente.apellido
                + (cliente.objetivo != null ? " · " + cliente.objetivo : ""));

        dias = findViewById(R.id.erDias);
        nombre = findViewById(R.id.erNombre);
        sesiones = findViewById(R.id.erSesiones);
        semanas = findViewById(R.id.erSemanas);
        objetivo = findViewById(R.id.erObjetivo);
        nivel = findViewById(R.id.erNivel);
        comoNueva = findViewById(R.id.erNueva);
        Dialogos.llenarSpinner(objetivo, OBJETIVOS, actual != null ? actual.objetivo : cliente.objetivo);
        Dialogos.llenarSpinner(nivel, NIVELES_TEXTO, Formato.nivel(actual != null ? actual.nivel : cliente.nivel));

        if (actual != null && actual.contenido != null) {
            nombre.setText(actual.nombre);
            sesiones.setText(String.valueOf(actual.contenido.sesionesPorSemana != null ? actual.contenido.sesionesPorSemana : 3));
            semanas.setText(String.valueOf(actual.semanasTotales));
            for (Modelos.DiaRutina d : actual.contenido.dias) agregarDia(d);
            comoNueva.setVisibility(View.VISIBLE);
        } else {
            sesiones.setText("3");
            semanas.setText("8");
            agregarDia(null);
        }
        findViewById(R.id.erAgregarDia).setOnClickListener(v -> agregarDia(null));
        findViewById(R.id.erGuardar).setOnClickListener(v -> guardar());
    }

    private void agregarDia(Modelos.DiaRutina dia) {
        View bloque = LayoutInflater.from(this).inflate(R.layout.item_editor_dia, dias, false);
        LinearLayout ejercicios = bloque.findViewById(R.id.edDiaEjercicios);
        if (dia != null) {
            ((EditText) bloque.findViewById(R.id.edDiaNombre)).setText(dia.nombre);
            ((EditText) bloque.findViewById(R.id.edDiaEnfoque)).setText(dia.enfoque);
            for (Modelos.Ejercicio e : dia.ejercicios) agregarEjercicio(ejercicios, e);
        } else {
            agregarEjercicio(ejercicios, null);
        }
        bloque.findViewById(R.id.edDiaAgregar).setOnClickListener(v -> agregarEjercicio(ejercicios, null));
        bloque.findViewById(R.id.edDiaEliminar).setOnClickListener(v -> {
            if (dias.getChildCount() > 1) {
                dias.removeView(bloque);
                numerarDias();
            } else {
                Toast.makeText(this, "La rutina necesita al menos un día", Toast.LENGTH_SHORT).show();
            }
        });
        dias.addView(bloque);
        numerarDias();
    }

    private void agregarEjercicio(LinearLayout contenedor, Modelos.Ejercicio e) {
        View fila = LayoutInflater.from(this).inflate(R.layout.item_editor_ejercicio, contenedor, false);
        if (e != null) {
            ((EditText) fila.findViewById(R.id.eeNombre)).setText(e.nombre);
            ((EditText) fila.findViewById(R.id.eeSeries)).setText(String.valueOf(e.series));
            ((EditText) fila.findViewById(R.id.eeReps)).setText(e.repeticiones);
            fila.setTag(e.descansoSeg);
        }
        fila.findViewById(R.id.eeEliminar).setOnClickListener(v -> {
            if (contenedor.getChildCount() > 1) contenedor.removeView(fila);
        });
        contenedor.addView(fila);
    }

    private void numerarDias() {
        for (int i = 0; i < dias.getChildCount(); i++) {
            ((TextView) dias.getChildAt(i).findViewById(R.id.edDiaNumero)).setText(String.format(Locale.US, "Día %d", i + 1));
        }
    }

    /** Valida (RNF-05) y arma el JSON de la rutina. Devuelve null si hay errores. */
    private Modelos.GuardarRutinaRequest construir() {
        Modelos.GuardarRutinaRequest req = new Modelos.GuardarRutinaRequest();
        req.nombre = nombre.getText().toString().trim();
        req.objetivo = OBJETIVOS[objetivo.getSelectedItemPosition()];
        req.nivel = NIVELES[nivel.getSelectedItemPosition()];
        req.sesionesPorSemana = Formato.aEntero(sesiones.getText().toString());
        req.semanas = Formato.aEntero(semanas.getText().toString());
        if (req.nombre.isEmpty()) {
            nombre.setError("Ingrese un nombre");
            return null;
        }
        if (req.sesionesPorSemana == null || req.sesionesPorSemana < 1 || req.sesionesPorSemana > 7) {
            sesiones.setError("Entre 1 y 7");
            return null;
        }
        if (req.semanas == null || req.semanas < 1 || req.semanas > 24) {
            semanas.setError("Entre 1 y 24");
            return null;
        }
        for (int i = 0; i < dias.getChildCount(); i++) {
            View bloque = dias.getChildAt(i);
            EditText nombreDia = bloque.findViewById(R.id.edDiaNombre);
            Modelos.DiaRutina dia = new Modelos.DiaRutina();
            dia.dia = i + 1;
            dia.nombre = nombreDia.getText().toString().trim();
            dia.enfoque = ((EditText) bloque.findViewById(R.id.edDiaEnfoque)).getText().toString().trim();
            if (dia.nombre.isEmpty()) {
                nombreDia.setError("Ingrese el nombre del día");
                nombreDia.requestFocus();
                return null;
            }
            LinearLayout ejercicios = bloque.findViewById(R.id.edDiaEjercicios);
            for (int j = 0; j < ejercicios.getChildCount(); j++) {
                View fila = ejercicios.getChildAt(j);
                EditText nombreEj = fila.findViewById(R.id.eeNombre);
                EditText series = fila.findViewById(R.id.eeSeries);
                EditText reps = fila.findViewById(R.id.eeReps);
                String n = nombreEj.getText().toString().trim();
                if (n.isEmpty() && series.getText().length() == 0 && reps.getText().length() == 0) continue;
                Integer s = Formato.aEntero(series.getText().toString());
                if (n.isEmpty()) { nombreEj.setError("Ejercicio"); nombreEj.requestFocus(); return null; }
                if (s == null || s < 1 || s > 10) { series.setError("1-10"); series.requestFocus(); return null; }
                if (reps.getText().toString().trim().isEmpty()) { reps.setError("Reps"); reps.requestFocus(); return null; }
                Integer descanso = fila.getTag() instanceof Integer ? (Integer) fila.getTag() : 60;
                dia.ejercicios.add(new Modelos.Ejercicio(n, s, reps.getText().toString().trim(), descanso));
            }
            if (dia.ejercicios.isEmpty()) {
                Toast.makeText(this, "El " + dia.nombre + " necesita al menos un ejercicio", Toast.LENGTH_LONG).show();
                return null;
            }
            req.dias.add(dia);
        }
        return req;
    }

    private void guardar() {
        Modelos.GuardarRutinaRequest req = construir();
        if (req == null) return;
        boolean actualizar = cliente.rutinaActiva != null && !comoNueva.isChecked();
        Call<Modelos.Rutina> llamada = actualizar
                ? ApiCliente.de(this).actualizarRutina(cliente.idCliente, cliente.rutinaActiva.idRutina, req)
                : ApiCliente.de(this).crearRutina(cliente.idCliente, req);
        findViewById(R.id.erGuardar).setEnabled(false);
        Llamada.ejecutar(this, llamada, r -> {
            Toast.makeText(this, "Rutina guardada. Se notificó a " + cliente.nombre.split(" ")[0] + ".", Toast.LENGTH_LONG).show();
            finish();
        }, (mensaje, campos) -> {
            findViewById(R.id.erGuardar).setEnabled(true);
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        }, Llamada.mientrasViva(this));
    }
}
