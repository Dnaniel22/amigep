package pe.edu.utp.amigep.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.datos.ApiCliente;
import pe.edu.utp.amigep.app.datos.Llamada;
import pe.edu.utp.amigep.app.datos.Modelos;
import pe.edu.utp.amigep.app.datos.Sesion;
import pe.edu.utp.amigep.app.ui.cliente.ClienteActivity;
import pe.edu.utp.amigep.app.ui.entrenador.EntrenadorActivity;

/**
 * C1 · Inicio de sesión (RF-01, BPM Fig. 2). El cliente y el entrenador ingresan con su rol;
 * el administrador usa el panel web. Valida los campos antes de enviar (RNF-05).
 */
public class LoginActivity extends AppCompatActivity {

    private String rol = Sesion.CLIENTE;
    private TextView rolCliente;
    private TextView rolEntrenador;
    private EditText correo;
    private EditText contrasena;
    private Button btnIngresar;
    private ProgressBar cargando;
    private boolean mostrandoClave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sesion sesion = Sesion.de(this);
        if (sesion.activa()) {
            irAPantallaInicial(sesion.rol());
            return;
        }
        setContentView(R.layout.activity_login);
        rolCliente = findViewById(R.id.rolCliente);
        rolEntrenador = findViewById(R.id.rolEntrenador);
        correo = findViewById(R.id.correo);
        contrasena = findViewById(R.id.contrasena);
        btnIngresar = findViewById(R.id.btnIngresar);
        cargando = findViewById(R.id.cargando);

        correo.setText(sesion.ultimoCorreo());
        rolCliente.setOnClickListener(v -> seleccionarRol(Sesion.CLIENTE));
        rolEntrenador.setOnClickListener(v -> seleccionarRol(Sesion.ENTRENADOR));
        seleccionarRol(Sesion.CLIENTE);

        TextView ver = findViewById(R.id.verContrasena);
        ver.setOnClickListener(v -> {
            mostrandoClave = !mostrandoClave;
            contrasena.setInputType(InputType.TYPE_CLASS_TEXT | (mostrandoClave
                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            contrasena.setSelection(contrasena.getText().length());
            ver.setText(mostrandoClave ? "Ocultar" : "Ver");
        });
        btnIngresar.setOnClickListener(v -> ingresar());
        findViewById(R.id.olvide).setOnClickListener(v -> recuperarContrasena());
        findViewById(R.id.btnServidor).setOnClickListener(v -> configurarServidor());
        ((TextView) findViewById(R.id.terminos)).setText(textoTerminos());
    }

    private void seleccionarRol(String nuevo) {
        rol = nuevo;
        boolean cliente = Sesion.CLIENTE.equals(rol);
        rolCliente.setBackgroundResource(cliente ? R.drawable.bg_segmento_activo : 0);
        rolEntrenador.setBackgroundResource(cliente ? 0 : R.drawable.bg_segmento_activo);
        rolCliente.setTextColor(Vistas.color(this, cliente ? R.color.turquesa : R.color.apagado));
        rolEntrenador.setTextColor(Vistas.color(this, cliente ? R.color.apagado : R.color.turquesa));
    }

    private void ingresar() {
        String c = correo.getText().toString().trim();
        String clave = contrasena.getText().toString();
        boolean valido = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(c).matches()) {
            correo.setError("Ingrese un correo válido");
            valido = false;
        }
        if (clave.isEmpty()) {
            contrasena.setError("Ingrese su contraseña");
            valido = false;
        }
        if (!valido) return;
        cargando(true);
        Llamada.ejecutar(this, ApiCliente.de(this).login(new Modelos.LoginRequest(c, clave, rol)), r -> {
            Sesion sesion = Sesion.de(this);
            sesion.guardar(r);
            sesion.guardarUltimoCorreo(c);
            irAPantallaInicial(r.rol);
        }, (mensaje, campos) -> {
            cargando(false);
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        }, Llamada.mientrasViva(this));
    }

