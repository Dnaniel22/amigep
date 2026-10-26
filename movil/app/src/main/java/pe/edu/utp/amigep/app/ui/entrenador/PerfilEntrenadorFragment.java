package pe.edu.utp.amigep.app.ui.entrenador;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.ui.BaseFragment;
import pe.edu.utp.amigep.app.ui.Dialogos;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.ReporteActivity;
import pe.edu.utp.amigep.app.ui.Vistas;

/** Perfil del entrenador: indicadores, reportes RP-04 / RP-05, contraseña y cierre de sesión. */
public class PerfilEntrenadorFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil_entrenador, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        opcion(v, R.id.peReporteAsistencia, R.drawable.ic_calendario, "Asistencia de mis clientes (RP-04)", x -> {
            HashMap<String, String> f = new HashMap<>();
            f.put("desde", Formato.haceDiasIso(29));
            f.put("hasta", Formato.hoyIso());
            ReporteActivity.abrir(requireContext(), "RP-04", "Asistencia (RP-04)", f);
        });
        opcion(v, R.id.peReporteRutinas, R.drawable.ic_lista, "Rutinas asignadas (RP-05)", x ->
                ReporteActivity.abrir(requireContext(), "RP-05", "Rutinas asignadas (RP-05)", null));
        opcion(v, R.id.peSeguridad, R.drawable.ic_escudo, "Seguridad y contraseña", x -> Dialogos.cambiarContrasena(requireActivity()));
        View salir = opcion(v, R.id.peSalir, R.drawable.ic_salir, "Cerrar sesión", x -> Dialogos.cerrarSesion(requireActivity()));
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
        llamar(api().perfilEntrenador(), p -> {
            View v = requireView();
            Vistas.avatar(v.findViewById(R.id.peAvatar), p.iniciales, p.nombre + p.apellido);
            ((TextView) v.findViewById(R.id.peNombre)).setText(p.nombre + " " + p.apellido);
            ((TextView) v.findViewById(R.id.peEspecialidad)).setText(p.especialidad == null ? "Entrenador" : "Entrenador · " + p.especialidad);
            ((TextView) v.findViewById(R.id.peEmail)).setText(p.email);
            ((TextView) v.findViewById(R.id.peClientes)).setText(String.valueOf(p.clientesAsignados));
            ((TextView) v.findViewById(R.id.peRutinas)).setText(String.valueOf(p.rutinasActivas));
        });
    }
}
