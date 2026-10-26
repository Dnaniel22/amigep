package pe.edu.utp.amigep.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Adaptador genérico para RecyclerView: un layout por fila y una función que la llena. */
public class ListaAdapter<T> extends RecyclerView.Adapter<ListaAdapter.Fila> {

    public interface Enlazador<T> {
        void enlazar(View fila, T item, int posicion);
    }

    public interface AlTocar<T> {
        void tocar(T item);
    }

    private final int layout;
    private final Enlazador<T> enlazador;
    private final AlTocar<T> alTocar;
    private final List<T> items = new ArrayList<>();

    public ListaAdapter(int layout, Enlazador<T> enlazador, AlTocar<T> alTocar) {
        this.layout = layout;
        this.enlazador = enlazador;
        this.alTocar = alTocar;
    }

    public void mostrar(List<T> nuevos) {
        items.clear();
        if (nuevos != null) items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(layout, padre, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        T item = items.get(posicion);
        enlazador.enlazar(fila.itemView, item, posicion);
        if (alTocar != null) fila.itemView.setOnClickListener(v -> alTocar.tocar(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class Fila extends RecyclerView.ViewHolder {
        public Fila(@NonNull View vista) {
            super(vista);
        }
    }
}
