package pe.edu.utp.amigep.app.ui.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.HashMap;
import java.util.Map;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.BaseFragment;
import pe.edu.utp.amigep.app.ui.Formato;

/** C5 · Alimentación IA: comidas recomendadas, macros del día, sugerencia de la IA y registro de comidas. */
public class GuiaFragment extends BaseFragment {

    private SwipeRefreshLayout refrescar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guia, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        refrescar = v.findViewById(R.id.refrescar);
        refrescar.setColorSchemeResources(R.color.turquesa);
        refrescar.setProgressBackgroundColorSchemeResource(R.color.tarjeta);
        refrescar.setOnRefreshListener(this::recargar);
        v.findViewById(R.id.btnRegistrarComida).setOnClickListener(x -> {
            recargarAlVolver = true;
            startActivity(new Intent(requireContext(), RegistrarComidaActivity.class));
        });
        v.findViewById(R.id.btnGenerarPlan).setOnClickListener(x -> generarPlan());
        v.findViewById(R.id.btnRegenerar).setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Actualizar plan nutricional")
                .setMessage("La IA recalculará tu meta calórica y tus comidas con tu peso y objetivo actuales.")
                .setPositiveButton("Actualizar", (d, w) -> generarPlan())
                .setNegativeButton("Cancelar", null)
                .show());
        recargar();
    }

    @Override
    public void recargar() {
        if (getView() == null) return;
        refrescar.setRefreshing(true);
        llamar(api().alimentacion(), a -> {
            refrescar.setRefreshing(false);
            mostrar(a);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            aviso(mensaje);
        });
    }

    private void generarPlan() {
        refrescar.setRefreshing(true);
        Map<String, String> cuerpo = new HashMap<>();
        llamar(api().generarPlanAlimenticio(cuerpo), a -> {
            refrescar.setRefreshing(false);
            aviso("Plan nutricional actualizado: " + a.plan.caloriasObjetivo + " kcal/día");
            mostrar(a);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            aviso(mensaje);
        });
    }

    private void mostrar(Modelos.Alimentacion a) {
        View v = requireView();
        boolean conPlan = a.plan != null && a.plan.contenido != null;
        v.findViewById(R.id.sinPlan).setVisibility(conPlan ? View.GONE : View.VISIBLE);

        LinearLayout comidas = v.findViewById(R.id.listaComidas);
        comidas.removeAllViews();
        if (conPlan) {
            for (Modelos.ComidaPlan c : a.plan.contenido.comidas) {
                View fila = LayoutInflater.from(requireContext()).inflate(R.layout.item_comida, comidas, false);
                ((TextView) fila.findViewById(R.id.comidaNombre)).setText(c.nombre);
                ((TextView) fila.findViewById(R.id.comidaDescripcion)).setText(c.descripcion);
                ((TextView) fila.findViewById(R.id.comidaKcal)).setText(c.calorias + " kcal");
                comidas.addView(fila);
            }
            Modelos.ContenidoPlan cp = a.plan.contenido;
            ((TextView) v.findViewById(R.id.objetivoNutricional)).setText(
                    cp.objetivoEtiqueta != null ? cp.objetivoEtiqueta : a.plan.objetivo);
            String meta = cp.metaSemanalKg == null || cp.metaSemanalKg == 0 ? "mantener peso"
                    : (cp.metaSemanalKg > 0 ? "+" : "") + Formato.numero(cp.metaSemanalKg) + " kg";
            ((TextView) v.findViewById(R.id.objetivoDetalle)).setText(
                    Formato.numero(a.plan.caloriasObjetivo) + " kcal/día · Meta semanal: " + meta);
        } else {
            ((TextView) v.findViewById(R.id.objetivoNutricional)).setText("Sin plan nutricional");
            ((TextView) v.findViewById(R.id.objetivoDetalle)).setText("Genera tu plan con IA");
        }

        Modelos.ResumenNutricional hoy = a.hoy;
        ((TextView) v.findViewById(R.id.pctProteinas)).setText(hoy.pctProteinas + "%");
        ((TextView) v.findViewById(R.id.pctCarbohidratos)).setText(hoy.pctCarbohidratos + "%");
        ((TextView) v.findViewById(R.id.pctGrasas)).setText(hoy.pctGrasas + "%");
        ((ProgressBar) v.findViewById(R.id.barraCalorias)).setProgress(Math.min(100, hoy.pctMetaCalorica));
        ((TextView) v.findViewById(R.id.caloriasHoy)).setText(hoy.caloriasObjetivo == null
                ? hoy.calorias + " kcal consumidas hoy"
                : Formato.numero(hoy.calorias) + " de " + Formato.numero(hoy.caloriasObjetivo) + " kcal · " + hoy.pctMetaCalorica + "% de tu meta");
        ((TextView) v.findViewById(R.id.sugerencia)).setText(a.sugerencia);

        LinearLayout registros = v.findViewById(R.id.listaRegistros);
        registros.removeAllViews();
        v.findViewById(R.id.tituloRegistros).setVisibility(a.registrosHoy.isEmpty() ? View.GONE : View.VISIBLE);
        for (Modelos.RegistroComida r : a.registrosHoy) {
            View fila = LayoutInflater.from(requireContext()).inflate(R.layout.item_comida, registros, false);
            ((TextView) fila.findViewById(R.id.comidaNombre)).setText(Formato.capital(r.tipoComida));
            ((TextView) fila.findViewById(R.id.comidaDescripcion)).setText(r.descripcion);
            ((TextView) fila.findViewById(R.id.comidaKcal)).setText((r.calorias == null ? 0 : r.calorias) + " kcal");
            registros.addView(fila);
        }
    }
}
