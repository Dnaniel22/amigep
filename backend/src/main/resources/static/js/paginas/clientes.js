// A2 · Clientes y membresías (RF-02, RF-03, RF-04 · BPM Fig. 4)
import { api } from '../api.js';
import {
  icono, esc, fmt, insignia, avatar, iniciales, cargando, tablaVacia, modal, campo, opciones, leerFormulario,
  mostrarErrores, conCarga, toast, errorToast, confirmar, qrImagen, debounce
} from '../ui.js';

const ESTADOS = [['ACTIVO', 'Activo'], ['POR_VENCER', 'Por vencer'], ['VENCIDO', 'Vencido'], ['SIN_MEMBRESIA', 'Sin membresía'], ['INACTIVO', 'Cuenta desactivada']];
const OBJETIVOS = ['Masa muscular', 'Pérdida de grasa', 'Fuerza', 'Resistencia'];

let refrescarLista = () => {};

export async function render(el) {
  const planes = await api('/admin/planes');
  el.innerHTML = `
    <div class="cabecera">
      <div><h1>Clientes y membresías</h1><p>RF-02 · RF-03 · RF-04 — gestión de usuarios, clientes y planes</p></div>
      <div class="acciones"><button class="btn btn-turquesa" id="nuevo">${icono('mas')} Nuevo cliente</button></div>
    </div>
    <div class="barra-filtros">
      <div class="buscador">${icono('buscar')}<input class="entrada" id="q" placeholder="Buscar por nombre, código o DNI..."></div>
      <div class="select-filtro">${icono('filtro')}<select class="entrada" id="f-plan">${opciones(planes.map(p => [p.idPlan, p.nombre]), '', { vacio: 'Plan' })}</select></div>
      <div class="select-filtro">${icono('filtro')}<select class="entrada" id="f-estado">${opciones(ESTADOS, '', { vacio: 'Estado' })}</select></div>
      <span class="c-apagado" id="total" style="margin-left:auto;font-size:13px"></span>
    </div>
    <div class="tarjeta" style="padding:6px 14px">
      <div class="tabla-envoltura">
        <table class="tabla">
          <thead><tr><th>Código</th><th>Cliente</th><th>Correo</th><th>Plan</th><th>Inicio</th><th>Vencimiento</th><th>Estado</th><th></th></tr></thead>
          <tbody id="filas"><tr><td colspan="8">${cargando()}</td></tr></tbody>
        </table>
      </div>
    </div>`;

  const cargar = async () => {
    const lista = await api('/admin/clientes', {
      params: { q: el.querySelector('#q').value, idPlan: el.querySelector('#f-plan').value, estado: el.querySelector('#f-estado').value }
    });
    el.querySelector('#total').textContent = `${lista.length} cliente${lista.length === 1 ? '' : 's'}`;
    el.querySelector('#filas').innerHTML = lista.length ? lista.map(c => `
      <tr class="clic" data-id="${c.idCliente}">
        <td class="mono">${esc(c.codigo)}</td>
        <td><div class="persona">${avatar(iniciales(c.nombre, c.apellido), c.nombre + c.apellido)}<div><strong>${esc(c.nombre)} ${esc(c.apellido)}</strong>
            ${c.entrenador ? `<small>Entrenador: ${esc(c.entrenador)}</small>` : ''}</div></div></td>
        <td class="mono">${esc(c.email)}</td>
        <td>${esc(c.plan || '—')}</td>
        <td class="mono">${fmt.fecha(c.fechaInicio)}</td>
        <td class="mono">${fmt.fecha(c.fechaCaducidad)}</td>
        <td>${c.activo ? insignia(c.estadoMembresia) : insignia('INACTIVO', 'Desactivado')}</td>
        <td class="num"><button class="btn-icono" data-editar="${c.idCliente}" title="Editar">${icono('lapiz')}</button></td>
      </tr>`).join('') : tablaVacia(8, 'No se encontraron clientes con esos filtros');
    el.querySelectorAll('tr[data-id]').forEach(tr => tr.addEventListener('click', e => {
      const editar = e.target.closest('[data-editar]');
      if (editar) abrirFormularioCliente(Number(editar.dataset.editar));
      else abrirDetalleCliente(Number(tr.dataset.id));
    }));
  };
  refrescarLista = () => cargar().catch(errorToast);
  el.querySelector('#q').addEventListener('input', debounce(refrescarLista));
  el.querySelector('#f-plan').addEventListener('change', refrescarLista);
  el.querySelector('#f-estado').addEventListener('change', refrescarLista);
  el.querySelector('#nuevo').addEventListener('click', () => abrirFormularioCliente(null));
  await cargar();
  return () => { refrescarLista = () => {}; };
}

