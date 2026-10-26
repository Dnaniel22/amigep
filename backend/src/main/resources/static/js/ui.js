// Componentes y utilidades de interfaz compartidos por las páginas del panel.

const RUTAS_ICONOS = {
  panel: '<path d="M21.2 15.9A10 10 0 1 1 8 2.8"/><path d="M22 12A10 10 0 0 0 12 2v10z"/>',
  usuarios: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  usuario: '<circle cx="12" cy="8" r="5"/><path d="M20 21a8 8 0 0 0-16 0"/>',
  tarjeta: '<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/>',
  dolar: '<path d="M12 2v20"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>',
  medalla: '<circle cx="12" cy="8" r="6"/><path d="M15.48 12.89 17 22l-5-3-5 3 1.52-9.11"/>',
  ia: '<path d="M12 2v4"/><path d="m16.2 7.8 2.9-2.9"/><path d="M18 12h4"/><path d="m16.2 16.2 2.9 2.9"/><path d="M12 18v4"/><path d="m4.9 19.1 2.9-2.9"/><path d="M2 12h4"/><path d="m4.9 4.9 2.9 2.9"/>',
  archivo: '<path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><path d="M14 2v6h6"/>',
  config: '<circle cx="12" cy="12" r="3.2"/><path d="M12 1.5v3M12 19.5v3M4.6 4.6l2.1 2.1M17.3 17.3l2.1 2.1M1.5 12h3M19.5 12h3M4.6 19.4l2.1-2.1M17.3 6.7l2.1-2.1"/>',
  qr: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><path d="M14 14h3v3h-3zM21 14v.01M14 21h.01M17.5 21H21v-3.5"/>',
  salir: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/>',
  calendario: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>',
  descargar: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m7 10 5 5 5-5"/><path d="M12 15V3"/>',
  tendencia: '<path d="m22 7-8.5 8.5-5-5L2 17"/><path d="M16 7h6v6"/>',
  buscar: '<circle cx="11" cy="11" r="7.5"/><path d="m21 21-4.3-4.3"/>',
  filtro: '<path d="M3 6h18M7 12h10M10 18h4"/>',
  mas: '<path d="M12 5v14M5 12h14"/>',
  lapiz: '<path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>',
  derecha: '<path d="m9 18 6-6-6-6"/>',
  cerrar: '<path d="M18 6 6 18M6 6l12 12"/>',
  campana: '<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>',
  telefono: '<path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8.1 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2z"/>',
  pesa: '<path d="M6.5 6.5v11M17.5 6.5v11M3 9.5v5M21 9.5v5M6.5 12h11"/>',
  etiqueta: '<path d="M12.6 2.6A2 2 0 0 0 11.2 2H4a2 2 0 0 0-2 2v7.2a2 2 0 0 0 .6 1.4l8.7 8.7a2.4 2.4 0 0 0 3.4 0l6.6-6.6a2.4 2.4 0 0 0 0-3.4z"/><circle cx="7.5" cy="7.5" r="1.5"/>',
  mensaje: '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>',
  reloj: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
  camara: '<path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/>',
  check: '<path d="M20 6 9 17l-5-5"/>',
  alerta: '<circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>',
  refrescar: '<path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/>',
  llave: '<circle cx="7.5" cy="15.5" r="5.5"/><path d="m21 2-9.6 9.6M15.5 7.5l3 3L22 7l-3-3"/>',
  imprimir: '<path d="M6 9V2h12v7"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/>',
  escudo: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
  grafico: '<path d="M3 3v18h18"/><path d="M18 17V9M13 17V5M8 17v-3"/>',
  rayo: '<path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"/>'
};

export function icono(nombre, clase = '') {
  return `<svg class="ico ${clase}" viewBox="0 0 24 24" aria-hidden="true">${RUTAS_ICONOS[nombre] || ''}</svg>`;
}

/** Escapa texto para insertarlo en HTML. */
export function esc(valor) {
  if (valor === null || valor === undefined) return '';
  return String(valor).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// ----------------------------------------------------------------- Formatos
const numero = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 });
export const fmt = {
  soles: v => 'S/ ' + numero.format(Number(v || 0)),
  numero: v => numero.format(Number(v || 0)),
  fecha: v => {
    if (!v) return '—';
    const [a, m, d] = String(v).slice(0, 10).split('-');
    return `${d}/${m}/${a}`;
  },
  fechaHora: v => {
    if (!v) return '—';
    const f = new Date(v);
    return f.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }) + ' · ' + fmt.hora(v);
  },
  hora: v => new Date(v).toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit' }),
  fechaLarga: (f = new Date()) => f.toLocaleDateString('es-PE', { day: 'numeric', month: 'long', year: 'numeric' }),
  diaMes: v => {
    const f = new Date(String(v).length === 10 ? v + 'T12:00:00' : v);
    return f.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' }).replace('.', '');
  },
  pct: v => (v === null || v === undefined ? '—' : `${v > 0 ? '+' : ''}${numero.format(v)}%`),
  iso: f => {
    const d = new Date(f.getTime() - f.getTimezoneOffset() * 60000);
    return d.toISOString().slice(0, 10);
  },
  metodo: m => ({ YAPE_PLIN: 'Yape / Plin', TARJETA: 'Tarjeta', EFECTIVO: 'Efectivo' }[m] || m),
  nivel: n => ({ PRINCIPIANTE: 'Principiante', INTERMEDIO: 'Intermedio', AVANZADO: 'Avanzado' }[n] || n || '—'),
  capital: t => (t ? t.charAt(0).toUpperCase() + t.slice(1).toLowerCase().replaceAll('_', ' ') : '')
};

