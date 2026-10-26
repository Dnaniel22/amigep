package pe.edu.utp.amigep.app.ui.entrenador;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.Vistas;

/** Consulta de la alimentación de un cliente asignado: plan, consumo de hoy y últimos 7 días. */
public class AlimentacionClienteActivity extends AppCompatActivity {

    private static final String ID = "idCliente";
    private static final String NOMBRE = "nombre";

    public static void abrir(Context c, int idCliente, String nombre) {
        c.startActivity(new Intent(c, AlimentacionClienteActivity.class).putExtra(ID, idCliente).putExtra(NOMBRE, nombre));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alimentacion_cliente);
        ((TextView) findViewById(R.id.tituloBarra)).setText("Alimentación");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        Llamada.ejecutar(this, ApiCliente.de(this).alimentacionCliente(getIntent().getIntExtra(ID, 0)), this::mostrar,
                null, Llamada.mientrasViva(this));
    }

    private void mostrar(Modelos.AlimentacionCliente a) {
        String nombre = getIntent().getStringExtra(NOMBRE);
        Modelos.Alimentacion hoy = a.hoy;
        TextView plan = findViewById(R.id.acPlan);
        TextView detalle = findViewById(R.id.acPlanDetalle);
        if (hoy.plan != null) {
            plan.setText(nombre + " · " + hoy.plan.objetivo);
            detalle.setText("Meta: " + Formato.numero(hoy.plan.caloriasObjetivo) + " kcal/día · " + hoy.plan.ritmo);
        } else {
            plan.setText(nombre);
            detalle.setText("El cliente aún no generó su plan nutricional con IA.");
        }
        ((ProgressBar) findViewById(R.id.acBarra)).setProgress(Math.min(100, hoy.hoy.pctMetaCalorica));
        ((TextView) findViewById(R.id.acHoy)).setText("Hoy: " + hoy.hoy.calorias + " kcal"
                + (hoy.hoy.caloriasObjetivo != null ? " (" + hoy.hoy.pctMetaCalorica + "% de la meta)" : "")
                + " · P " + hoy.hoy.pctProteinas + "% · C " + hoy.hoy.pctCarbohidratos + "% · G " + hoy.hoy.pctGrasas + "%");
        ((TextView) findViewById(R.id.acSugerencia)).setText(hoy.sugerencia);
        ((TextView) findViewById(R.id.acTituloSemana)).setText("Últimos 7 días · " + a.diasConRegistro + " días con registro");
        LinearLayout lista = findViewById(R.id.acRegistros);
        lista.removeAllViews();
        if (a.ultimos7Dias.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Sin comidas registradas en la última semana.");
            vacio.setTextColor(Vistas.color(this, R.color.apagado));
            lista.addView(vacio);
        }
        for (Modelos.RegistroComida r : a.ultimos7Dias) {
            View fila = LayoutInflater.from(this).inflate(R.layout.item_comida, lista, false);
            ((TextView) fila.findViewById(R.id.comidaNombre)).setText(Formato.capital(r.tipoComida) + " · " + Formato.diaMes(r.fecha));
            ((TextView) fila.findViewById(R.id.comidaDescripcion)).setText(r.descripcion);
            ((TextView) fila.findViewById(R.id.comidaKcal)).setText((r.calorias == null ? 0 : r.calorias) + " kcal");
            lista.addView(fila);
        }
    }
}
