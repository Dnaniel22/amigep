// A4 · Reportes del sistema (RF-16, RF-20 · sección 13). Bajo demanda, con filtros y sin modificar registros.
import { api, descargar } from '../api.js';
import { icono, esc, fmt, insignia, cargando, tablaVacia, modal, campo, opciones, leerFormulario, conCarga, errorToast } from '../ui.js';

const ICONOS = { 'RP-01': 'usuarios', 'RP-02': 'tarjeta', 'RP-03': 'dolar', 'RP-04': 'qr', 'RP-05': 'pesa', 'RP-08': 'panel' };
const rango = { desde: '', hasta: '' };

export async function render(el) {
  const catalogo = await api('/reportes');
  el.innerHTML = `
    <div class="cabecera">
      <div><h1>Reportes del sistema</h1><p>Sección 13 — reportes autorizados para el administrador: RP-01 a RP-05 y RP-08</p></div>
      <div class="acciones"><button class="btn btn-borde" id="rango">${icono('calendario')} <span id="rango-texto">Rango de fechas</span></button></div>
    </div>
    <section class="rejilla-3">
      ${catalogo.map(r => `
        <div class="tarjeta reporte-tarjeta">
          <div class="arriba"><div class="icono-caja">${icono(ICONOS[r.codigo] || 'archivo')}</div><span class="etiqueta-rp">${r.codigo}</span></div>
          <h3>${esc(r.titulo)}</h3>
          <p>${esc(r.descripcion)}</p>
          <button class="btn btn-borde btn-bloque" data-rp="${r.codigo}">Generar reporte</button>
        </div>`).join('')}
    </section>
    <p class="c-apagado" style="font-size:12.5px;margin-top:16px">RP-06 (progreso) y RP-07 (seguimiento alimenticio) se consultan desde la app del cliente y del entrenador.</p>`;

  el.querySelector('#rango').addEventListener('click', () => {
    const m = modal({
      titulo: 'Rango de fechas',
      subtitulo: 'Se aplica a los reportes con filtro por periodo',
      contenido: `<form id="f-rango" class="form-2">
        ${campo('Desde', `<input class="entrada" type="date" name="desde" value="${rango.desde}">`)}
        ${campo('Hasta', `<input class="entrada" type="date" name="hasta" value="${rango.hasta}">`)}</form>`,
      pie: '<button class="btn btn-borde" id="limpiar">Quitar rango</button><button class="btn btn-turquesa" id="aplicar">Aplicar</button>'
    });
    const aplicar = (d, h) => {
      rango.desde = d || ''; rango.hasta = h || '';
      el.querySelector('#rango-texto').textContent = rango.desde || rango.hasta ? `${fmt.fecha(rango.desde) || '…'} – ${fmt.fecha(rango.hasta) || '…'}` : 'Rango de fechas';
      m.cerrar();
    };
    m.el.querySelector('#aplicar').addEventListener('click', () => { const d = leerFormulario(m.el.querySelector('#f-rango')); aplicar(d.desde, d.hasta); });
    m.el.querySelector('#limpiar').addEventListener('click', () => aplicar('', ''));
  });
  el.querySelectorAll('[data-rp]').forEach(b => b.addEventListener('click', () => abrirReporte(catalogo.find(r => r.codigo === b.dataset.rp))));
}

