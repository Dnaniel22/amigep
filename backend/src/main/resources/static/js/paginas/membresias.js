// Membresías (RF-04): planes del catálogo y suscripciones de los clientes
import { api } from '../api.js';
import { icono, esc, fmt, insignia, cargando, tablaVacia, opciones, errorToast, toast, confirmar, debounce } from '../ui.js';
import { abrirRegistroPago, abrirDetalleCliente } from './clientes.js';

const ESTADOS = [['', 'Todas'], ['VIGENTE', 'Vigentes'], ['POR_VENCER', 'Por vencer'], ['VENCIDA', 'Vencidas'], ['PROGRAMADA', 'Programadas'], ['SUSPENDIDA', 'Suspendidas']];

export async function render(el) {
  const [planes, todas] = await Promise.all([api('/admin/planes'), api('/admin/membresias')]);
  const vigentesPorPlan = {};
  todas.filter(m => m.estado === 'VIGENTE' || m.estado === 'POR_VENCER')
    .forEach(m => { vigentesPorPlan[m.idPlan] = (vigentesPorPlan[m.idPlan] || 0) + 1; });
  let estado = 'POR_VENCER';

  el.innerHTML = `
    <div class="cabecera">
      <div><h1>Membresías</h1><p>RF-04 · planes del gimnasio y vigencia de las suscripciones (clasificación RP-02)</p></div>
      <div class="acciones"><button class="btn btn-dorado" id="renovar">${icono('dolar')} Registrar pago / renovar</button></div>
    </div>
    <section class="rejilla-3" style="margin-bottom:18px">
      ${planes.map(p => `
        <div class="tarjeta plan-tarjeta ${p.nombre.toUpperCase() === 'VIP' ? 'vip' : ''}">
          <div class="c-apagado" style="font-size:11.5px;font-weight:700;text-transform:uppercase">Plan ${esc(p.nombre)}</div>
          <div class="precio">${fmt.soles(p.precio)} <small>/ ${p.duracionDias} días</small></div>
          <div class="c-apagado" style="font-size:12.5px;min-height:18px">${esc(p.descripcion || '')}</div>
          <div style="margin-top:12px">${insignia('VIGENTE', `${vigentesPorPlan[p.idPlan] || 0} membresías activas`)}</div>
        </div>`).join('')}
    </section>
    <div class="barra-filtros">
      <div class="chips" id="chips">${ESTADOS.map(([v, t]) => `<button class="chip ${v === estado ? 'activo' : ''}" data-estado="${v}">${t}</button>`).join('')}</div>
      <div class="buscador" style="margin-left:auto">${icono('buscar')}<input class="entrada" id="q" placeholder="Buscar cliente..."></div>
      <div class="select-filtro">${icono('filtro')}<select class="entrada" id="f-plan">${opciones(planes.map(p => [p.idPlan, p.nombre]), '', { vacio: 'Plan' })}</select></div>
    </div>
    <div class="tarjeta" style="padding:6px 14px">
      <div class="tabla-envoltura">
        <table class="tabla">
          <thead><tr><th>Cliente</th><th>Plan</th><th>Inicio</th><th>Vencimiento</th><th>Días restantes</th><th>Pago</th><th>Estado</th><th></th></tr></thead>
          <tbody id="filas"><tr><td colspan="8">${cargando()}</td></tr></tbody>
        </table>
      </div>
    </div>`;

  const cargar = async () => {
    const lista = await api('/admin/membresias', { params: { estado, q: el.querySelector('#q').value, idPlan: el.querySelector('#f-plan').value } });
    el.querySelector('#filas').innerHTML = lista.length ? lista.slice(0, 300).map(m => `
      <tr>
        <td><a href="#" data-cliente="${m.idCliente}"><span class="mono">${esc(m.codigoCliente)}</span> · ${esc(m.cliente)}</a></td>
        <td>${esc(m.plan)}</td>
        <td class="mono">${fmt.fecha(m.fechaInicio)}</td>
        <td class="mono">${fmt.fecha(m.fechaCaducidad)}</td>
        <td class="mono">${m.estado === 'VENCIDA' ? '—' : m.diasRestantes}</td>
        <td>${m.pagada ? insignia('VALIDADO', 'Pagada') : insignia('PENDIENTE', 'Pendiente')}</td>
        <td>${insignia(m.estado)} ${m.renovada && m.estado !== 'VENCIDA' ? insignia('VIGENTE', 'Renovada') : ''}</td>
        <td class="num" style="white-space:nowrap">
          ${!m.pagada ? `<button class="btn btn-borde btn-chico" data-pagar="${m.idSuscripcion}" data-cli="${m.idCliente}">Registrar pago</button>` : ''}
          ${(m.estado === 'POR_VENCER' || m.estado === 'VENCIDA') && !m.renovada ? `<button class="btn btn-borde btn-chico" data-renovar="${m.idCliente}">Renovar</button>` : ''}
          ${m.pagada && m.estado !== 'VENCIDA' ? `<button class="btn btn-borde btn-chico" data-estado-sus="${m.idSuscripcion}" data-activo="${m.estado !== 'SUSPENDIDA'}">${m.estado === 'SUSPENDIDA' ? 'Reactivar' : 'Suspender'}</button>` : ''}
        </td>
      </tr>`).join('') : tablaVacia(8, 'No hay membresías con este filtro');
    el.querySelectorAll('[data-cliente]').forEach(a => a.addEventListener('click', e => { e.preventDefault(); abrirDetalleCliente(Number(a.dataset.cliente)); }));
    el.querySelectorAll('[data-renovar]').forEach(b => b.addEventListener('click', () => abrirRegistroPago({ idCliente: Number(b.dataset.renovar), alGuardar: refrescar })));
    el.querySelectorAll('[data-pagar]').forEach(b => b.addEventListener('click', () =>
      abrirRegistroPago({ idCliente: Number(b.dataset.cli), idSuscripcion: Number(b.dataset.pagar), alGuardar: refrescar })));
    el.querySelectorAll('[data-estado-sus]').forEach(b => b.addEventListener('click', async () => {
      const suspender = b.dataset.activo === 'true';
      if (suspender && !await confirmar('El cliente no podrá ingresar mientras la membresía esté suspendida.', { boton: 'Suspender', peligro: true })) return;
      try {
        await api(`/admin/membresias/${b.dataset.estadoSus}/estado`, { metodo: 'PATCH', cuerpo: { activo: !suspender } });
        toast(suspender ? 'Membresía suspendida' : 'Membresía reactivada');
        refrescar();
      } catch (e) { errorToast(e); }
    }));
  };
  const refrescar = () => cargar().catch(errorToast);
  el.querySelector('#chips').addEventListener('click', e => {
    const b = e.target.closest('[data-estado]');
    if (!b) return;
    estado = b.dataset.estado;
    el.querySelectorAll('#chips .chip').forEach(c => c.classList.toggle('activo', c === b));
    refrescar();
  });
  el.querySelector('#q').addEventListener('input', debounce(refrescar));
  el.querySelector('#f-plan').addEventListener('change', refrescar);
  el.querySelector('#renovar').addEventListener('click', () => abrirRegistroPago({ alGuardar: refrescar }));
  await cargar();
}