// ----------------------------------------------------------------- Alta / edición (BPM Fig. 4)
export async function abrirFormularioCliente(idCliente, alGuardar) {
  const [planes, entrenadores, actual] = await Promise.all([
    api('/admin/planes'),
    api('/admin/trabajadores', { params: { rol: 'ENTRENADOR' } }),
    idCliente ? api(`/admin/clientes/${idCliente}`) : Promise.resolve(null)
  ]);
  const c = actual?.resumen || {};
  const nuevo = !idCliente;
  const m = modal({
    titulo: nuevo ? 'Nuevo cliente' : `Editar cliente ${c.codigo}`,
    subtitulo: nuevo ? 'Se generará su código QR personal para el control de acceso' : `${c.nombre} ${c.apellido}`,
    ancho: 'ancho',
    contenido: `
      <form id="f-cliente" novalidate>
        <div class="aviso error oculto" data-error-general></div>
        <div class="form-3">
          ${campo('Nombres *', `<input class="entrada" name="nombre" value="${esc(c.nombre)}" maxlength="50">`)}
          ${campo('Apellidos *', `<input class="entrada" name="apellido" value="${esc(c.apellido)}" maxlength="50">`)}
          ${campo('DNI *', `<input class="entrada" name="dni" value="${esc(c.dni)}" inputmode="numeric" maxlength="8">`)}
          ${campo('Correo *', `<input class="entrada" name="email" type="email" value="${esc(c.email)}">`)}
          ${campo('Teléfono', `<input class="entrada" name="telefono" value="${esc(c.telefono)}" maxlength="15">`)}
          ${campo('Fecha de nacimiento', `<input class="entrada" name="fechaNacimiento" type="date" value="${esc(actual?.fechaNacimiento)}">`)}
          ${campo('Objetivo', `<select class="entrada" name="objetivo">${opciones(OBJETIVOS.map(o => [o, o]), actual?.objetivo, { vacio: 'Sin definir' })}</select>`)}
          ${campo('Nivel', `<select class="entrada" name="nivel">${opciones([['PRINCIPIANTE', 'Principiante'], ['INTERMEDIO', 'Intermedio'], ['AVANZADO', 'Avanzado']], actual?.nivel, { vacio: 'Sin definir' })}</select>`)}
          ${campo('Altura (m)', `<input class="entrada" name="altura" type="number" step="0.01" min="1" max="2.5" value="${esc(actual?.altura)}">`)}
          ${campo('Entrenador asignado', `<select class="entrada" name="idEntrenador">${opciones(entrenadores.filter(e => e.activo).map(e => [e.idTrabajador, `${e.nombre} ${e.apellido} (${e.clientesAsignados})`]), c.idEntrenador, { vacio: 'Sin entrenador' })}</select>`)}
          ${campo(nuevo ? 'Contraseña inicial *' : 'Nueva contraseña (opcional)', '<input class="entrada" name="contrasena" type="password" autocomplete="new-password" placeholder="Mínimo 8 caracteres">')}
        </div>
        ${nuevo ? `
          <div class="aviso">Opcional: registre la primera membresía y su pago en recepción.</div>
          <div class="form-2">
            ${campo('Plan', `<select class="entrada" name="idPlan">${opciones(planes.map(p => [p.idPlan, `${p.nombre} · ${fmt.soles(p.precio)} / ${p.duracionDias} días`]), '', { vacio: 'Sin membresía por ahora' })}</select>`)}
            ${campo('Método de pago', `<select class="entrada" name="metodoPago">${opciones([['YAPE_PLIN', 'Yape / Plin'], ['TARJETA', 'Tarjeta'], ['EFECTIVO', 'Efectivo']], 'YAPE_PLIN')}</select>`)}
          </div>` : ''}
      </form>`,
    pie: `<button class="btn btn-borde" data-cerrar>Cancelar</button><button class="btn btn-turquesa" id="guardar">${icono('check')} Guardar</button>`
  });
  const form = m.el.querySelector('#f-cliente');
  const boton = m.el.querySelector('#guardar');
  boton.addEventListener('click', () => conCarga(boton, async () => {
    const d = leerFormulario(form);
    if (d.idEntrenador) d.idEntrenador = Number(d.idEntrenador);
    if (d.idPlan) d.idPlan = Number(d.idPlan); else { d.idPlan = null; d.metodoPago = null; }
    // Validación previa en el navegador (RNF-05); el backend vuelve a validar.
    const errores = {};
    if (!d.nombre) errores.nombre = 'Ingrese el nombre';
    if (!d.apellido) errores.apellido = 'Ingrese el apellido';
    if (!/^\d{8}$/.test(d.dni || '')) errores.dni = 'El DNI debe tener 8 dígitos';
    if (!/^\S+@\S+\.\S+$/.test(d.email || '')) errores.email = 'Correo no válido';
    if (nuevo && !d.contrasena) errores.contrasena = 'Ingrese una contraseña inicial';
    if (Object.keys(errores).length) return mostrarErrores(form, { message: 'Revise los datos ingresados', campos: errores });
    try {
      const r = nuevo
        ? await api('/admin/clientes', { metodo: 'POST', cuerpo: d })
        : await api(`/admin/clientes/${idCliente}`, { metodo: 'PUT', cuerpo: d });
      m.cerrar();
      toast(nuevo ? `Cliente ${r.resumen.codigo} registrado. Código QR generado.` : 'Datos del cliente actualizados');
      refrescarLista();
      alGuardar?.(r);
      if (nuevo) abrirDetalleCliente(r.resumen.idCliente);
    } catch (err) {
      mostrarErrores(form, err);
    }
  }));
}

