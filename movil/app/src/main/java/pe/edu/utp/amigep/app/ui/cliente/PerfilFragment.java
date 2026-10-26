package pe.edu.utp.amigep.app.ui.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.BaseFragment;
import pe.edu.utp.amigep.app.ui.Dialogos;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.NotificacionesActivity;
import pe.edu.utp.amigep.app.ui.ReporteActivity;
import pe.edu.utp.amigep.app.ui.Vistas;

/** C6 · Perfil y progreso: datos personales, entrenador asignado, estadísticas (RF-14) y configuración. */
public class PerfilFragment extends BaseFragment {

    private SwipeRefreshLayout refrescar;
    private Modelos.PerfilCliente perfil;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        refrescar = v.findViewById(R.id.refrescar);
        refrescar.setColorSchemeResources(R.color.turquesa);
        refrescar.setProgressBackgroundColorSchemeResource(R.color.tarjeta);
        refrescar.setOnRefreshListener(this::recargar);
        v.findViewById(R.id.btnEditar).setOnClickListener(x -> {
            recargarAlVolver = true;
            startActivity(new Intent(requireContext(), EditarPerfilActivity.class));
        });
        v.findViewById(R.id.btnMedicion).setOnClickListener(x -> registrarMedicion());
        v.findViewById(R.id.tarjetaEntrenador).setOnClickListener(x -> {
            if (perfil != null && perfil.entrenador != null) {
                aviso(perfil.entrenador.nombre + " · " + (perfil.entrenador.especialidad == null ? "Entrenador" : perfil.entrenador.especialidad));
            }
        });
        opcion(v, R.id.opcionNotificaciones, R.drawable.ic_campana, "Notificaciones", x -> {
            recargarAlVolver = true;
            startActivity(new Intent(requireContext(), NotificacionesActivity.class));
        });
        opcion(v, R.id.opcionReportes, R.drawable.ic_grafico, "Mis reportes", x -> elegirReporte());
        opcion(v, R.id.opcionSeguridad, R.drawable.ic_escudo, "Seguridad y contraseña", x -> Dialogos.cambiarContrasena(requireActivity()));
        View salir = opcion(v, R.id.opcionSalir, R.drawable.ic_salir, "Cerrar sesión", x -> Dialogos.cerrarSesion(requireActivity()));
        ((TextView) salir.findViewById(R.id.texto)).setTextColor(Vistas.color(requireContext(), R.color.rojo));
        ((ImageView) salir.findViewById(R.id.icono)).setColorFilter(Vistas.color(requireContext(), R.color.rojo));
        salir.findViewById(R.id.flecha).setVisibility(View.GONE);
        recargar();
    }

    private View opcion(View raiz, int id, int icono, String texto, View.OnClickListener clic) {
        View fila = raiz.findViewById(id);
        ((ImageView) fila.findViewById(R.id.icono)).setImageResource(icono);
        ((TextView) fila.findViewById(R.id.texto)).setText(texto);
        fila.setOnClickListener(clic);
        return fila;
    }

    @Override
    public void recargar() {
        if (getView() == null) return;
        refrescar.setRefreshing(true);
        llamar(api().perfil(), p -> {
            refrescar.setRefreshing(false);
            perfil = p;
            mostrar(p);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            aviso(mensaje);
        });
        llamar(api().notificaciones(), lista -> {
            int noLeidas = 0;
            for (Modelos.Notificacion n : lista) if (!n.leido) noLeidas++;
            TextView extra = requireView().findViewById(R.id.opcionNotificaciones).findViewById(R.id.extra);
            if (noLeidas > 0) Vistas.insignia(extra, noLeidas + " nuevas", Vistas.Tono.ROJO);
            else extra.setVisibility(View.GONE);
        });
    }

    private void mostrar(Modelos.PerfilCliente p) {
        View v = requireView();
        Vistas.avatar(v.findViewById(R.id.avatar), p.iniciales, p.nombre + p.apellido);
        ((TextView) v.findViewById(R.id.nombre)).setText(p.nombre + " " + p.apellido);
        ((TextView) v.findViewById(R.id.miembro)).setText(p.plan != null ? "Miembro " + p.plan + " · " + p.codigo : "Sin membresía activa");
        ((TextView) v.findViewById(R.id.email)).setText(p.email);
        ((TextView) v.findViewById(R.id.telefono)).setText(p.telefono != null ? "+51 " + p.telefono : "Sin teléfono");
        ((TextView) v.findViewById(R.id.objetivo)).setText(p.objetivo != null ? p.objetivo : "—");
        ((TextView) v.findViewById(R.id.nivel)).setText(Formato.nivel(p.nivel));
        ((TextView) v.findViewById(R.id.peso)).setText(p.peso != null ? Formato.numero(p.peso) + " kg" : "—");
        ((TextView) v.findViewById(R.id.altura)).setText(p.altura != null ? String.format(Formato.PERU, "%.2f m", p.altura) : "—");

        View entrenador = v.findViewById(R.id.tarjetaEntrenador);
        if (p.entrenador != null) {
            entrenador.setVisibility(View.VISIBLE);
            Vistas.avatar(v.findViewById(R.id.entrenadorAvatar), p.entrenador.iniciales, p.entrenador.nombre);
            ((TextView) v.findViewById(R.id.entrenadorNombre)).setText(p.entrenador.nombre);
            ((TextView) v.findViewById(R.id.entrenadorRol)).setText("Entrenador de confianza");
        } else {
            entrenador.setVisibility(View.GONE);
        }

        Modelos.Estadisticas e = p.estadisticas;
        TextView evolucion = v.findViewById(R.id.evolucionPeso);
        evolucion.setText(e.variacionPesoPct != null ? Formato.porcentajeConSigno(e.variacionPesoPct) : "Sin datos");
        ((ProgressBar) v.findViewById(R.id.barraPeso)).setProgress(
                e.variacionPesoPct == null ? 0 : (int) Math.min(100, Math.abs(e.variacionPesoPct) * 10));
        ((TextView) v.findViewById(R.id.asistencia)).setText(e.asistencia30Pct + "% · " + e.asistencias30 + " visitas");
        ((ProgressBar) v.findViewById(R.id.barraAsistencia)).setProgress(e.asistencia30Pct);
        ((TextView) v.findViewById(R.id.cumplimiento)).setText(e.cumplimientoRutinasPct + "%");
        ((ProgressBar) v.findViewById(R.id.barraCumplimiento)).setProgress(e.cumplimientoRutinasPct);
    }

    private void registrarMedicion() {
        View vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialogo_campo, null);
        ((TextView) vista.findViewById(R.id.dlgTexto)).setText("Registra tu peso actual para seguir tu evolución.");
        EditText peso = vista.findViewById(R.id.dlgCampo);
        EditText grasa = vista.findViewById(R.id.dlgCampo2);
        peso.setHint("Peso (kg)");
        peso.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        grasa.setHint("Grasa corporal % (opcional)");
        grasa.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        grasa.setVisibility(View.VISIBLE);
        AlertDialog d = new AlertDialog.Builder(requireContext())
                .setTitle("Registrar medición")
                .setView(vista)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            Modelos.RegistrarProgresoRequest req = new Modelos.RegistrarProgresoRequest();
            req.peso = Formato.aDouble(peso.getText().toString());
            req.grasaCorporal = Formato.aDouble(grasa.getText().toString());
            if (req.peso == null || req.peso < 25 || req.peso > 350) {
                peso.setError("Peso no válido");
                return;
            }
            llamar(api().registrarProgreso(req), est -> {
                d.dismiss();
                aviso("Medición registrada");
                recargar();
            }, (mensaje, campos) -> peso.setError(mensaje));
        }));
        d.show();
    }

    /** RP-06, RP-07 y RP-08 (vista del cliente). */
    private void elegirReporte() {
        String[] codigos = {"RP-06", "RP-07", "RP-08"};
        String[] nombres = {"Mi progreso (RP-06)", "Seguimiento alimenticio (RP-07)", "Resumen de mi dashboard (RP-08)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Mis reportes")
                .setItems(nombres, (d, i) -> ReporteActivity.abrir(requireContext(), codigos[i], nombres[i], null))
                .show();
    }
}
