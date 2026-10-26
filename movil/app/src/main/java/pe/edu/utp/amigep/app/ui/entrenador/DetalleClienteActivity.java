package pe.edu.utp.amigep.app.ui.entrenador;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.HashMap;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.ReporteActivity;
import pe.edu.utp.amigep.app.ui.Vistas;

/** E2 · Detalle de cliente: rutina activa, progreso reciente, últimas asistencias y acciones. */
public class DetalleClienteActivity extends AppCompatActivity {

    private static final String ID_CLIENTE = "idCliente";
    private int idCliente;
    private Modelos.DetalleCliente detalle;
    private SwipeRefreshLayout refrescar;
    private boolean recargarAlVolver;

    public static void abrir(Context c, int idCliente) {
        c.startActivity(new Intent(c, DetalleClienteActivity.class).putExtra(ID_CLIENTE, idCliente));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_cliente);
        idCliente = getIntent().getIntExtra(ID_CLIENTE, 0);
        ((TextView) findViewById(R.id.tituloBarra)).setText("Detalle de cliente");
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        ImageView editar = findViewById(R.id.accionBarra);
        editar.setImageResource(R.drawable.ic_editar);
        editar.setVisibility(View.VISIBLE);
        editar.setOnClickListener(v -> editarRutina());
        refrescar = findViewById(R.id.refrescar);
        refrescar.setColorSchemeResources(R.color.turquesa);
        refrescar.setProgressBackgroundColorSchemeResource(R.color.tarjeta);
        refrescar.setOnRefreshListener(this::cargar);
        findViewById(R.id.btnActualizarRutina).setOnClickListener(v -> editarRutina());
        findViewById(R.id.btnVerAlimentacion).setOnClickListener(v -> AlimentacionClienteActivity.abrir(this, idCliente,
                detalle == null ? "" : detalle.nombre + " " + detalle.apellido));
        opcion(R.id.dReporteProgreso, R.drawable.ic_progreso, "Progreso del cliente (RP-06)", "RP-06");
        opcion(R.id.dReporteAlimentacion, R.drawable.ic_dieta, "Seguimiento alimenticio (RP-07)", "RP-07");
        cargar();
    }

    private void opcion(int id, int icono, String texto, String codigo) {
        View fila = findViewById(id);
        ((ImageView) fila.findViewById(R.id.icono)).setImageResource(icono);
        ((TextView) fila.findViewById(R.id.texto)).setText(texto);
        fila.setOnClickListener(v -> {
            HashMap<String, String> filtros = new HashMap<>();
            filtros.put("idCliente", String.valueOf(idCliente));
            ReporteActivity.abrir(this, codigo, texto, filtros);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recargarAlVolver) {
            recargarAlVolver = false;
            cargar();
        }
    }

    private void cargar() {
        refrescar.setRefreshing(true);
        Llamada.ejecutar(this, ApiCliente.de(this).detalleCliente(idCliente), d -> {
            refrescar.setRefreshing(false);
            detalle = d;
            mostrar(d);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            android.widget.Toast.makeText(this, mensaje, android.widget.Toast.LENGTH_LONG).show();
        }, Llamada.mientrasViva(this));
    }

    private void mostrar(Modelos.DetalleCliente d) {
        Vistas.avatar(findViewById(R.id.dAvatar), d.iniciales, d.nombre + d.apellido);
        ((TextView) findViewById(R.id.dNombre)).setText(Formato.nombreCorto(d.nombre, d.apellido) + " "
                + (d.apellido.contains(" ") ? d.apellido.substring(d.apellido.indexOf(' ') + 1) : ""));
        ((TextView) findViewById(R.id.dObjetivo)).setText("Objetivo: " + (d.objetivo == null ? "sin definir" : d.objetivo.toLowerCase(Formato.PERU))
                + " · " + Formato.nivelCorto(d.nivel) + (d.edad != null ? " · " + d.edad + " años" : ""));

        TextView rutina = findViewById(R.id.dRutina);
        TextView rutinaDetalle = findViewById(R.id.dRutinaDetalle);
        ProgressBar barra = findViewById(R.id.dRutinaBarra);
        if (d.rutinaActiva != null) {
            Modelos.Rutina r = d.rutinaActiva;
            rutina.setText("Rutina: " + r.nombre);
            Vistas.insignia(findViewById(R.id.dRutinaEstado), "POR_FINALIZAR".equals(r.estado) ? "Por finalizar" : "Activa",
                    "POR_FINALIZAR".equals(r.estado) ? Vistas.Tono.DORADO : Vistas.Tono.VERDE);
            int sesiones = r.contenido != null && r.contenido.sesionesPorSemana != null ? r.contenido.sesionesPorSemana : 0;
            rutinaDetalle.setText("Semana " + r.semanaActual + " de " + r.semanasTotales + " · " + sesiones + " sesiones / semana · "
                    + ("IA".equals(r.origen) ? "IA" : "Entrenador") + " · cumplimiento " + r.cumplimientoPct + "%");
            barra.setVisibility(View.VISIBLE);
            barra.setProgress(Math.round(r.semanaActual * 100f / Math.max(1, r.semanasTotales)));
            ((android.widget.Button) findViewById(R.id.btnActualizarRutina)).setText("Actualizar rutina");
        } else {
            rutina.setText("Sin rutina asignada");
            Vistas.insignia(findViewById(R.id.dRutinaEstado), "Pendiente", Vistas.Tono.GRIS);
            rutinaDetalle.setText("Crea una rutina para este cliente.");
            barra.setVisibility(View.GONE);
            ((android.widget.Button) findViewById(R.id.btnActualizarRutina)).setText("Asignar rutina");
        }

        ((TextView) findViewById(R.id.dPeso)).setText(d.pesoActual == null ? "—" : conDetalle(Formato.numero(d.pesoActual) + " kg",
                d.variacionPesoPct == null ? "" : " " + Formato.porcentajeConSigno(d.variacionPesoPct)));
        ((TextView) findViewById(R.id.dAsistencia)).setText(d.asistenciasMes + " / " + d.diasMes + " días");
        ((TextView) findViewById(R.id.dMembresia)).setText(d.membresia == null ? "Sin membresía registrada"
                : "Membresía " + d.membresia.plan + " · " + Formato.capital(d.membresia.estado) + " hasta " + Formato.fecha(d.membresia.fechaCaducidad));

        LinearLayout asistencias = findViewById(R.id.dAsistencias);
        asistencias.removeAllViews();
        if (d.ultimasAsistencias.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Sin asistencias registradas.");
            vacio.setTextColor(Vistas.color(this, R.color.apagado));
            asistencias.addView(vacio);
        }
        for (Modelos.Acceso a : d.ultimasAsistencias) {
            View fila = LayoutInflater.from(this).inflate(R.layout.item_asistencia, asistencias, false);
            ((TextView) fila.findViewById(R.id.asTitulo)).setText(Formato.diaMes(a.fechaHora) + " · " + Formato.hora(a.fechaHora));
            ((TextView) fila.findViewById(R.id.asTipo)).setText("QR".equals(a.tipo) ? "Check-in" : "Manual");
            asistencias.addView(fila);
        }
    }

    /** "90 kg +4.2%" con el porcentaje en turquesa y más pequeño. */
    private CharSequence conDetalle(String principal, String extra) {
        SpannableStringBuilder sb = new SpannableStringBuilder(principal);
        if (!extra.isEmpty()) {
            int inicio = sb.length();
            sb.append(extra);
            sb.setSpan(new ForegroundColorSpan(Vistas.color(this, R.color.turquesa)), inicio, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new RelativeSizeSpan(0.7f), inicio, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    private void editarRutina() {
        if (detalle == null) return;
        recargarAlVolver = true;
        EditorRutinaActivity.abrir(this, detalle);
    }
}
