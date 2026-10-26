package pe.edu.utp.amigep.app.ui.cliente;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.ui.ActividadConPestanas;
import pe.edu.utp.amigep.app.ui.BaseFragment;

/** Rol cliente: Inicio (C2), Entreno IA (C3), Pagos (C4), Guía - alimentación IA (C5) y Perfil (C6). */
public class ClienteActivity extends ActividadConPestanas {

    public static final int INICIO = 0;
    public static final int ENTRENO = 1;
    public static final int PAGOS = 2;
    public static final int GUIA = 3;
    public static final int PERFIL = 4;

    @Override
    protected int layout() {
        return R.layout.activity_cliente;
    }

    @Override
    protected int[] idsBotones() {
        return new int[]{R.id.navInicio, R.id.navEntreno, R.id.navPagos, R.id.navGuia, R.id.navPerfil};
    }

    @Override
    protected BaseFragment crearPestana(int indice) {
        switch (indice) {
            case ENTRENO: return new EntrenoFragment();
            case PAGOS: return new PagosFragment();
            case GUIA: return new GuiaFragment();
            case PERFIL: return new PerfilFragment();
            default: return new InicioFragment();
        }
    }
}