// ----------------------------------------------------------------- Insignias
const INSIGNIAS = {
  ACTIVO: ['i-verde', 'Activo'], VIGENTE: ['i-verde', 'Vigente'], POR_VENCER: ['i-dorado', 'Por vencer'],
  VENCIDO: ['i-rojo', 'Vencido'], VENCIDA: ['i-rojo', 'Vencida'], SIN_MEMBRESIA: ['i-gris', 'Sin membresía'],
  SUSPENDIDO: ['i-gris', 'Suspendido'], SUSPENDIDA: ['i-gris', 'Suspendida'], PROGRAMADA: ['i-gris', 'Programada'],
  ALTO: ['i-rojo', 'Alto'], MEDIO: ['i-dorado', 'Medio'], BAJO: ['i-verde', 'Bajo'],
  ENVIADA: ['i-verde', 'Enviada'], PENDIENTE: ['i-dorado', 'Pendiente'], RETENIDO: ['i-verde', 'Retenido'], NO_RETENIDO: ['i-rojo', 'No retenido'],
  VALIDADO: ['i-verde', 'Validado'], RECHAZADO: ['i-rojo', 'Rechazado'],
  ACTIVA: ['i-verde', 'Activa'], INACTIVA: ['i-gris', 'Inactiva'], FINALIZADA: ['i-gris', 'Finalizada'], POR_FINALIZAR: ['i-dorado', 'Por finalizar'],
  IA: ['i-verde', 'IA'], ENTRENADOR: ['i-dorado', 'Entrenador'], ADMINISTRADOR: ['i-dorado', 'Administrador'],
  QR: ['i-verde', 'QR'], MANUAL: ['i-gris', 'Manual'], HABILITADO: ['i-verde', 'Habilitado'],
  PRINCIPIANTE: ['i-gris', 'Principiante'], INTERMEDIO: ['i-gris', 'Intermedio'], AVANZADO: ['i-gris', 'Avanzado'],
  DESAYUNO: ['i-gris', 'Desayuno'], ALMUERZO: ['i-gris', 'Almuerzo'], CENA: ['i-gris', 'Cena'], REFRIGERIO: ['i-gris', 'Refrigerio'],
  VENCIMIENTO: ['i-dorado', 'Vencimiento'], RUTINA: ['i-verde', 'Rutina'], RETENCION: ['i-rojo', 'Retención'],
  INACTIVO: ['i-gris', 'Inactivo'], INACTIVA_CUENTA: ['i-gris', 'Desactivado']
};

export function insignia(estado, texto) {
  const [clase, etiqueta] = INSIGNIAS[estado] || ['i-gris', fmt.capital(estado || '—')];
  return `<span class="insignia ${clase}">${esc(texto || etiqueta)}</span>`;
}

const COLORES_AVATAR = ['#E3B34C', '#5B7FD6', '#D9744F', '#8B5CC9', '#3FB58E', '#C9A43A', '#C9567A', '#4FA3C9'];
export function avatar(iniciales, semilla = iniciales, tam = '') {
  let h = 0;
  for (const c of String(semilla)) h = (h * 31 + c.charCodeAt(0)) >>> 0;
  const color = COLORES_AVATAR[h % COLORES_AVATAR.length];
  return `<span class="avatar" style="background:${color};${tam ? `width:${tam}px;height:${tam}px` : ''}">${esc(iniciales)}</span>`;
}

export function iniciales(nombre = '', apellido = '') {
  return ((nombre.trim()[0] || '') + (apellido.trim()[0] || '')).toUpperCase();
}

/** Iniciales desde el nombre completo: "Luis Fernando Atocsa Tapia" → "LA", "Percy Gonzales Vera" → "PG". */
export function inicialesNombre(completo = '') {
  const p = completo.trim().split(/\s+/);
  if (p.length < 2) return (p[0] || '?').slice(0, 2).toUpperCase();
  return (p[0][0] + p[Math.ceil(p.length / 2) - (p.length === 3 ? 1 : 0)][0]).toUpperCase();
}

// ----------------------------------------------------------------- Toasts
export function toast(mensaje, tipo = 'ok') {
  const el = document.createElement('div');
  el.className = 'toast' + (tipo === 'error' ? ' error' : '');
  el.textContent = mensaje;
  document.getElementById('toasts').appendChild(el);
  setTimeout(() => el.remove(), 4200);
}

export function errorToast(e) {
  toast(e?.message || 'Ocurrió un error', 'error');
}

