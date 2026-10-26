package pe.edu.utp.amigep.app.ui.cliente;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.Dialogos;
import pe.edu.utp.amigep.app.ui.Formato;

/** Editar datos personales y de entrenamiento (los usa el módulo inteligente, RF-10). */
public class EditarPerfilActivity extends AppCompatActivity {

    private static final String[] OBJETIVOS = {"Masa muscular", "Pérdida de grasa", "Fuerza", "Resistencia"};
    private static final String[] NIVELES = {"PRINCIPIANTE", "INTERMEDIO", "AVANZADO"};
    private static final String[] NIVELES_TEXTO = {"Principiante", "Intermedio", "Avanzado"};

    private EditText telefono;
    private EditText peso;
    private EditText altura;
    private TextView nacimiento;
    private Spinner objetivo;
    private Spinner nivel;
    private String fechaNacimiento;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);
        ((TextView) findViewById(R.id.tituloBarra)).setText("Editar datos");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        telefono = findViewById(R.id.edTelefono);
        peso = findViewById(R.id.edPeso);
        altura = findViewById(R.id.edAltura);
        nacimiento = findViewById(R.id.edNacimiento);
        objetivo = findViewById(R.id.edObjetivo);
        nivel = findViewById(R.id.edNivel);
        nacimiento.setOnClickListener(v -> elegirFecha());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());
        Llamada.ejecutar(this, ApiCliente.de(this).perfil(), this::mostrar, null, Llamada.mientrasViva(this));
    }

    private void mostrar(Modelos.PerfilCliente p) {
        telefono.setText(p.telefono);
        if (p.peso != null) peso.setText(String.format(Locale.US, "%.1f", p.peso));
        if (p.altura != null) altura.setText(String.format(Locale.US, "%.2f", p.altura));
        fechaNacimiento = p.fechaNacimiento;
        nacimiento.setText(p.fechaNacimiento != null ? Formato.fecha(p.fechaNacimiento) : "");
        Dialogos.llenarSpinner(objetivo, OBJETIVOS, p.objetivo);
        String nivelTexto = p.nivel == null ? "Principiante" : Formato.nivel(p.nivel);
        Dialogos.llenarSpinner(nivel, NIVELES_TEXTO, nivelTexto);
    }

    private void elegirFecha() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -25);
        if (fechaNacimiento != null && fechaNacimiento.length() >= 10) {
            c.set(Integer.parseInt(fechaNacimiento.substring(0, 4)), Integer.parseInt(fechaNacimiento.substring(5, 7)) - 1,
                    Integer.parseInt(fechaNacimiento.substring(8, 10)));
        }
        DatePickerDialog d = new DatePickerDialog(this, (vista, anio, mes, dia) -> {
            fechaNacimiento = String.format(Locale.US, "%04d-%02d-%02d", anio, mes + 1, dia);
            nacimiento.setText(Formato.fecha(fechaNacimiento));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.getDatePicker().setMaxDate(System.currentTimeMillis());
        d.show();
    }

    private void guardar() {
        Modelos.ActualizarPerfilRequest req = new Modelos.ActualizarPerfilRequest();
        req.telefono = telefono.getText().toString().trim();
        req.objetivo = OBJETIVOS[objetivo.getSelectedItemPosition()];
        req.nivel = NIVELES[nivel.getSelectedItemPosition()];
        req.peso = Formato.aDouble(peso.getText().toString());
        req.altura = Formato.aDouble(altura.getText().toString());
        req.fechaNacimiento = fechaNacimiento;
        if (!req.telefono.matches("^[0-9+ ]*$")) {
            telefono.setError("Teléfono no válido");
            return;
        }
        if (req.peso != null && (req.peso < 25 || req.peso > 350)) {
            peso.setError("Peso no válido");
            return;
        }
        if (req.altura != null && (req.altura < 1 || req.altura > 2.5)) {
            altura.setError("Altura en metros, p. ej. 1.72");
            return;
        }
        Llamada.ejecutar(this, ApiCliente.de(this).actualizarPerfil(req), p -> {
            Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show();
            finish();
        }, (mensaje, campos) -> {
            if (campos.containsKey("telefono")) telefono.setError(campos.get("telefono"));
            if (campos.containsKey("peso")) peso.setError(campos.get("peso"));
            if (campos.containsKey("altura")) altura.setError(campos.get("altura"));
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        }, Llamada.mientrasViva(this));
    }
}
