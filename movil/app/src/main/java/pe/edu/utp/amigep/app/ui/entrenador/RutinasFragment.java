package pe.edu.utp.amigep.app.ui.entrenador;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.BaseFragment;
import pe.edu.utp.amigep.app.ui.ListaAdapter;
import pe.edu.utp.amigep.app.ui.ReporteActivity;
import pe.edu.utp.amigep.app.ui.Vistas;

/** Rutinas de los clientes asignados (RF-08) con su cumplimiento; acceso al reporte RP-05. */
public class RutinasFragment extends BaseFragment {

    private SwipeRefreshLayout refrescar;
    private ListaAdapter<Modelos.Rutina> adapter;
    private List<Modelos.Rutina> todas = new ArrayList<>();
    private boolean soloActivas = true;
    private TextView chipActivas;
    private TextView chipTodas;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_entrenador, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        ((TextView) v.findViewById(R.id.titulo)).setText("Rutinas");
        v.findViewById(R.id.btnReporte).setOnClickListener(x ->
                ReporteActivity.abrir(requireContext(), "RP-05", "Rutinas asignadas (RP-05)", null));
        LinearLayout filtros = v.findViewById(R.id.filtros);
        chipActivas = Vistas.chip(requireContext(), filtros, "Activas", true, x -> filtrar(true));
        chipTodas = Vistas.chip(requireContext(), filtros, "Historial completo", false, x -> filtrar(false));
        refrescar = v.findViewById(R.id.refrescar);
        refrescar.setColorSchemeResources(R.color.turquesa);
        refrescar.setProgressBackgroundColorSchemeResource(R.color.tarjeta);
        refrescar.setOnRefreshListener(this::recargar);
        RecyclerView lista = v.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ListaAdapter<>(R.layout.item_rutina, this::enlazar, r -> {
            recargarAlVolver = true;
            DetalleClienteActivity.abrir(requireContext(), r.idCliente);
        });
        lista.setAdapter(adapter);
        recargar();
    }

    @Override
    public void recargar() {
        if (getView() == null) return;
        refrescar.setRefreshing(true);
        llamar(api().rutinasEntrenador(), lista -> {
            refrescar.setRefreshing(false);
            todas = lista;
            filtrar(soloActivas);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            aviso(mensaje);
        });
    }

    private void filtrar(boolean activas) {
        soloActivas = activas;
        Vistas.marcarChip(chipActivas, activas);
        Vistas.marcarChip(chipTodas, !activas);
        List<Modelos.Rutina> visibles = new ArrayList<>();
        int conteoActivas = 0;
        for (Modelos.Rutina r : todas) {
            if (r.activo) conteoActivas++;
            if (!activas || r.activo) visibles.add(r);
        }
        adapter.mostrar(visibles);
        ((TextView) requireView().findViewById(R.id.subtitulo)).setText(conteoActivas + " activas · " + todas.size() + " en total");
        TextView vacio = requireView().findViewById(R.id.vacio);
        vacio.setText("No hay rutinas para mostrar.");
        vacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void enlazar(View fila, Modelos.Rutina r, int pos) {
        ((TextView) fila.findViewById(R.id.ruNombre)).setText(r.nombre + " · " + r.cliente.split(" ")[0]);
        Vistas.insigniaEstado(fila.findViewById(R.id.ruEstado), r.estado);
        ((TextView) fila.findViewById(R.id.ruDetalle)).setText(r.cliente + "\nSemana " + r.semanaActual + " de " + r.semanasTotales
                + " · " + r.ritmo + " · " + ("IA".equals(r.origen) ? "Generada por IA" : "Entrenador"));
        ((ProgressBar) fila.findViewById(R.id.ruBarra)).setProgress(r.cumplimientoPct);
        ((TextView) fila.findViewById(R.id.ruCumplimiento)).setText("Cumplimiento " + r.cumplimientoPct + "% · "
                + r.sesionesCompletadas + " sesiones");
    }
}