// ----------------------------------------------------------------- Detalle
export async function abrirDetalleCliente(idCliente) {
  const m = modal({ titulo: 'Detalle del cliente', ancho: 'ancho', contenido: cargando() });
  const pintar = async () => {
    const d = await api(`/admin/clientes/${idCliente}`);
    const c = d.resumen;
    m.el.querySelector('.modal-cabecera h3').textContent = `${c.nombre} ${c.apellido}`;
    m.cuerpo.innerHTML = `
      <div class="detalle-cabecera">
        ${avatar(iniciales(c.nombre, c.apellido), c.nombre + c.apellido)}
        <div style="flex:1;min-width:0">
          <h2>${esc(c.nombre)} ${esc(c.apellido)}</h2>
          <div class="c-apagado" style="font-size:13px">${esc(c.codigo)} · DNI ${esc(c.dni)} · ${esc(c.email)}</div>
          <div style="display:flex;gap:6px;margin-top:8px;flex-wrap:wrap">
            ${c.activo ? insignia(c.estadoMembresia) : insignia('INACTIVO', 'Cuenta desactivada')}
            ${d.riesgo ? insignia(d.riesgo, 'Riesgo ' + fmt.capital(d.riesgo) + (d.probabilidadRiesgo !== null ? ` · ${d.probabilidadRiesgo.toFixed(2)}` : '')) : ''}
          </div>
        </div>
        <div class="qr-caja" title="${esc(d.codigoQR)}">${qrImagen(d.codigoQR, 4)}</div>
      </div>
      <div class="pestanas no-imprimir">
        <button class="activo" data-tab="datos">Datos</button>
        <button data-tab="membresias">Membresías (${d.membresias.length})</button>
        <button data-tab="pagos">Pagos (${d.pagos.length})</button>
        <button data-tab="accesos">Asistencias</button>
      </div>
      <div data-panel="datos">
        <div class="datos-lista">
          <div><small>Teléfono</small>${esc(c.telefono || '—')}</div>
          <div><small>Entrenador</small>${esc(c.entrenador || 'Sin asignar')}</div>
          <div><small>Objetivo</small>${esc(d.objetivo || '—')}</div>
          <div><small>Nivel</small>${fmt.nivel(d.nivel)}</div>
          <div><small>Altura</small>${d.altura ? d.altura + ' m' : '—'}</div>
          <div><small>Fecha de nacimiento</small>${fmt.fecha(d.fechaNacimiento)}</div>
          <div><small>Registro</small>${fmt.fecha(d.fechaRegistro)}</div>
          <div><small>Plan actual</small>${esc(c.plan || '—')} ${c.fechaCaducidad ? '· vence ' + fmt.fecha(c.fechaCaducidad) : ''}</div>
        </div>
      </div>
      <div data-panel="membresias" class="oculto">
        <table class="tabla"><thead><tr><th>Plan</th><th>Inicio</th><th>Vencimiento</th><th>Estado</th></tr></thead><tbody>
          ${d.membresias.map(s => `<tr><td>${esc(s.plan)}</td><td class="mono">${fmt.fecha(s.fechaInicio)}</td><td class="mono">${fmt.fecha(s.fechaCaducidad)}</td><td>${insignia(s.estado)}</td></tr>`).join('') || tablaVacia(4)}
        </tbody></table>
      </div>
      <div data-panel="pagos" class="oculto">
        <table class="tabla"><thead><tr><th>Fecha</th><th>Plan</th><th>Método</th><th class="num">Monto</th><th>Estado</th></tr></thead><tbody>
          ${d.pagos.map(p => `<tr><td class="mono">${fmt.fecha(p.fechaPago)}</td><td>${esc(p.plan)}</td><td>${fmt.metodo(p.metodoPago)}</td><td class="num">${fmt.soles(p.monto)}</td><td>${insignia(p.estado)}</td></tr>`).join('') || tablaVacia(5)}
        </tbody></table>
      </div>
      <div data-panel="accesos" class="oculto">
        <table class="tabla"><thead><tr><th>Fecha y hora</th><th>Registro</th></tr></thead><tbody>
          ${d.ultimosAccesos.map(a => `<tr><td class="mono">${fmt.fechaHora(a.fechaHora)}</td><td>${insignia(a.tipo)}</td></tr>`).join('') || tablaVacia(2, 'Sin asistencias registradas')}
        </tbody></table>
      </div>
      <div style="display:flex;gap:10px;flex-wrap:wrap;margin-top:20px" class="no-imprimir">
        <button class="btn btn-dorado" data-accion="pago">${icono('dolar')} Registrar pago / renovar</button>
        <button class="btn btn-borde" data-accion="editar">${icono('lapiz')} Editar</button>
        <button class="btn btn-borde" data-accion="qr">${icono('qr')} Regenerar QR</button>
        <button class="btn ${c.activo ? 'btn-peligro' : 'btn-turquesa'}" data-accion="estado" style="margin-left:auto">${c.activo ? 'Desactivar cuenta' : 'Reactivar cuenta'}</button>
      </div>`;
    m.cuerpo.querySelectorAll('[data-tab]').forEach(b => b.addEventListener('click', () => {
      m.cuerpo.querySelectorAll('[data-tab]').forEach(x => x.classList.toggle('activo', x === b));
      m.cuerpo.querySelectorAll('[data-panel]').forEach(p => p.classList.toggle('oculto', p.dataset.panel !== b.dataset.tab));
    }));
    m.cuerpo.querySelector('[data-accion="pago"]').addEventListener('click', () =>
      abrirRegistroPago({ idCliente, alGuardar: () => { pintar(); refrescarLista(); } }));
    m.cuerpo.querySelector('[data-accion="editar"]').addEventListener('click', () => { m.cerrar(); abrirFormularioCliente(idCliente); });
    m.cuerpo.querySelector('[data-accion="qr"]').addEventListener('click', async () => {
      if (!await confirmar('El código QR actual dejará de funcionar. El cliente verá el nuevo código en su app.', { boton: 'Regenerar' })) return;
      await api(`/admin/clientes/${idCliente}/regenerar-qr`, { metodo: 'POST' }).catch(errorToast);
      toast('Nuevo código QR generado');
      pintar();
    });
    m.cuerpo.querySelector('[data-accion="estado"]').addEventListener('click', async () => {
      const activar = !c.activo;
      if (!activar && !await confirmar('El cliente no podrá iniciar sesión ni ingresar al gimnasio. Su historial se conserva (baja lógica).', { boton: 'Desactivar', peligro: true })) return;
      await api(`/admin/clientes/${idCliente}/estado`, { metodo: 'PATCH', cuerpo: { activo: activar } }).catch(errorToast);
      toast(activar ? 'Cuenta reactivada' : 'Cuenta desactivada');
      pintar();
      refrescarLista();
    });
  };
  pintar().catch(e => { m.cerrar(); errorToast(e); });
}

