// Configuración: usuarios y roles (RF-02 · BPM Fig. 3), catálogo de planes y cuenta del administrador
import { api, sesion } from '../api.js';
import { icono, esc, fmt, insignia, avatar, cargando, tablaVacia, modal, campo, opciones, leerFormulario, mostrarErrores, conCarga, toast, errorToast, confirmar } from '../ui.js';
import { restablecer } from './entrenadores.js';

export async function render(el) {
  el.innerHTML = `
    <div class="cabecera"><div><h1>Configuración</h1><p>RF-02 · usuarios y roles del personal, catálogo de planes y su cuenta</p></div></div>
    <div class="pestanas">
      <button class="activo" data-tab="usuarios">Usuarios y roles</button>
      <button data-tab="planes">Planes</button>
      <button data-tab="cuenta">Mi cuenta</button>
    </div>
    <div id="panel"></div>`;
  const tabs = { usuarios, planes, cuenta };
  const abrir = nombre => {
    el.querySelectorAll('[data-tab]').forEach(b => b.classList.toggle('activo', b.dataset.tab === nombre));
    const panel = el.querySelector('#panel');
    panel.innerHTML = cargando();
    tabs[nombre](panel).catch(errorToast);
  };
  el.querySelectorAll('[data-tab]').forEach(b => b.addEventListener('click', () => abrir(b.dataset.tab)));
  abrir('usuarios');
}

async function usuarios(panel) {
  const lista = await api('/admin/trabajadores');
  const yo = sesion()?.id;
  panel.innerHTML = `
    <div class="barra-filtros"><span class="c-apagado">El administrador usa el panel web; los entrenadores usan la app móvil.</span>
      <button class="btn btn-turquesa" style="margin-left:auto" id="nuevo">${icono('mas')} Nuevo usuario</button></div>
    <div class="tarjeta" style="padding:6px 14px"><div class="tabla-envoltura"><table class="tabla">
      <thead><tr><th>Usuario</th><th>DNI</th><th>Rol</th><th>Especialidad</th><th>Estado</th><th></th></tr></thead>
      <tbody>${lista.map(t => `
        <tr>
          <td><div class="persona">${avatar(t.iniciales, t.nombre + t.apellido)}<div><strong>${esc(t.nombre)} ${esc(t.apellido)} ${t.idTrabajador === yo ? '<span class="c-apagado">(usted)</span>' : ''}</strong><small>${esc(t.email)}</small></div></div></td>
          <td class="mono">${esc(t.dni)}</td><td>${insignia(t.rol)}</td><td>${esc(t.especialidad || '—')}</td>
          <td>${t.activo ? insignia('ACTIVO') : insignia('INACTIVO')}</td>
          <td class="num" style="white-space:nowrap">
            <button class="btn-icono" data-editar="${t.idTrabajador}" title="Editar">${icono('lapiz')}</button>
            <button class="btn-icono" data-clave="${t.idTrabajador}" title="Restablecer contraseña">${icono('llave')}</button>
            ${t.idTrabajador !== yo ? `<button class="btn btn-borde btn-chico" data-estado="${t.idTrabajador}" data-activo="${t.activo}">${t.activo ? 'Desactivar' : 'Activar'}</button>` : ''}
          </td>
        </tr>`).join('')}</tbody></table></div></div>`;
  const recargar = () => usuarios(panel).catch(errorToast);
  panel.querySelector('#nuevo').addEventListener('click', () => abrirFormularioTrabajador(null, recargar));
  panel.querySelectorAll('[data-editar]').forEach(b => b.addEventListener('click', () => abrirFormularioTrabajador(Number(b.dataset.editar), recargar)));
  panel.querySelectorAll('[data-clave]').forEach(b => b.addEventListener('click', () => restablecer(Number(b.dataset.clave))));
  panel.querySelectorAll('[data-estado]').forEach(b => b.addEventListener('click', async () => {
    const activar = b.dataset.activo !== 'true';
    if (!activar && !await confirmar('El usuario no podrá iniciar sesión.', { boton: 'Desactivar', peligro: true })) return;
    try {
      await api(`/admin/trabajadores/${b.dataset.estado}/estado`, { metodo: 'PATCH', cuerpo: { activo: activar } });
      toast('Estado actualizado');
      recargar();
    } catch (e) { errorToast(e); }
  }));
}

export async function abrirFormularioTrabajador(id, alGuardar, rolPorDefecto = 'ENTRENADOR') {
  const t = id ? await api(`/admin/trabajadores/${id}`) : {};
  const m = modal({
    titulo: id ? `Editar ${t.nombre} ${t.apellido}` : 'Nuevo usuario del personal',
    subtitulo: 'Validación de datos y asignación de rol (BPM Fig. 3 y 7)',
    contenido: `
      <form id="f-trab" novalidate>
        <div class="aviso error oculto" data-error-general></div>
        <div class="form-2">
          ${campo('Nombres *', `<input class="entrada" name="nombre" value="${esc(t.nombre)}">`)}
          ${campo('Apellidos *', `<input class="entrada" name="apellido" value="${esc(t.apellido)}">`)}
          ${campo('DNI *', `<input class="entrada" name="dni" value="${esc(t.dni)}" maxlength="8" inputmode="numeric">`)}
          ${campo('Correo *', `<input class="entrada" name="email" type="email" value="${esc(t.email)}">`)}
          ${campo('Rol *', `<select class="entrada" name="rol">${opciones([['ENTRENADOR', 'Entrenador (app móvil)'], ['ADMINISTRADOR', 'Administrador (panel web)']], t.rol || rolPorDefecto)}</select>`)}
          ${campo('Especialidad', `<input class="entrada" name="especialidad" value="${esc(t.especialidad)}" placeholder="Ej. Hipertrofia y fuerza">`)}
        </div>
        ${campo(id ? 'Nueva contraseña (opcional)' : 'Contraseña *', '<input class="entrada" name="contrasena" type="password" autocomplete="new-password" placeholder="Mínimo 8 caracteres">')}
      </form>`,
    pie: `<button class="btn btn-borde" data-cerrar>Cancelar</button><button class="btn btn-turquesa" id="guardar">${icono('check')} Guardar</button>`
  });
  const form = m.el.querySelector('#f-trab');
  const boton = m.el.querySelector('#guardar');
  boton.addEventListener('click', () => conCarga(boton, async () => {
    const d = leerFormulario(form);
    try {
      await api(id ? `/admin/trabajadores/${id}` : '/admin/trabajadores', { metodo: id ? 'PUT' : 'POST', cuerpo: d });
      m.cerrar();
      toast(id ? 'Usuario actualizado' : 'Usuario registrado con su rol');
      alGuardar?.();
    } catch (err) { mostrarErrores(form, err); }
  }));
}

