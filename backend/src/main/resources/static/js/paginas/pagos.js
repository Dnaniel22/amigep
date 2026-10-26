// Pagos (RF-05): pagos registrados en recepción, con filtros por periodo y método
import { api, descargar } from '../api.js';
import { icono, esc, fmt, insignia, cargando, tablaVacia, opciones, errorToast, debounce } from '../ui.js';
import { abrirRegistroPago } from './clientes.js';

export async function render(el) {
  const hoy = new Date();
  const inicioMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
  el.innerHTML = `
    <div class="cabecera">
      <div><h1>Pagos</h1><p>RF-05 · pagos en recepción (efectivo, tarjeta o Yape/Plin) · el sistema no procesa pagos en línea</p></div>
      <div class="acciones">
        <button class="btn btn-borde" id="csv">${icono('descargar')} Exportar RP-03</button>
        <button class="btn btn-dorado" id="nuevo">${icono('mas')} Registrar pago</button>
      </div>
    </div>
    <div class="barra-filtros">
      <div class="campo" style="margin:0"><label>Desde</label><input class="entrada" type="date" id="desde" value="${fmt.iso(inicioMes)}"></div>
      <div class="campo" style="margin:0"><label>Hasta</label><input class="entrada" type="date" id="hasta" value="${fmt.iso(hoy)}"></div>
      <div class="campo" style="margin:0"><label>Método</label><select class="entrada" id="metodo">${opciones([['YAPE_PLIN', 'Yape / Plin'], ['TARJETA', 'Tarjeta'], ['EFECTIVO', 'Efectivo']], '', { vacio: 'Todos' })}</select></div>
      <div class="campo" style="margin:0;flex:1;max-width:320px"><label>Cliente</label><input class="entrada" id="q" placeholder="Nombre del cliente"></div>
    </div>
    <section class="kpis" id="kpis"></section>
    <div class="tarjeta" style="padding:6px 14px">
      <div class="tabla-envoltura">
        <table class="tabla">
          <thead><tr><th>Código</th><th>Fecha</th><th>Cliente</th><th>Plan</th><th>Método</th><th class="num">Monto</th><th>Estado</th></tr></thead>
          <tbody id="filas"><tr><td colspan="7">${cargando()}</td></tr></tbody>
        </table>
      </div>
    </div>`;
  const filtros = () => ({
    desde: el.querySelector('#desde').value, hasta: el.querySelector('#hasta').value,
    metodo: el.querySelector('#metodo').value, q: el.querySelector('#q').value
  });
  const cargar = async () => {
    const lista = await api('/admin/pagos', { params: filtros() });
    const validados = lista.filter(p => p.estado === 'VALIDADO');
    const total = validados.reduce((s, p) => s + Number(p.monto), 0);
    const porMetodo = m => validados.filter(p => p.metodoPago === m).reduce((s, p) => s + Number(p.monto), 0);
    el.querySelector('#kpis').innerHTML = `
      <div class="kpi"><div class="nombre">Total del periodo</div><div class="valor c-turquesa">${fmt.soles(total)}</div><div class="nota">${validados.length} pagos validados</div></div>
      <div class="kpi"><div class="nombre">Yape / Plin</div><div class="valor">${fmt.soles(porMetodo('YAPE_PLIN'))}</div></div>
      <div class="kpi"><div class="nombre">Tarjeta</div><div class="valor">${fmt.soles(porMetodo('TARJETA'))}</div></div>
      <div class="kpi"><div class="nombre">Efectivo</div><div class="valor">${fmt.soles(porMetodo('EFECTIVO'))}</div><div class="nota c-rojo">${lista.length - validados.length} rechazados</div></div>`;
    el.querySelector('#filas').innerHTML = lista.length ? lista.map(p => `
      <tr>
        <td class="mono">PG-${String(p.idPago).padStart(4, '0')}</td>
        <td class="mono">${fmt.fecha(p.fechaPago)}</td>
        <td>${esc(p.cliente)}</td><td>${esc(p.plan)}</td><td>${fmt.metodo(p.metodoPago)}</td>
        <td class="num">${fmt.soles(p.monto)}</td><td>${insignia(p.estado)}</td>
      </tr>`).join('') : tablaVacia(7, 'No hay pagos en el periodo seleccionado');
  };
  const refrescar = () => cargar().catch(errorToast);
  ['#desde', '#hasta', '#metodo'].forEach(s => el.querySelector(s).addEventListener('change', refrescar));
  el.querySelector('#q').addEventListener('input', debounce(refrescar));
  el.querySelector('#nuevo').addEventListener('click', () => abrirRegistroPago({ alGuardar: refrescar }));
  el.querySelector('#csv').addEventListener('click', () => {
    const f = filtros();
    descargar('/reportes/RP-03', { desde: f.desde, hasta: f.hasta, metodo: f.metodo, formato: 'csv' }, 'RP-03-pagos.csv').catch(errorToast);
  });
  await cargar();
}
