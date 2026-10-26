package pe.edu.utp.amigep.app.ui.entrenador;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.datos.Sesion;
import pe.edu.utp.amigep.app.ui.BaseFragment;
import pe.edu.utp.amigep.app.ui.ListaAdapter;
import pe.edu.utp.amigep.app.ui.Vistas;

/** E1 · Mis clientes: búsqueda y filtros por estado de rutina (vigente, por finalizar, pendiente). */
public class ClientesFragment extends BaseFragment {

    private static final String[][] FILTROS = {{"", "Todos"}, {"VIGENTE", "Rutina vigente"}, {"POR_FINALIZAR", "Por finalizar"}, {"PENDIENTE", "Pendiente"}};

    private SwipeRefreshLayout refrescar;
    private ListaAdapter<Modelos.ClienteAsignado> adapter;
    private final TextView[] chips = new TextView[FILTROS.length];
    private String filtro = "";
    private EditText buscar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clientes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        Sesion s = Sesion.de(requireContext());
        Vistas.avatar(v.findViewById(R.id.avatar), s.iniciales(), s.nombre() + s.apellido());
        ((TextView) v.findViewById(R.id.nombreEntrenador)).setText(s.nombre() + " " + s.apellido().split(" ")[0]);
        v.findViewById(R.id.btnAgenda).setOnClickListener(x -> ((EntrenadorActivity) requireActivity()).mostrar(EntrenadorActivity.AGENDA));

        LinearLayout filtros = v.findViewById(R.id.filtros);
        for (int i = 0; i < FILTROS.length; i++) {
            String valor = FILTROS[i][0];
            chips[i] = Vistas.chip(requireContext(), filtros, FILTROS[i][1], i == 0, x -> {
                filtro = valor;
                recargar();
            });
        }
        buscar = v.findViewById(R.id.buscar);
        buscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s1, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s1, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { recargar(); }
        });

        refrescar = v.findViewById(R.id.refrescar);
        refrescar.setColorSchemeResources(R.color.turquesa);
        refrescar.setProgressBackgroundColorSchemeResource(R.color.tarjeta);
        refrescar.setOnRefreshListener(this::recargar);
        RecyclerView lista = v.findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ListaAdapter<>(R.layout.item_cliente_asignado, this::enlazar, c -> {
            recargarAlVolver = true;
            DetalleClienteActivity.abrir(requireContext(), c.idCliente);
        });
        lista.setAdapter(adapter);
        recargar();
    }

    @Override
    public void recargar() {
        if (getView() == null) return;
        refrescar.setRefreshing(true);
        llamar(api().misClientes(buscar.getText().toString().trim(), filtro), r -> {
            refrescar.setRefreshing(false);
            adapter.mostrar(r.clientes);
            chips[0].setText("Todos (" + r.total + ")");
            chips[1].setText("Rutina vigente (" + r.vigentes + ")");
            chips[2].setText("Por finalizar (" + r.porFinalizar + ")");
            chips[3].setText("Pendiente (" + r.pendientes + ")");
            for (int i = 0; i < FILTROS.length; i++) Vistas.marcarChip(chips[i], FILTROS[i][0].equals(filtro));
            TextView vacio = requireView().findViewById(R.id.vacio);
            vacio.setText(r.total == 0 ? "Aún no tienes clientes asignados." : "Ningún cliente coincide con el filtro.");
            vacio.setVisibility(r.clientes.isEmpty() ? View.VISIBLE : View.GONE);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            aviso(mensaje);
        });
    }

    private void enlazar(View fila, Modelos.ClienteAsignado c, int pos) {
        Vistas.avatar(fila.findViewById(R.id.caAvatar), c.iniciales, c.nombre + c.apellido);
        ((TextView) fila.findViewById(R.id.caNombre)).setText(c.nombre.split(" ")[0] + " " + c.apellido);
        ((TextView) fila.findViewById(R.id.caDetalle)).setText(c.detalle);
        Vistas.insigniaEstado(fila.findViewById(R.id.caEstado), c.estadoRutina);
    }
}
