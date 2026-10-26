package pe.edu.utp.amigep.app.ui.cliente;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.ui.BaseFragment;
import pe.edu.utp.amigep.app.ui.Dialogos;
import pe.edu.utp.amigep.app.ui.Formato;
import pe.edu.utp.amigep.app.ui.Vistas;

/** C3 · Entrenamiento IA: rutina activa, objetivo, ejercicios recomendados y generación personalizada. */
public class EntrenoFragment extends BaseFragment {

    /** Etiqueta del chip → objetivo que entiende el backend. */
    private static final Map<String, String> OBJETIVOS = new LinkedHashMap<>();

    static {
        OBJETIVOS.put("Ganar masa muscular", "Masa muscular");
        OBJETIVOS.put("Pérdida de grasa", "Pérdida de grasa");
        OBJETIVOS.put("Fuerza", "Fuerza");
        OBJETIVOS.put("Resistencia", "Resistencia");
    }

    private static final String[] NIVELES = {"PRINCIPIANTE", "INTERMEDIO", "AVANZADO"};
    private static final String[] NIVELES_TEXTO = {"Principiante", "Intermedio", "Avanzado"};

    private SwipeRefreshLayout refrescar;
    private final List<TextView> chips = new ArrayList<>();
    private String objetivo = "Masa muscular";
    private int sesionesPorSemana = 4;
    private Modelos.Rutina activa;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entreno, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        refrescar = v.findViewById(R.id.refrescar);
        refrescar.setColorSchemeResources(R.color.turquesa);
        refrescar.setProgressBackgroundColorSchemeResource(R.color.tarjeta);
        refrescar.setOnRefreshListener(this::recargar);

