package pe.edu.utp.amigep.app.ui.entrenador;

import pe.edu.utp.amigep.app.R;
import pe.edu.utp.amigep.app.ui.ActividadConPestanas;
import pe.edu.utp.amigep.app.ui.BaseFragment;

/** Rol entrenador: Clientes (E1), Rutinas, Agenda (calendario de asistencias) y Perfil. */
public class EntrenadorActivity extends ActividadConPestanas {

    public static final int CLIENTES = 0;
    public static final int RUTINAS = 1;
    public static final int AGENDA = 2;
    public static final int PERFIL = 3;

    @Override
    protected int layout() {
        return R.layout.activity_entrenador;
    }

    @Override
    protected int[] idsBotones() {
        return new int[]{R.id.navClientes, R.id.navRutinas, R.id.navAgenda, R.id.navPerfil};
    }

    @Override
    protected BaseFragment crearPestana(int indice) {
        switch (indice) {
            case RUTINAS: return new RutinasFragment();
            case AGENDA: return new AgendaFragment();
            case PERFIL: return new PerfilEntrenadorFragment();
            default: return new ClientesFragment();
        }
    }
}