// ----------------------------------------------------------------- Modal
export function modal({ titulo, subtitulo = '', contenido = '', pie = '', ancho = '', alCerrar } = {}) {
  const fondo = document.createElement('div');
  fondo.className = 'modal-fondo';
  fondo.innerHTML = `
    <div class="modal ${ancho}" role="dialog" aria-modal="true" aria-label="${esc(titulo)}">
      <div class="modal-cabecera">
        <div><h3>${esc(titulo)}</h3>${subtitulo ? `<p>${esc(subtitulo)}</p>` : ''}</div>
        <button class="btn-icono" data-cerrar title="Cerrar">${icono('cerrar')}</button>
      </div>
      <div class="modal-cuerpo">${contenido}</div>
      ${pie ? `<div class="modal-pie">${pie}</div>` : ''}
    </div>`;
  const cerrar = () => {
    fondo.remove();
    document.removeEventListener('keydown', teclado);
    alCerrar?.();
  };
  const teclado = e => { if (e.key === 'Escape') cerrar(); };
  fondo.addEventListener('mousedown', e => { if (e.target === fondo) cerrar(); });
  fondo.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', cerrar));
  document.addEventListener('keydown', teclado);
  document.body.appendChild(fondo);
  const primero = fondo.querySelector('input:not([type=hidden]), select, textarea');
  primero?.focus();
  return { el: fondo, cuerpo: fondo.querySelector('.modal-cuerpo'), cerrar };
}

export function confirmar(mensaje, { titulo = 'Confirmar', boton = 'Confirmar', peligro = false } = {}) {
  return new Promise(resolver => {
    let respondido = false;
    const m = modal({
      titulo,
      contenido: `<p style="margin:0;color:var(--texto-2)">${esc(mensaje)}</p>`,
      pie: `<button class="btn btn-borde" data-cerrar>Cancelar</button>
            <button class="btn ${peligro ? 'btn-peligro' : 'btn-turquesa'}" data-ok>${esc(boton)}</button>`,
      alCerrar: () => { if (!respondido) resolver(false); }
    });
    m.el.querySelector('[data-ok]').addEventListener('click', () => { respondido = true; m.cerrar(); resolver(true); });
  });
}

// ----------------------------------------------------------------- Formularios
/** Convierte el formulario en objeto: números en campos type=number, null en vacíos. */
export function leerFormulario(form) {
  const datos = {};
  form.querySelectorAll('[name]').forEach(el => {
    if (el.type === 'checkbox') { datos[el.name] = el.checked; return; }
    if (el.type === 'radio') { if (el.checked) datos[el.name] = el.value; return; }
    const v = el.value.trim();
    datos[el.name] = v === '' ? null : (el.type === 'number' ? Number(v) : v);
  });
  return datos;
}

/** Marca los campos con error devueltos por la API (RNF-05). */
export function mostrarErrores(form, error) {
  form.querySelectorAll('.invalida').forEach(e => e.classList.remove('invalida'));
  form.querySelectorAll('.error-campo').forEach(e => { e.textContent = ''; });
  const general = form.querySelector('[data-error-general]');
  if (general) {
    general.textContent = error?.message || '';
    general.classList.toggle('oculto', !error);
  }
  Object.entries(error?.campos || {}).forEach(([campo, msg]) => {
    const el = form.querySelector(`[name="${campo}"]`);
    if (el) {
      el.classList.add('invalida');
      const nota = el.closest('.campo')?.querySelector('.error-campo');
      if (nota) nota.textContent = msg;
    }
  });
}

export function campo(etiqueta, control) {
  return `<div class="campo"><label>${esc(etiqueta)}</label>${control}<span class="error-campo"></span></div>`;
}

export function opciones(lista, seleccionado, { vacio } = {}) {
  return (vacio !== undefined ? `<option value="">${esc(vacio)}</option>` : '') +
    lista.map(([valor, texto]) => `<option value="${esc(valor)}" ${String(valor) === String(seleccionado ?? '') ? 'selected' : ''}>${esc(texto)}</option>`).join('');
}

/** Estado de carga en un botón mientras se ejecuta una acción. */
export async function conCarga(boton, accion) {
  const original = boton.innerHTML;
  boton.disabled = true;
  boton.innerHTML = `${icono('refrescar', 'girar')} Procesando…`;
  try { return await accion(); } finally { boton.disabled = false; boton.innerHTML = original; }
}

export function cargando() {
  return `<div class="cargando">${icono('refrescar', 'girar')}</div>`;
}

/** Genera un QR como <img> usando qrcode-generator (incluido localmente). */
export function qrImagen(texto, tam = 6) {
  const qr = window.qrcode(0, 'M');
  qr.addData(texto);
  qr.make();
  return qr.createImgTag(tam, 8);
}

export function tablaVacia(columnas, mensaje = 'Sin registros') {
  return `<tr><td class="vacio" colspan="${columnas}">${esc(mensaje)}</td></tr>`;
}

export function debounce(fn, ms = 300) {
  let t;
  return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); };
}