async function filtrosDe(codigo) {
  const planes = () => api('/admin/planes').then(ps => ps.map(p => [p.idPlan, p.nombre]));
  const fechas = (etq = 'Periodo') => `<div class="form-2">
      ${campo(`${etq} desde`, `<input class="entrada" type="date" name="desde" value="${rango.desde}">`)}
      ${campo(`${etq} hasta`, `<input class="entrada" type="date" name="hasta" value="${rango.hasta}">`)}</div>`;
  switch (codigo) {
    case 'RP-01': return `<div class="form-2">${campo('Cliente (nombre o código)', '<input class="entrada" name="q">')}
      ${campo('Plan', `<select class="entrada" name="idPlan">${opciones(await planes(), '', { vacio: 'Todos' })}</select>`)}</div>`;
    case 'RP-02': return `<div class="form-2">${campo('Cliente', '<input class="entrada" name="q">')}
      ${campo('Plan', `<select class="entrada" name="idPlan">${opciones(await planes(), '', { vacio: 'Todos' })}</select>`)}
      ${campo('Estado', `<select class="entrada" name="estado">${opciones([['VIGENTE', 'Vigente'], ['POR_VENCER', 'Por vencer'], ['VENCIDA', 'Vencida'], ['PROGRAMADA', 'Programada'], ['SUSPENDIDA', 'Suspendida']], 'POR_VENCER', { vacio: 'Todos' })}</select>`)}
      ${campo('Días de anticipación', '<input class="entrada" type="number" name="diasAnticipacion" value="7" min="1" max="60">')}</div>${fechas('Vencimiento')}`;
    case 'RP-03': return `${fechas()}<div class="form-2">
      ${campo('Plan', `<select class="entrada" name="idPlan">${opciones(await planes(), '', { vacio: 'Todos' })}</select>`)}
      ${campo('Método', `<select class="entrada" name="metodo">${opciones([['YAPE_PLIN', 'Yape / Plin'], ['TARJETA', 'Tarjeta'], ['EFECTIVO', 'Efectivo']], '', { vacio: 'Todos' })}</select>`)}</div>`;
    case 'RP-04': {
      const clientes = await api('/admin/clientes');
      return `${fechas()}${campo('Cliente', `<select class="entrada" name="idCliente">${opciones(clientes.map(c => [c.idCliente, `${c.codigo} · ${c.nombre} ${c.apellido}`]), '', { vacio: 'Todos' })}</select>`)}`;
    }
    case 'RP-05': {
      const entrenadores = await api('/admin/trabajadores', { params: { rol: 'ENTRENADOR' } });
      return `<div class="form-2">
        ${campo('Entrenador', `<select class="entrada" name="idEntrenador">${opciones(entrenadores.map(e => [e.idTrabajador, `${e.nombre} ${e.apellido}`]), '', { vacio: 'Todos' })}</select>`)}
        ${campo('Origen', `<select class="entrada" name="origen">${opciones([['IA', 'Módulo IA'], ['ENTRENADOR', 'Entrenador']], '', { vacio: 'Todos' })}</select>`)}
        ${campo('Nivel', `<select class="entrada" name="nivel">${opciones([['PRINCIPIANTE', 'Principiante'], ['INTERMEDIO', 'Intermedio'], ['AVANZADO', 'Avanzado']], '', { vacio: 'Todos' })}</select>`)}
        ${campo('Estado', `<select class="entrada" name="estado">${opciones([['ACTIVA', 'Activa'], ['INACTIVA', 'Inactiva']], 'ACTIVA', { vacio: 'Todos' })}</select>`)}
        ${campo('Objetivo', `<select class="entrada" name="objetivo">${opciones(['Masa muscular', 'Pérdida de grasa', 'Fuerza', 'Resistencia'].map(o => [o, o]), '', { vacio: 'Todos' })}</select>`)}
      </div>${fechas('Creación')}`;
    }
    default: return '<p class="c-apagado" style="margin:0">Este reporte no requiere filtros: muestra los indicadores consolidados del gimnasio.</p>';
  }
}

async function abrirReporte(rp) {
  const m = modal({
    titulo: `${rp.codigo} · ${rp.titulo}`, subtitulo: rp.descripcion, ancho: 'extra',
    contenido: `<form id="f-filtros" class="no-imprimir">${await filtrosDe(rp.codigo)}</form><div id="resultado"></div>`,
    pie: `<button class="btn btn-borde" id="imprimir" disabled>${icono('imprimir')} Imprimir</button>
          <button class="btn btn-borde" id="csv">${icono('descargar')} Exportar CSV</button>
          <button class="btn btn-turquesa" id="generar">${icono('grafico')} Generar</button>`
  });
  const form = m.el.querySelector('#f-filtros');
  const salida = m.el.querySelector('#resultado');
  const filtros = () => leerFormulario(form);
  const generar = () => conCarga(m.el.querySelector('#generar'), async () => {
    salida.innerHTML = cargando();
    try {
      const r = await api(`/reportes/${rp.codigo}`, { params: filtros() });
      salida.innerHTML = pintarReporte(r);
      m.el.querySelector('#imprimir').disabled = false;
    } catch (e) {
      salida.innerHTML = `<div class="aviso error">${esc(e.message)}</div>`;
    }
  });
  m.el.querySelector('#generar').addEventListener('click', generar);
  m.el.querySelector('#csv').addEventListener('click', () =>
    descargar(`/reportes/${rp.codigo}`, { ...filtros(), formato: 'csv' }, `${rp.codigo}.csv`).catch(errorToast));
  m.el.querySelector('#imprimir').addEventListener('click', () => window.print());
  generar();
}

