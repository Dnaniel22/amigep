// Entrenadores (RF-07 · BPM Fig. 7)
import { api } from '../api.js';
import { icono, esc, insignia, avatar, cargando, tablaVacia, errorToast, toast, confirmar } from '../ui.js';
import { abrirFormularioTrabajador } from './configuracion.js';

export async function render(el) {
  el.innerHTML = `
    <div class="cabecera">
      <div><h1>Entrenadores</h1><p>RF-07 · registro y gestión de entrenadores; cada entrenador ve en la app solo a sus clientes asignados</p></div>
      <div class="acciones"><button class="btn btn-turquesa" id="nuevo">${icono('mas')} Nuevo entrenador</button></div>
    </div>
    <div class="tarjeta" style="padding:6px 14px">
      <div class="tabla-envoltura">
        <table class="tabla">
          <thead><tr><th>Entrenador</th><th>DNI</th><th>Especialidad</th><th>Clientes asignados</th><th>Estado</th><th></th></tr></thead>
          <tbody id="filas"><tr><td colspan="6">${cargando()}</td></tr></tbody>
        </table>
      </div>
    </div>`;
  const cargar = async () => {
    const lista = await api('/admin/trabajadores', { params: { rol: 'ENTRENADOR' } });
    el.querySelector('#filas').innerHTML = lista.length ? lista.map(t => `
      <tr>
        <td><div class="persona">${avatar(t.iniciales, t.nombre + t.apellido)}<div><strong>${esc(t.nombre)} ${esc(t.apellido)}</strong><small>${esc(t.email)}</small></div></div></td>
        <td class="mono">${esc(t.dni)}</td>
        <td>${esc(t.especialidad || '—')}</td>
        <td><strong>${t.clientesAsignados}</strong> <span class="c-apagado">clientes</span></td>
        <td>${t.activo ? insignia('ACTIVO') : insignia('INACTIVO')}</td>
        <td class="num" style="white-space:nowrap">
          <button class="btn-icono" data-editar="${t.idTrabajador}" title="Editar">${icono('lapiz')}</button>
          <button class="btn-icono" data-clave="${t.idTrabajador}" title="Restablecer contraseña">${icono('llave')}</button>
          <button class="btn btn-borde btn-chico" data-estado="${t.idTrabajador}" data-activo="${t.activo}">${t.activo ? 'Desactivar' : 'Activar'}</button>
        </td>
      </tr>`).join('') : tablaVacia(6, 'Aún no hay entrenadores registrados');
    el.querySelectorAll('[data-editar]').forEach(b => b.addEventListener('click', () => abrirFormularioTrabajador(Number(b.dataset.editar), refrescar, 'ENTRENADOR')));
    el.querySelectorAll('[data-clave]').forEach(b => b.addEventListener('click', () => restablecer(Number(b.dataset.clave))));
    el.querySelectorAll('[data-estado]').forEach(b => b.addEventListener('click', async () => {
      const activar = b.dataset.activo !== 'true';
      if (!activar && !await confirmar('El entrenador no podrá iniciar sesión en la app. Sus rutinas y clientes se conservan.', { boton: 'Desactivar', peligro: true })) return;
      try {
        await api(`/admin/trabajadores/${b.dataset.estado}/estado`, { metodo: 'PATCH', cuerpo: { activo: activar } });
        toast(activar ? 'Entrenador activado' : 'Entrenador desactivado');
        refrescar();
      } catch (e) { errorToast(e); }
    }));
  };
  const refrescar = () => cargar().catch(errorToast);
  el.querySelector('#nuevo').addEventListener('click', () => abrirFormularioTrabajador(null, refrescar, 'ENTRENADOR'));
  await cargar();
}

export async function restablecer(id) {
  if (!await confirmar('Se generará una contraseña temporal que deberá entregar al trabajador.', { boton: 'Generar' })) return;
  try {
    const r = await api(`/admin/trabajadores/${id}/restablecer-contrasena`, { metodo: 'POST' });
    await confirmar(`${r.mensaje}: ${r.contrasenaTemporal}`, { titulo: 'Contraseña temporal', boton: 'Entendido' });
  } catch (e) { errorToast(e); }
}
