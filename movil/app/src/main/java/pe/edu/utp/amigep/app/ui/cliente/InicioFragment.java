package pe.edu.utp.amigep.app.ui.cliente;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.BaseFragment;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.NotificacionesActivity;
import pe.edu.utp.amigep.app.ui.QrUtil;
import pe.edu.utp.amigep.app.ui.Vistas;

/** C2 · Inicio del cliente: QR personal (RF-17), plan vigente (RF-04) y sesión de hoy (RF-09). */
public class InicioFragment extends BaseFragment {

    private SwipeRefreshLayout refrescar;
    private Modelos.Inicio datos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        refrescar = v.findViewById(R.id.refrescar);
        refrescar.setColorSchemeResources(R.color.turquesa);
        refrescar.setProgressBackgroundColorSchemeResource(R.color.tarjeta);
        refrescar.setOnRefreshListener(this::recargar);

        ClienteActivity actividad = (ClienteActivity) requireActivity();
        acceso(v, R.id.tileProgreso, R.drawable.ic_progreso, "Mi progreso", () -> actividad.mostrar(ClienteActivity.PERFIL));
        acceso(v, R.id.tileRutina, R.drawable.ic_entreno, "Mi rutina", () -> actividad.mostrar(ClienteActivity.ENTRENO));
        acceso(v, R.id.tilePagos, R.drawable.ic_pagos, "Pagos", () -> actividad.mostrar(ClienteActivity.PAGOS));
        acceso(v, R.id.tileDieta, R.drawable.ic_dieta, "Dieta", () -> actividad.mostrar(ClienteActivity.GUIA));
        v.findViewById(R.id.insigniaIa).setOnClickListener(x -> actividad.mostrar(ClienteActivity.ENTRENO));
        v.findViewById(R.id.btnNotificaciones).setOnClickListener(x -> {
            recargarAlVolver = true;
            startActivity(new Intent(requireContext(), NotificacionesActivity.class));
        });
        v.findViewById(R.id.tarjetaSesion).setOnClickListener(x -> {
            if (datos != null && datos.sesionHoy != null) {
                recargarAlVolver = true;
                startActivity(new Intent(requireContext(), RutinaActivity.class));
            } else {
                actividad.mostrar(ClienteActivity.ENTRENO);
            }
        });
        recargar();
    }

    private void acceso(View raiz, int id, int icono, String texto, Runnable accion) {
        View tile = raiz.findViewById(id);
        ((ImageView) tile.findViewById(R.id.icono)).setImageResource(icono);
        ((TextView) tile.findViewById(R.id.texto)).setText(texto);
        tile.setOnClickListener(x -> accion.run());
    }

    @Override
    public void recargar() {
        if (getView() == null) return;
        refrescar.setRefreshing(true);
        llamar(api().inicio(), d -> {
            refrescar.setRefreshing(false);
            datos = d;
            mostrar(d);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            aviso(mensaje);
        });
    }

    private void mostrar(Modelos.Inicio d) {
        View v = requireView();
        Vistas.avatar(v.findViewById(R.id.avatar), d.iniciales, d.nombre + d.apellido);
        ((TextView) v.findViewById(R.id.nombreCompleto)).setText(d.nombre + " " + d.apellido);

        Bitmap qr = QrUtil.generar(d.codigoQR, 600, Vistas.color(requireContext(), R.color.negro_qr),
                Vistas.color(requireContext(), R.color.crema));
        ((ImageView) v.findViewById(R.id.qr)).setImageBitmap(qr);

        TextView contador = v.findViewById(R.id.contadorNotificaciones);
        contador.setVisibility(d.notificacionesNoLeidas > 0 ? View.VISIBLE : View.GONE);
        contador.setText(String.valueOf(Math.min(d.notificacionesNoLeidas, 9)));

        TextView plan = v.findViewById(R.id.planNombre);
        TextView vigencia = v.findViewById(R.id.planVigencia);
        TextView estado = v.findViewById(R.id.planEstado);
        Modelos.Membresia m = d.membresia;
        if (m == null) {
            plan.setText("Sin membresía");
            vigencia.setText("Acércate a recepción para activar tu plan");
            estado.setText("");
        } else {
            plan.setText("Plan " + m.plan);
            switch (m.estado) {
                case "VIGENTE":
                    vigencia.setText("Vigente hasta " + Formato.fecha(m.fechaCaducidad));
                    estilarEstado(estado, "● ACTIVO", R.color.turquesa);
                    break;
                case "POR_VENCER":
                    vigencia.setText(m.diasRestantes == 0 ? "Vence hoy" : "Vence en " + m.diasRestantes + " días · " + Formato.fecha(m.fechaCaducidad));
                    estilarEstado(estado, "● POR VENCER", R.color.dorado);
                    break;
                case "VENCIDA":
                    vigencia.setText("Venció el " + Formato.fecha(m.fechaCaducidad) + " · renueva en recepción");
                    estilarEstado(estado, "● VENCIDO", R.color.rojo);
                    break;
                default:
                    vigencia.setText(Formato.capital(m.estado));
                    estilarEstado(estado, "● " + m.estado, R.color.apagado);
            }
        }

        TextView titulo = v.findViewById(R.id.sesionTitulo);
        TextView detalle = v.findViewById(R.id.sesionDetalle);
        TextView estadoSesion = v.findViewById(R.id.sesionEstado);
        if (d.sesionHoy == null) {
            titulo.setText("Sin rutina activa");
            detalle.setText("Genera tu rutina personalizada con IA");
            Vistas.insignia(estadoSesion, "Crear", Vistas.Tono.VERDE);
        } else {
            titulo.setText(d.sesionHoy.titulo);
            detalle.setText(d.sesionHoy.detalle);
            if (d.sesionHoy.completada) Vistas.insignia(estadoSesion, "Completada", Vistas.Tono.GRIS);
            else Vistas.insignia(estadoSesion, "Por hacer", Vistas.Tono.VERDE);
        }
    }

    private void estilarEstado(TextView vista, String texto, int color) {
        vista.setText(texto);
        vista.setTextColor(Vistas.color(requireContext(), color));
    }
}