/** Claves tipo enum (POR_VENCER) se muestran legibles; las demás tal cual ("Yape / Plin"). */
const etiquetaClave = k => (/^[A-Z_]+$/.test(k) ? fmt.capital(k) : k);

function valorResumen(v, moneda = false) {
  if (v === null || v === undefined) return '—';
  if (Array.isArray(v)) {
    if (!v.length) return '—';
    return typeof v[0] === 'object' ? `${v.length} registros` : v.map(esc).join(' · ');
  }
  if (typeof v === 'object') {
    return Object.entries(v).map(([k, x]) => `${esc(etiquetaClave(k))}: ${moneda ? fmt.soles(x) : esc(x)}`).join(' · ');
  }
  if (typeof v === 'number') return moneda ? fmt.soles(v) : fmt.numero(v);
  return esc(v);
}

const CLAVES_MONEDA = ['subtotalesPorMetodo', 'totalPeriodo'];

const ETIQUETAS = {
  totalClientesActivos: 'Clientes activos', porPlan: 'Por plan', total: 'Total', porEstado: 'Por estado', diasAnticipacion: 'Días de anticipación',
  cantidadPagos: 'Pagos validados', pagosRechazados: 'Pagos rechazados', subtotalesPorMetodo: 'Subtotales por método', totalPeriodo: 'Total del periodo',
  totalIngresos: 'Total de ingresos', diasConAsistencia: 'Días con asistencia', clientesDistintos: 'Clientes distintos', totalRutinas: 'Rutinas',
  porOrigen: 'Por origen', porNivel: 'Por nivel', generadoPara: 'Vista'
};

function celda(valor, tipo) {
  if (valor === null || valor === undefined || valor === '') return '<span class="c-apagado">—</span>';
  switch (tipo) {
    case 'fecha': return `<span class="mono">${fmt.fecha(valor)}</span>`;
    case 'moneda': return fmt.soles(valor);
    case 'porcentaje': return `${valor}%`;
    case 'numero': return fmt.numero(valor);
    case 'estado': return insignia(valor);
    default: return esc(valor);
  }
}

function pintarReporte(r) {
  const resumen = Object.entries(r.resumen || {}).filter(([k]) => k !== 'totalesDiarios');
  return `
    <div style="display:flex;align-items:center;gap:10px;margin:6px 0 14px">
      <h3 style="margin:0;font-size:15px">${esc(r.titulo)}</h3>
      <span class="c-apagado" style="font-size:12px;margin-left:auto">Generado: ${fmt.fechaHora(r.generado)} · ${r.filas.length} filas</span>
    </div>
    <p class="c-apagado" style="font-size:12.5px;margin:-6px 0 14px">${esc(r.descripcion)}${r.filtros?.desde ? ` · Periodo: ${fmt.fecha(r.filtros.desde)} – ${fmt.fecha(r.filtros.hasta)}` : ''}</p>
    ${resumen.length ? `<div class="resumen-reporte">${resumen.map(([k, v]) => `
      <div class="dato"><small>${esc(ETIQUETAS[k] || fmt.capital(k.replace(/([A-Z])/g, ' $1')))}</small>
        <strong>${valorResumen(v, CLAVES_MONEDA.includes(k))}</strong></div>`).join('')}</div>` : ''}
    <div class="tabla-envoltura" style="max-height:52vh;overflow:auto">
      <table class="tabla">
        <thead><tr>${r.columnas.map(c => `<th class="${['moneda', 'numero', 'porcentaje'].includes(c.tipo) ? 'num' : ''}">${esc(c.titulo)}</th>`).join('')}</tr></thead>
        <tbody>${r.filas.length ? r.filas.map(f => `<tr>${r.columnas.map(c =>
          `<td class="${['moneda', 'numero', 'porcentaje'].includes(c.tipo) ? 'num' : ''}" ${c.clave === 'detalle' ? 'style="font-size:12px;color:var(--texto-2);min-width:280px"' : ''}>${celda(f[c.clave], c.tipo)}</td>`).join('')}</tr>`).join('')
          : tablaVacia(r.columnas.length, 'No hay datos con los filtros seleccionados')}</tbody>
      </table>
    </div>`;
}
