package pe.edu.utp.amigep.app.ui.cliente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.BaseFragment;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.Vistas;

/**
 * C4 · Membresías y pagos: plan actual, comparación de planes e historial de pagos.
 * La renovación se paga en recepción (BPM Fig. 5); la app indica qué solicitar.
 */
public class PagosFragment extends BaseFragment {

    private SwipeRefreshLayout refrescar;
    private Modelos.MembresiaCliente datos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pagos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        refrescar = v.findViewById(R.id.refrescar);
        refrescar.setColorSchemeResources(R.color.turquesa);
        refrescar.setProgressBackgroundColorSchemeResource(R.color.tarjeta);
        refrescar.setOnRefreshListener(this::recargar);
        Vistas.insignia(v.findViewById(R.id.insigniaPresencial), "Presencial", Vistas.Tono.GRIS);
        v.findViewById(R.id.btnRenovar).setOnClickListener(x -> {
            if (datos == null) return;
            Modelos.Plan plan = planActual();
            if (plan != null) solicitarEnRecepcion(plan, true);
            else elegirPlan();
        });
        v.findViewById(R.id.btnCambiar).setOnClickListener(x -> elegirPlan());
        recargar();
    }

    @Override
    public void recargar() {
        if (getView() == null) return;
        refrescar.setRefreshing(true);
        llamar(api().membresia(), d -> {
            refrescar.setRefreshing(false);
            datos = d;
            mostrar(d);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            aviso(mensaje);
        });
    }

    private Modelos.Plan planActual() {
        if (datos == null || datos.actual == null) return null;
        for (Modelos.Plan p : datos.planes) if (p.idPlan == datos.actual.idPlan) return p;
        return null;
    }

    private void mostrar(Modelos.MembresiaCliente d) {
        View v = requireView();
        TextView nombre = v.findViewById(R.id.planActual);
        TextView vence = v.findViewById(R.id.planActualVence);
        TextView precio = v.findViewById(R.id.planActualPrecio);
        if (d.actual != null) {
            String estado = "VIGENTE".equals(d.actual.estado) ? "Activo" : "POR_VENCER".equals(d.actual.estado) ? "Por vencer"
                    : "VENCIDA".equals(d.actual.estado) ? "Vencido" : Formato.capital(d.actual.estado);
            nombre.setText(d.actual.plan + " · " + estado);
            vence.setText(("VENCIDA".equals(d.actual.estado) ? "Venció el " : "Vigente hasta ") + Formato.fecha(d.actual.fechaCaducidad));
            precio.setText(Formato.soles(d.actual.precio));
        } else {
            nombre.setText("Sin plan activo");
            vence.setText("Elige un plan y págalo en recepción");
            precio.setText("—");
        }
        TextView proxima = v.findViewById(R.id.proximaRenovacion);
        if (d.proxima != null) {
            proxima.setVisibility(View.VISIBLE);
            proxima.setText("✓ Renovación pagada: " + d.proxima.plan + " desde el " + Formato.fecha(d.proxima.fechaInicio));
        } else {
            proxima.setVisibility(View.GONE);
        }

        LinearLayout planes = v.findViewById(R.id.listaPlanes);
        planes.removeAllViews();
        for (Modelos.Plan p : d.planes) {
            View fila = LayoutInflater.from(requireContext()).inflate(R.layout.item_plan, planes, false);
            boolean esMio = d.actual != null && d.actual.idPlan == p.idPlan;
            ((TextView) fila.findViewById(R.id.planNombre)).setText("Plan " + p.nombre.toLowerCase(Formato.PERU));
            ((TextView) fila.findViewById(R.id.planDescripcion)).setText(p.descripcion);
            ((TextView) fila.findViewById(R.id.planPrecio)).setText(Formato.soles(p.precio));
            if (esMio) {
                Vistas.insignia(fila.findViewById(R.id.planTuyo), "TU PLAN", Vistas.Tono.DORADO);
                fila.setBackgroundResource(R.drawable.bg_tarjeta_seleccion);
            }
            fila.setOnClickListener(x -> solicitarEnRecepcion(p, esMio));
            planes.addView(fila);
        }

        LinearLayout pagos = v.findViewById(R.id.listaPagos);
        pagos.removeAllViews();
        if (d.historialPagos.isEmpty()) {
            TextView vacio = new TextView(requireContext());
            vacio.setText("Aún no tienes pagos registrados.");
            vacio.setTextColor(Vistas.color(requireContext(), R.color.apagado));
            pagos.addView(vacio);
        }
        for (Modelos.Pago p : d.historialPagos) {
            View fila = LayoutInflater.from(requireContext()).inflate(R.layout.item_pago, pagos, false);
            ((TextView) fila.findViewById(R.id.pagoTitulo)).setText("Membresía · " + Formato.mesAnio(p.fechaPago));
            boolean rechazado = "RECHAZADO".equals(p.estado);
            ((TextView) fila.findViewById(R.id.pagoDetalle)).setText(rechazado ? "Pago rechazado · " + Formato.fecha(p.fechaPago)
                    : "Pagado con " + Formato.metodo(p.metodoPago) + " · plan " + p.plan);
            TextView monto = fila.findViewById(R.id.pagoMonto);
            monto.setText(Formato.soles(p.monto));
            monto.setTextColor(Vistas.color(requireContext(), rechazado ? R.color.rojo : R.color.texto));
            pagos.addView(fila);
        }

        Modelos.Plan actual = planActual();
        ((Button) v.findViewById(R.id.btnRenovar)).setText(actual != null ? "Renovar · " + Formato.soles(actual.precio) : "Elegir plan");
    }

    private void elegirPlan() {
        if (datos == null || datos.planes.isEmpty()) return;
        String[] nombres = new String[datos.planes.size()];
        for (int i = 0; i < nombres.length; i++) {
            Modelos.Plan p = datos.planes.get(i);
            nombres[i] = "Plan " + p.nombre + " · " + Formato.soles(p.precio) + " / " + p.duracionDias + " días";
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Cambiar plan")
                .setItems(nombres, (d, i) -> solicitarEnRecepcion(datos.planes.get(i), false))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** El sistema no procesa pagos en línea: el cliente paga en recepción y el administrador lo registra. */
    private void solicitarEnRecepcion(Modelos.Plan plan, boolean renovacion) {
        new AlertDialog.Builder(requireContext())
                .setTitle(renovacion ? "Renovar plan " + plan.nombre : "Plan " + plan.nombre)
                .setMessage("Acércate a recepción y solicita: \"" + (renovacion ? "Renovar" : "Cambiar a") + " plan "
                        + plan.nombre + " · " + Formato.soles(plan.precio) + "\".\n\nPuedes pagar en efectivo, tarjeta o Yape/Plin. "
                        + "Tu membresía se actualizará al confirmar el pago y recibirás una notificación.")
                .setPositiveButton("Entendido", null)
                .show();
    }
}
