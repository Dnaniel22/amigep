package pe.edu.utp.amigep.app.ui;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.Sesion;

/**
 * Pantalla principal de cada rol con barra de navegación inferior persistente.
 * Las pestañas se conservan en memoria (show/hide) para volver a ellas en un toque.
 */
public abstract class ActividadConPestanas extends AppCompatActivity {

    private static final String PESTANA = "pestana";
    private BaseFragment[] pestanas;
    private ViewGroup[] botones;
    private int actual = -1;

    protected abstract int layout();

    protected abstract int[] idsBotones();

    protected abstract BaseFragment crearPestana(int indice);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sesion.de(this).activa()) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(layout());
        int[] ids = idsBotones();
        pestanas = new BaseFragment[ids.length];
        botones = new ViewGroup[ids.length];
        for (int i = 0; i < ids.length; i++) {
            final int indice = i;
            botones[i] = findViewById(ids[i]);
            botones[i].setOnClickListener(v -> mostrar(indice));
            // Tras recrear la actividad, el FragmentManager ya restauró las pestañas
            pestanas[i] = (BaseFragment) getSupportFragmentManager().findFragmentByTag(PESTANA + i);
        }
        mostrar(savedInstanceState != null ? savedInstanceState.getInt(PESTANA, 0) : 0);
    }

    public void mostrar(int indice) {
        if (indice == actual) {
            pestanas[indice].recargar();
            return;
        }
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        for (BaseFragment f : pestanas) {
            if (f != null) t.hide(f);
        }
        if (pestanas[indice] == null) {
            pestanas[indice] = crearPestana(indice);
            t.add(R.id.contenido, pestanas[indice], PESTANA + indice);
        } else {
            t.show(pestanas[indice]);
            if (actual != -1) pestanas[indice].recargar();
        }
        t.commit();
        actual = indice;
        for (int i = 0; i < botones.length; i++) {
            int color = Vistas.color(this, i == indice ? R.color.dorado : R.color.apagado);
            ((ImageView) botones[i].getChildAt(0)).setColorFilter(color);
            ((TextView) botones[i].getChildAt(1)).setTextColor(color);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putInt(PESTANA, Math.max(actual, 0));
    }
}