    private void cargando(boolean si) {
        btnIngresar.setEnabled(!si);
        btnIngresar.setAlpha(si ? 0.6f : 1f);
        cargando.setVisibility(si ? View.VISIBLE : View.GONE);
    }

    private void irAPantallaInicial(String rolUsuario) {
        Class<?> destino = Sesion.ENTRENADOR.equals(rolUsuario) ? EntrenadorActivity.class : ClienteActivity.class;
        startActivity(new Intent(this, destino));
        finish();
    }

    /** Recuperación en dos pasos: solicitar código → ingresar código y nueva contraseña. */
    private void recuperarContrasena() {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialogo_recuperar, null);
        EditText recCorreo = vista.findViewById(R.id.recCorreo);
        EditText recCodigo = vista.findViewById(R.id.recCodigo);
        EditText recNueva = vista.findViewById(R.id.recNueva);
        TextView recMensaje = vista.findViewById(R.id.recMensaje);
        View paso2 = vista.findViewById(R.id.recPaso2);
        recCorreo.setText(correo.getText());
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña")
                .setView(vista)
                .setPositiveButton("Enviar código", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String c = recCorreo.getText().toString().trim();
            if (paso2.getVisibility() == View.GONE) {
                if (!Patterns.EMAIL_ADDRESS.matcher(c).matches()) {
                    recCorreo.setError("Correo no válido");
                    return;
                }
                Llamada.ejecutar(this, ApiCliente.de(this).recuperar(new Modelos.RecuperarRequest(c, rol)), r -> {
                    recMensaje.setText(r.mensaje + (r.codigoDesarrollo != null ? "\nCódigo de prueba: " + r.codigoDesarrollo : ""));
                    paso2.setVisibility(View.VISIBLE);
                    recCorreo.setEnabled(false);
                    dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setText("Cambiar contraseña");
                });
            } else {
                String nueva = recNueva.getText().toString();
                if (nueva.length() < 8) {
                    recNueva.setError("Mínimo 8 caracteres");
                    return;
                }
                Llamada.ejecutar(this, ApiCliente.de(this).restablecer(new Modelos.RestablecerRequest(c, rol,
                        recCodigo.getText().toString().trim(), nueva)), r -> {
                    Toast.makeText(this, r.mensaje, Toast.LENGTH_LONG).show();
                    dialogo.dismiss();
                }, (mensaje, campos) -> recCodigo.setError(mensaje));
            }
        }));
        dialogo.show();
    }

    /** Permite probar con un celular real apuntando a la IP de la PC donde corre el backend. */
    private void configurarServidor() {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialogo_campo, null);
        ((TextView) vista.findViewById(R.id.dlgTexto)).setText(
                "URL del backend Spring Boot. Emulador: http://10.0.2.2:8080/ · Celular: http://IP-DE-TU-PC:8080/");
        EditText campo = vista.findViewById(R.id.dlgCampo);
        campo.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        campo.setText(Sesion.de(this).servidor());
        new AlertDialog.Builder(this)
                .setTitle("Servidor")
                .setView(vista)
                .setPositiveButton("Guardar", (d, w) -> {
                    String url = campo.getText().toString().trim();
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        Sesion.de(this).guardarServidor(url);
                        Toast.makeText(this, "Servidor actualizado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "La URL debe empezar con http:// o https://", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private CharSequence textoTerminos() {
        SpannableStringBuilder sb = new SpannableStringBuilder("Al continuar aceptas los ");
        agregarResaltado(sb, "Términos de uso");
        sb.append(" y la\n");
        agregarResaltado(sb, "Política de privacidad");
        sb.append(" de AMIGEP.");
        return sb;
    }

    private void agregarResaltado(SpannableStringBuilder sb, String texto) {
        int inicio = sb.length();
        sb.append(texto);
        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), inicio, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(Vistas.color(this, R.color.texto_2)), inicio, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
