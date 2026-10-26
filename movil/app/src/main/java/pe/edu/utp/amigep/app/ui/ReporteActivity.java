package pe.edu.utp.amigep.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;

/**
 * Visor de reportes (RF-20) para el cliente (RP-06, RP-07, RP-08) y el entrenador (RP-04 a RP-07).
 * El backend aplica los permisos: el cliente solo ve sus datos y el entrenador, a sus clientes asignados.
 */
public class ReporteActivity extends AppCompatActivity {

    private static final String CODIGO = "codigo";
    private static final String TITULO = "titulo";
    private static final String FILTROS = "filtros";

    public static void abrir(Context c, String codigo, String titulo, HashMap<String, String> filtros) {
        Intent i = new Intent(c, ReporteActivity.class);
        i.putExtra(CODIGO, codigo);
        i.putExtra(TITULO, titulo);
        i.putExtra(FILTROS, filtros == null ? new HashMap<String, String>() : filtros);
        c.startActivity(i);
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        String codigo = getIntent().getStringExtra(CODIGO);
        ((TextView) findViewById(R.id.tituloBarra)).setText(getIntent().getStringExtra(TITULO));
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        Map<String, String> filtros = (HashMap<String, String>) getIntent().getSerializableExtra(FILTROS);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        Llamada.ejecutar(this, ApiCliente.de(this).reporte(codigo, filtros == null ? new HashMap<>() : filtros),
                r -> mostrar(r, lista), (mensaje, campos) -> {
                    TextView vacio = findViewById(R.id.vacio);
                    vacio.setText(mensaje);
                    vacio.setVisibility(View.VISIBLE);
                }, Llamada.mientrasViva(this));
    }

    private void mostrar(Modelos.Reporte r, RecyclerView lista) {
        TextView encabezado = findViewById(R.id.encabezado);
        StringBuilder sb = new StringBuilder(r.codigo + " · " + r.titulo + "\n" + r.descripcion);
        if (r.resumen != null) {
            for (Map.Entry<String, Object> e : r.resumen.entrySet()) {
                if (e.getValue() instanceof List) continue;
                sb.append("\n• ").append(etiqueta(e.getKey())).append(": ").append(valor(e.getValue()));
            }
        }
        encabezado.setText(sb.toString());
        encabezado.setVisibility(View.VISIBLE);
        ListaAdapter<Map<String, Object>> adapter = new ListaAdapter<>(R.layout.item_fila_reporte, (fila, datos, pos) -> {
            Modelos.Columna primera = r.columnas.get(0);
            ((TextView) fila.findViewById(R.id.filaTitulo)).setText(valor(datos.get(primera.clave), primera.tipo));
            StringBuilder detalle = new StringBuilder();
            for (int i = 1; i < r.columnas.size(); i++) {
                Modelos.Columna c = r.columnas.get(i);
                Object v = datos.get(c.clave);
                if (v == null || String.valueOf(v).isEmpty()) continue;
                if (detalle.length() > 0) detalle.append('\n');
                detalle.append(c.titulo).append(": ").append(valor(v, c.tipo));
            }
            ((TextView) fila.findViewById(R.id.filaDetalle)).setText(detalle.toString());
        }, null);
        lista.setAdapter(adapter);
        adapter.mostrar(r.filas);
        TextView vacio = findViewById(R.id.vacio);
        vacio.setText("No hay registros en el periodo.");
        vacio.setVisibility(r.filas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static String etiqueta(String clave) {
        String conEspacios = clave.replaceAll("([A-Z])", " $1").toLowerCase(Formato.PERU);
        return Character.toUpperCase(conEspacios.charAt(0)) + conEspacios.substring(1);
    }

    private static String valor(Object v) {
        if (v instanceof Double && ((Double) v) % 1 == 0) return String.valueOf(((Double) v).longValue());
        if (v instanceof Map) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(Formato.capital(String.valueOf(e.getKey()))).append(' ').append(valor(e.getValue()));
            }
            return sb.toString();
        }
        return String.valueOf(v);
    }

    private static String valor(Object v, String tipo) {
        if (v == null) return "—";
        if ("fecha".equals(tipo)) return Formato.fecha(String.valueOf(v));
        if ("moneda".equals(tipo) && v instanceof Number) return Formato.soles(((Number) v).doubleValue());
        if ("porcentaje".equals(tipo)) return valor(v) + "%";
        if ("estado".equals(tipo)) return Formato.capital(String.valueOf(v));
        return valor(v);
    }
}