// ----------------------------------------------------------------- Registrar pago (BPM Fig. 5)
export async function abrirRegistroPago({ idCliente = null, idSuscripcion = null, alGuardar } = {}) {
  const [planes, clientes] = await Promise.all([api('/admin/planes'), idCliente ? Promise.resolve(null) : api('/admin/clientes')]);
  const m = modal({
    titulo: idSuscripcion ? 'Registrar pago de membresía pendiente' : 'Registrar pago en recepción',
    subtitulo: 'Efectivo, tarjeta o Yape/Plin · el sistema no procesa pagos en línea',
    contenido: `
      <form id="f-pago" novalidate>
        <div class="aviso error oculto" data-error-general></div>
        <div class="aviso ok oculto" id="resultado"></div>
        ${idCliente ? `<input type="hidden" name="idCliente" value="${idCliente}">` : campo('Cliente *', `<select class="entrada" name="idCliente">${opciones(clientes.filter(c => c.activo).map(c => [c.idCliente, `${c.codigo} · ${c.nombre} ${c.apellido}`]), '', { vacio: 'Seleccione el cliente' })}</select>`)}
        ${idSuscripcion ? `<input type="hidden" name="idSuscripcion" value="${idSuscripcion}">` : campo('Plan *', `<select class="entrada" name="idPlan">${opciones(planes.map(p => [p.idPlan, `${p.nombre} · ${fmt.soles(p.precio)} / ${p.duracionDias} días`]), planes[0]?.idPlan)}</select>`)}
        <div class="form-2">
          ${campo('Método de pago *', `<select class="entrada" name="metodoPago">${opciones([['YAPE_PLIN', 'Yape / Plin'], ['TARJETA', 'Tarjeta'], ['EFECTIVO', 'Efectivo']], 'YAPE_PLIN')}</select>`)}
          ${campo('Resultado del pago', `<select class="entrada" name="estado">${opciones([['VALIDADO', 'Validado'], ['RECHAZADO', 'Rechazado']], 'VALIDADO')}</select>`)}
        </div>
        ${campo('Monto (S/)', '<input class="entrada" name="monto" type="number" step="0.01" min="0.01" placeholder="Por defecto, el precio del plan">')}
        <p class="c-apagado" style="font-size:12.5px;margin:0">Si el cliente tiene una membresía vigente, la renovación empieza al día siguiente de su vencimiento.</p>
      </form>`,
    pie: `<button class="btn btn-borde" data-cerrar>Cerrar</button><button class="btn btn-dorado" id="registrar">${icono('dolar')} Registrar pago</button>`
  });
  const form = m.el.querySelector('#f-pago');
  const boton = m.el.querySelector('#registrar');
  boton.addEventListener('click', () => conCarga(boton, async () => {
    const d = leerFormulario(form);
    ['idCliente', 'idPlan', 'idSuscripcion'].forEach(k => { if (d[k]) d[k] = Number(d[k]); });
    if (!d.idCliente) return mostrarErrores(form, { message: 'Seleccione el cliente', campos: { idCliente: 'Seleccione el cliente' } });
    try {
      const r = await api('/admin/pagos', { metodo: 'POST', cuerpo: d });
      const aviso = form.querySelector('#resultado');
      aviso.textContent = r.mensaje;
      aviso.className = 'aviso ' + (r.pago.estado === 'VALIDADO' ? 'ok' : 'error');
      mostrarErrores(form, null);
      toast(r.pago.estado === 'VALIDADO' ? 'Pago registrado y membresía actualizada' : 'Pago rechazado registrado', r.pago.estado === 'VALIDADO' ? 'ok' : 'error');
      boton.classList.add('oculto');
      alGuardar?.(r);
    } catch (err) {
      mostrarErrores(form, err);
    }
  }));
}