async function planes(panel) {
  const lista = await api('/admin/planes');
  panel.innerHTML = `
    <div class="barra-filtros"><span class="c-apagado">Catálogo de planes de membresía que se ofrecen en recepción y en la app (C4).</span>
      <button class="btn btn-turquesa" style="margin-left:auto" id="nuevo">${icono('mas')} Nuevo plan</button></div>
    <div class="tarjeta" style="padding:6px 14px"><table class="tabla">
      <thead><tr><th>Plan</th><th>Descripción</th><th>Duración</th><th class="num">Precio</th><th></th></tr></thead>
      <tbody>${lista.map(p => `<tr><td><strong>${esc(p.nombre)}</strong></td><td class="c-apagado">${esc(p.descripcion || '—')}</td>
        <td>${p.duracionDias} días</td><td class="num">${fmt.soles(p.precio)}</td>
        <td class="num"><button class="btn-icono" data-editar="${p.idPlan}">${icono('lapiz')}</button></td></tr>`).join('') || tablaVacia(5)}</tbody>
    </table></div>`;
  const formulario = p => {
    const m = modal({
      titulo: p ? `Editar plan ${p.nombre}` : 'Nuevo plan',
      contenido: `<form id="f-plan" novalidate><div class="aviso error oculto" data-error-general></div>
        <div class="form-2">${campo('Nombre *', `<input class="entrada" name="nombre" value="${esc(p?.nombre)}">`)}
        ${campo('Duración (días) *', `<input class="entrada" type="number" name="duracionDias" min="1" value="${p?.duracionDias ?? 30}">`)}
        ${campo('Precio (S/) *', `<input class="entrada" type="number" name="precio" min="0" step="0.01" value="${p?.precio ?? ''}">`)}</div>
        ${campo('Descripción', `<input class="entrada" name="descripcion" maxlength="200" value="${esc(p?.descripcion)}">`)}</form>`,
      pie: `<button class="btn btn-borde" data-cerrar>Cancelar</button><button class="btn btn-turquesa" id="guardar">Guardar</button>`
    });
    const form = m.el.querySelector('#f-plan');
    m.el.querySelector('#guardar').addEventListener('click', e => conCarga(e.currentTarget, async () => {
      try {
        await api(p ? `/admin/planes/${p.idPlan}` : '/admin/planes', { metodo: p ? 'PUT' : 'POST', cuerpo: leerFormulario(form) });
        m.cerrar();
        toast('Plan guardado');
        planes(panel);
      } catch (err) { mostrarErrores(form, err); }
    }));
  };
  panel.querySelector('#nuevo').addEventListener('click', () => formulario(null));
  panel.querySelectorAll('[data-editar]').forEach(b => b.addEventListener('click', () => formulario(lista.find(p => p.idPlan === Number(b.dataset.editar)))));
}

async function cuenta(panel) {
  const s = sesion();
  panel.innerHTML = `
    <div class="rejilla-2b">
      <div class="tarjeta">
        <div class="tarjeta-titulo"><h2>Mi cuenta</h2></div>
        <div class="datos-lista">
          <div><small>Nombre</small>${esc(s.nombre)} ${esc(s.apellido)}</div>
          <div><small>Correo</small>${esc(s.correo)}</div>
          <div><small>Rol</small>${insignia('ADMINISTRADOR')}</div>
        </div>
      </div>
      <form class="tarjeta" id="f-clave" novalidate>
        <div class="tarjeta-titulo"><h2>Cambiar contraseña</h2></div>
        <div class="aviso error oculto" data-error-general></div>
        ${campo('Contraseña actual', '<input class="entrada" type="password" name="actual" autocomplete="current-password">')}
        ${campo('Nueva contraseña', '<input class="entrada" type="password" name="nueva" autocomplete="new-password">')}
        <button class="btn btn-turquesa" type="submit">${icono('llave')} Actualizar contraseña</button>
      </form>
    </div>`;
  const form = panel.querySelector('#f-clave');
  form.addEventListener('submit', e => {
    e.preventDefault();
    conCarga(form.querySelector('button'), async () => {
      try {
        await api('/auth/contrasena', { metodo: 'PUT', cuerpo: leerFormulario(form) });
        form.reset();
        mostrarErrores(form, null);
        toast('Contraseña actualizada');
      } catch (err) { mostrarErrores(form, err); }
    });
  });
}