        LinearLayout fila1 = v.findViewById(R.id.filaObjetivos1);
        LinearLayout fila2 = v.findViewById(R.id.filaObjetivos2);
        int i = 0;
        for (Map.Entry<String, String> e : OBJETIVOS.entrySet()) {
            String valor = e.getValue();
            TextView chip = Vistas.chip(requireContext(), i++ < 2 ? fila1 : fila2, e.getKey(), false, x -> seleccionarObjetivo(valor));
            chip.setTag(valor);
            chips.add(chip);
        }
        v.findViewById(R.id.btnGenerar).setOnClickListener(x -> elegirSesionesYGenerar());
        v.findViewById(R.id.tarjetaRutina).setOnClickListener(x -> abrirRutina());
        recargar();
    }

    @Override
    public void recargar() {
        if (getView() == null) return;
        refrescar.setRefreshing(true);
        llamar(api().rutinas(), r -> {
            refrescar.setRefreshing(false);
            mostrarRutinas(r);
        }, (mensaje, campos) -> {
            refrescar.setRefreshing(false);
            aviso(mensaje);
        });
    }

    private void mostrarRutinas(Modelos.RutinasCliente r) {
        View v = requireView();
        activa = r.activa;
        TextView titulo = v.findViewById(R.id.rutinaTitulo);
        TextView detalle = v.findViewById(R.id.rutinaDetalle);
        ProgressBar barra = v.findViewById(R.id.rutinaBarra);
        TextView ver = v.findViewById(R.id.rutinaVer);
        if (activa != null) {
            titulo.setText("Rutina activa · " + activa.nombre);
            detalle.setText("Semana " + activa.semanaActual + " de " + activa.semanasTotales + " · Nivel "
                    + Formato.nivel(activa.nivel).toLowerCase(Formato.PERU) + " · "
                    + ("IA".equals(activa.origen) ? "Generada por IA" : "Entrenador: " + activa.entrenador));
            barra.setProgress(Math.round(activa.semanaActual * 100f / Math.max(1, activa.semanasTotales)));
            barra.setVisibility(View.VISIBLE);
            ver.setVisibility(View.VISIBLE);
            if (activa.contenido != null && activa.contenido.sesionesPorSemana != null) {
                sesionesPorSemana = activa.contenido.sesionesPorSemana;
            }
            objetivo = activa.objetivo;
        } else {
            titulo.setText("Aún no tienes una rutina activa");
            detalle.setText("Elige tu objetivo y genera una rutina personalizada con IA");
            barra.setVisibility(View.GONE);
            ver.setVisibility(View.GONE);
        }
        seleccionarObjetivo(objetivo);

        LinearLayout anteriores = v.findViewById(R.id.listaAnteriores);
        anteriores.removeAllViews();
        v.findViewById(R.id.tituloAnteriores).setVisibility(r.anteriores.isEmpty() ? View.GONE : View.VISIBLE);
        for (Modelos.Rutina a : r.anteriores) {
            View fila = LayoutInflater.from(requireContext()).inflate(R.layout.item_ejercicio, anteriores, false);
            ((TextView) fila.findViewById(R.id.ejNombre)).setText(a.nombre + " · " + Formato.nivel(a.nivel));
            TextView det = fila.findViewById(R.id.ejDetalle);
            det.setVisibility(View.VISIBLE);
            det.setText(Formato.fecha(a.fechaCreacion) + " – " + Formato.fecha(a.fechaFinalizacion) + " · "
                    + ("IA".equals(a.origen) ? "IA" : a.entrenador));
            ((TextView) fila.findViewById(R.id.ejSeries)).setText(a.cumplimientoPct + "%");
            anteriores.addView(fila);
        }
    }

    private void seleccionarObjetivo(String nuevo) {
        objetivo = nuevo;
        for (TextView chip : chips) Vistas.marcarChip(chip, nuevo.equalsIgnoreCase((String) chip.getTag()));
        boolean esActiva = activa != null && activa.objetivo.equalsIgnoreCase(nuevo) && activa.contenido != null;
        if (esActiva) {
            mostrarEjercicios(activa.contenido, "Tu rutina · día 1");
        } else {
            llamar(api().vistaPrevia(nuevo, sesionesPorSemana),
                    p -> mostrarEjercicios(p.contenido, "Vista previa IA"));
        }
    }

    private void mostrarEjercicios(Modelos.ContenidoRutina contenido, String info) {
        View v = getView();
        if (v == null || contenido == null || contenido.dias.isEmpty()) return;
        ((TextView) v.findViewById(R.id.previaInfo)).setText(info);
        LinearLayout lista = v.findViewById(R.id.listaEjercicios);
        lista.removeAllViews();
        for (Modelos.Ejercicio e : contenido.dias.get(0).ejercicios) {
            View fila = LayoutInflater.from(requireContext()).inflate(R.layout.item_ejercicio, lista, false);
            ((TextView) fila.findViewById(R.id.ejNombre)).setText(e.nombre);
            ((TextView) fila.findViewById(R.id.ejSeries)).setText(e.series + " × " + e.repeticiones);
            lista.addView(fila);
        }
    }

    private void elegirSesionesYGenerar() {
        String[] opciones = {"2 sesiones por semana", "3 sesiones por semana", "4 sesiones por semana", "5 sesiones por semana", "6 sesiones por semana"};
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Cuántos días a la semana entrenarás?")
                .setSingleChoiceItems(opciones, Math.max(0, Math.min(4, sesionesPorSemana - 2)), (d, cual) -> {
                    sesionesPorSemana = cual + 2;
                    d.dismiss();
                    generar(new Modelos.GenerarRutinaRequest());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** BPM Fig. 9: si faltan datos el backend responde con los campos y se piden al cliente. */
    private void generar(Modelos.GenerarRutinaRequest req) {
        req.objetivo = objetivo;
        req.sesionesPorSemana = sesionesPorSemana;
        View boton = requireView().findViewById(R.id.btnGenerar);
        boton.setEnabled(false);
        llamar(api().generarRutina(req), rutina -> {
            boton.setEnabled(true);
            aviso("¡Tu rutina " + rutina.nombre + " está lista! " + rutina.semanasTotales + " semanas, " + rutina.ritmo + ".");
            recargar();
            abrirRutina();
        }, (mensaje, campos) -> {
            boton.setEnabled(true);
            if (!campos.isEmpty()) pedirDatosFaltantes(campos);
            else aviso(mensaje);
        });
    }

    private void pedirDatosFaltantes(Map<String, String> campos) {
        View vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialogo_datos_faltantes, null);
        Spinner nivel = vista.findViewById(R.id.faltaNivel);
        EditText peso = vista.findViewById(R.id.faltaPeso);
        EditText altura = vista.findViewById(R.id.faltaAltura);
        Dialogos.llenarSpinner(nivel, NIVELES_TEXTO, "Principiante");
        boolean pideNivel = campos.containsKey("nivel");
        boolean pidePeso = campos.containsKey("peso");
        boolean pideAltura = campos.containsKey("altura");
        nivel.setVisibility(pideNivel ? View.VISIBLE : View.GONE);
        vista.findViewById(R.id.etiquetaNivel).setVisibility(pideNivel ? View.VISIBLE : View.GONE);
        peso.setVisibility(pidePeso ? View.VISIBLE : View.GONE);
        vista.findViewById(R.id.etiquetaPeso).setVisibility(pidePeso ? View.VISIBLE : View.GONE);
        altura.setVisibility(pideAltura ? View.VISIBLE : View.GONE);
        vista.findViewById(R.id.etiquetaAltura).setVisibility(pideAltura ? View.VISIBLE : View.GONE);
        AlertDialog d = new AlertDialog.Builder(requireContext())
                .setTitle("Completa tus datos")
                .setView(vista)
                .setPositiveButton("Generar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            Modelos.GenerarRutinaRequest req = new Modelos.GenerarRutinaRequest();
            if (pideNivel) req.nivel = NIVELES[nivel.getSelectedItemPosition()];
            if (pidePeso) {
                req.peso = Formato.aDouble(peso.getText().toString());
                if (req.peso == null || req.peso < 25 || req.peso > 350) {
                    peso.setError("Peso no válido");
                    return;
                }
            }
            if (pideAltura) {
                req.altura = Formato.aDouble(altura.getText().toString());
                if (req.altura == null || req.altura < 1 || req.altura > 2.5) {
                    altura.setError("Altura en metros, p. ej. 1.72");
                    return;
                }
            }
            d.dismiss();
            generar(req);
        }));
        d.show();
    }

    private void abrirRutina() {
        recargarAlVolver = true;
        startActivity(new Intent(requireContext(), RutinaActivity.class));
    }
}
