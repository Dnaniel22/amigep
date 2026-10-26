package pe.edu.utp.amigep.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.datos.Sesion;

/** Diálogos compartidos por el cliente y el entrenador. */
public final class Dialogos {

    private Dialogos() {
    }

    public static void cambiarContrasena(Activity a) {
        View vista = LayoutInflater.from(a).inflate(R.layout.dialogo_campo, null);
        ((TextView) vista.findViewById(R.id.dlgTexto)).setText("La nueva contraseña debe tener al menos 8 caracteres.");
        EditText actual = vista.findViewById(R.id.dlgCampo);
        EditText nueva = vista.findViewById(R.id.dlgCampo2);
        actual.setHint("Contraseña actual");
        actual.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        nueva.setHint("Nueva contraseña");
        nueva.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        nueva.setVisibility(View.VISIBLE);
        AlertDialog d = new AlertDialog.Builder(a)
                .setTitle("Seguridad y contraseña")
                .setView(vista)
                .setPositiveButton("Actualizar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            if (nueva.getText().length() < 8) {
                nueva.setError("Mínimo 8 caracteres");
                return;
            }
            Llamada.ejecutar(a, ApiCliente.de(a).cambiarContrasena(new Modelos.CambiarContrasenaRequest(
                    actual.getText().toString(), nueva.getText().toString())), r -> {
                Toast.makeText(a, "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                d.dismiss();
            }, (mensaje, campos) -> actual.setError(campos.containsKey("actual") ? campos.get("actual") : mensaje));
        }));
        d.show();
    }

    public static void cerrarSesion(Activity a) {
        new AlertDialog.Builder(a)
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas salir de tu cuenta?")
                .setPositiveButton("Cerrar sesión", (d, w) -> {
                    Sesion.de(a).cerrar();
                    Intent i = new Intent(a, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    a.startActivity(i);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Llena un Spinner con el estilo oscuro de la app. */
    public static void llenarSpinner(Spinner spinner, String[] opciones, String seleccion) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinner.getContext(), R.layout.item_spinner, opciones);
        adapter.setDropDownViewResource(R.layout.item_spinner_desplegable);
        spinner.setAdapter(adapter);
        for (int i = 0; i < opciones.length; i++) {
            if (opciones[i].equalsIgnoreCase(seleccion == null ? "" : seleccion)) spinner.setSelection(i);
        }
    }
}
