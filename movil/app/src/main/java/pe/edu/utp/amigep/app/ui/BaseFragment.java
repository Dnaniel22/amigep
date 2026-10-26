package pe.edu.utp.amigep.app.ui;

import android.widget.Toast;

import androidx.fragment.app.Fragment;

import pe.edu.utp.amigep.app.datos.AmigepApi;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import retrofit2.Call;

/** Base de las pestañas: acceso a la API ignorando respuestas si el fragmento ya no está visible. */
public abstract class BaseFragment extends Fragment {

    protected AmigepApi api() {
        return ApiCliente.de(requireContext());
    }

    protected <T> void llamar(Call<T> call, Llamada.Exito<T> exito) {
        Llamada.ejecutar(requireContext(), call, exito, null, () -> isAdded() && getView() != null);
    }

    protected <T> void llamar(Call<T> call, Llamada.Exito<T> exito, Llamada.Fallo fallo) {
        Llamada.ejecutar(requireContext(), call, exito, fallo, () -> isAdded() && getView() != null);
    }

    protected void aviso(String mensaje) {
        if (isAdded()) Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
    }

    /** Se llama al volver a la pestaña para refrescar sus datos. */
    public void recargar() {
    }

    /** Marcar antes de abrir otra pantalla que modifica datos: al volver se recarga la pestaña. */
    protected boolean recargarAlVolver;

    @Override
    public void onResume() {
        super.onResume();
        if (recargarAlVolver) {
            recargarAlVolver = false;
            recargar();
        }
    }
}
