package pe.edu.utp.amigep.app.ui.entrenador;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.BaseFragment;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.ListaAdapter;
import pe.edu.utp.amigep.app.ui.ReporteActivity;
import pe.edu.utp.amigep.app.ui.Vistas;

/** Agenda: calendario de asistencias de los clientes asignados (RF-06); acceso al reporte RP-04. */
public class AgendaFragment extends BaseFragment {

    private static final int[] RANGOS = {0, 6, 29};
    private static final String[] ETIQUETAS = {"Hoy", "Últimos 7 días", "Últimos 30 días"};

    private SwipeRefreshLayout refrescar;
    private ListaAdapter<Modelos.Acceso> adapter;
    private final TextView[] chips = new TextView[RANGOS.length];
    private int rango = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_entrenador, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        ((TextView) v.findViewById(R.id.titulo)).setText("Agenda de asistencias");
        v.findViewById(R.id.btnReporte).setOnClickListener(x -> {
            HashMap<String, String> filtros = new HashMap<>();
            filtros.put("desde", Formato.haceDiasIso(RANGOS[rango]));
            filtros.put("hasta", Formato.hoyIso());
            ReporteActivity.abrir(requireContext(), "RP-04", "Asistencia por cliente (RP-04)", filtros);
        });
        LinearLayout filtros = v.findViewById(R.id.filtros);
        for (int i = 0; i < RANGOS.length; i++) {
            final int indice = i;
            chips[i] = Vistas.chip(requireContext(), filtros, ETIQUETAS[i], i == rango, x -> {
                rango = indice;
                recargar();
            });
        }
        refrescar = v.findViewById(R.id.refrescar);
        refrescar.setColorSchemeResources(R.color.turquesa);
        refrescar.setProgressBackgroundColorSchemeResource(R.color.tarjeta);
        refrescar.setOnRefreshListener(this::recargar);
        RecyclerView lista = v.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ListaAdapter<>(R.layout.item_asistencia, this::enlazar, a -> DetalleClienteActivity.abrir(requireContext(), a.idCliente));
        lista.setAdapter(adapter);
        recargar();
    }

    @Override
    public void recargar() {
        if (getView() == null) return;
        for (int i = 0; i < chips.length; i++) Vistas.marcarChip(chips[i], i == rango);
        refrescar.setRefreshing(true);
        llamar(api().agenda(Formato.haceDiasIso(RANGOS[rango]), Formato.hoyIso()), lista -> {
            refrescar.setRefreshing(false);
            adapter.mostrar(lista);
            Set<Integer> clientes = new HashSet<>();
            for (Modelos.Acceso a : lista) clientes.add(a.idCliente);
            ((TextView) requireView().findViewById(R.id.subtitulo)).setText(lista.size() + " ingresos · " + clientes.size() + " clientes");
            TextView vacio = requireView().findViewById(R.id.vacio);
            vacio.setText("Sin asistencias de tus clientes en este periodo.");
            vacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            aviso(mensaje);
        });
    }

    private void enlazar(View fila, Modelos.Acceso a, int pos) {
        ((TextView) fila.findViewById(R.id.asTitulo)).setText(a.cliente);
        TextView detalle = fila.findViewById(R.id.asDetalle);
        detalle.setVisibility(View.VISIBLE);
        detalle.setText(Formato.diaMes(a.fechaHora) + " · " + Formato.hora(a.fechaHora));
        ((TextView) fila.findViewById(R.id.asTipo)).setText("QR".equals(a.tipo) ? "Check-in" : "Manual");
    }
}
