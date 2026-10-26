package pe.edu.utp.amigep.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;

/** RF-19: notificaciones del cliente (vencimientos, rutinas asignadas y retención) y su estado de lectura. */
public class NotificacionesActivity extends AppCompatActivity {

    private ListaAdapter<Modelos.Notificacion> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        ((TextView) findViewById(R.id.tituloBarra)).setText("Notificaciones");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListaAdapter<>(R.layout.item_notificacion, this::enlazar, this::marcarLeida);
        lista.setAdapter(adapter);
        cargar();
    }

    private void cargar() {
        Llamada.ejecutar(this, ApiCliente.de(this).notificaciones(), lista -> {
            adapter.mostrar(lista);
            TextView vacio = findViewById(R.id.vacio);
            vacio.setText("No tienes notificaciones.");
            vacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        }, null, Llamada.mientrasViva(this));
    }

    private void enlazar(View fila, Modelos.Notificacion n, int posicion) {
        int icono = "RUTINA".equals(n.tipo) ? R.drawable.ic_entreno : "RETENCION".equals(n.tipo) ? R.drawable.ic_ia : R.drawable.ic_campana;
        ImageView img = fila.findViewById(R.id.notIcono);
        img.setImageResource(icono);
        img.setColorFilter(Vistas.color(this, "VENCIMIENTO".equals(n.tipo) ? R.color.dorado : R.color.turquesa));
        ((TextView) fila.findViewById(R.id.notTitulo)).setText(n.titulo);
        ((TextView) fila.findViewById(R.id.notMensaje)).setText(n.mensaje);
        ((TextView) fila.findViewById(R.id.notFecha)).setText(Formato.diaMes(n.fechaEnvio) + " · " + Formato.hora(n.fechaEnvio));
        fila.findViewById(R.id.notPunto).setVisibility(n.leido ? View.INVISIBLE : View.VISIBLE);
        fila.setAlpha(n.leido ? 0.75f : 1f);
    }

    private void marcarLeida(Modelos.Notificacion n) {
        if (n.leido) return;
        Llamada.ejecutar(this, ApiCliente.de(this).marcarLeida(n.idNotificacion), r -> cargar(), null, Llamada.mientrasViva(this));
    }
}
