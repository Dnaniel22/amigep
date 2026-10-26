package pe.edu.utp.amigep.app.ui.cliente;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.Vistas;

/** Registro de comidas y cantidades; la respuesta trae la evaluación frente a la meta (BPM Fig. 10). */
public class RegistrarComidaActivity extends AppCompatActivity {

    private static final String[] TIPOS = {"DESAYUNO", "ALMUERZO", "CENA", "REFRIGERIO"};
    private final List<TextView> chips = new ArrayList<>();
    private String tipo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_comida);
        ((TextView) findViewById(R.id.tituloBarra)).setText("Registrar comida");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());

        LinearLayout fila1 = findViewById(R.id.tipos1);
        LinearLayout fila2 = findViewById(R.id.tipos2);
        for (int i = 0; i < TIPOS.length; i++) {
            String t = TIPOS[i];
            TextView chip = Vistas.chip(this, i < 2 ? fila1 : fila2, Formato.capital(t), false, v -> seleccionar(t));
            chip.setTag(t);
            chips.add(chip);
        }
        seleccionar(tipoSegunHora());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());
    }

    private String tipoSegunHora() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 11) return "DESAYUNO";
        if (h < 16) return "ALMUERZO";
        if (h < 18) return "REFRIGERIO";
        return "CENA";
    }

    private void seleccionar(String t) {
        tipo = t;
        for (TextView c : chips) Vistas.marcarChip(c, t.equals(c.getTag()));
    }

    private void guardar() {
        EditText descripcion = findViewById(R.id.descripcion);
        EditText calorias = findViewById(R.id.calorias);
        Modelos.RegistrarComidaRequest req = new Modelos.RegistrarComidaRequest();
        req.tipoComida = tipo;
        req.descripcion = descripcion.getText().toString().trim();
        req.calorias = Formato.aEntero(calorias.getText().toString());
        req.proteinas = Formato.aDouble(((EditText) findViewById(R.id.proteinas)).getText().toString());
        req.carbohidratos = Formato.aDouble(((EditText) findViewById(R.id.carbohidratos)).getText().toString());
        req.grasas = Formato.aDouble(((EditText) findViewById(R.id.grasas)).getText().toString());
        if (req.descripcion.isEmpty()) {
            descripcion.setError("Describe la comida");
            return;
        }
        if (req.calorias == null || req.calorias > 5000) {
            calorias.setError("Ingresa las calorías (0 - 5000)");
            return;
        }
        findViewById(R.id.btnGuardar).setEnabled(false);
        Llamada.ejecutar(this, ApiCliente.de(this).registrarComida(req), ev -> new AlertDialog.Builder(this)
                .setTitle(ev.cumpleObjetivo ? "¡Comida registrada!" : "Comida registrada · ajuste sugerido")
                .setMessage(ev.mensaje + "\n\nHoy: " + ev.hoy.calorias + " kcal"
                        + (ev.hoy.caloriasObjetivo != null ? " de " + ev.hoy.caloriasObjetivo + " (" + ev.hoy.pctMetaCalorica + "%)" : ""))
                .setPositiveButton("Aceptar", (d, w) -> finish())
                .setOnCancelListener(d -> finish())
                .show(), (mensaje, campos) -> {
            findViewById(R.id.btnGuardar).setEnabled(true);
            if (campos.containsKey("descripcion")) descripcion.setError(campos.get("descripcion"));
            if (campos.containsKey("calorias")) calorias.setError(campos.get("calorias"));
            if (campos.isEmpty()) android.widget.Toast.makeText(this, mensaje, android.widget.Toast.LENGTH_LONG).show();
        }, Llamada.mientrasViva(this));
    }
}
