// Cliente de la API REST de AMIGEP (la misma que consume la app móvil).
const CLAVE_SESION = 'amigep.sesion';

export function sesion() {
  try { return JSON.parse(localStorage.getItem(CLAVE_SESION)); } catch { return null; }
}

export function guardarSesion(datos) {
  try { localStorage.setItem(CLAVE_SESION, JSON.stringify(datos)); } catch { /* modo privado */ }
}

export function cerrarSesion() {
  try { localStorage.removeItem(CLAVE_SESION); } catch { /* sin almacenamiento */ }
  location.hash = '#/login';
  location.reload();
}

/** Error con el mensaje y los campos que devuelve el backend ({mensaje, campos}). */
export class ErrorApi extends Error {
  constructor(estado, cuerpo) {
    super(cuerpo?.mensaje || `Error ${estado}`);
    this.estado = estado;
    this.campos = cuerpo?.campos || {};
  }
}

export async function api(ruta, { metodo = 'GET', cuerpo, params } = {}) {
  const url = new URL('/api' + ruta, location.origin);
  if (params) {
    Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v); });
  }
  const s = sesion();
  const resp = await fetch(url, {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      ...(s?.token ? { Authorization: 'Bearer ' + s.token } : {})
    },
    body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined
  });
  if (resp.status === 401 && !ruta.startsWith('/auth/login')) {
    cerrarSesion();
    throw new ErrorApi(401, { mensaje: 'Su sesión expiró' });
  }
  const texto = await resp.text();
  let datos = null;
  try { datos = texto ? JSON.parse(texto) : null; } catch { datos = texto; }
  if (!resp.ok) throw new ErrorApi(resp.status, datos);
  return datos;
}

/** Descarga un archivo protegido (p. ej. reporte CSV) con el token de la sesión. */
export async function descargar(ruta, params, nombre) {
  const url = new URL('/api' + ruta, location.origin);
  Object.entries(params || {}).forEach(([k, v]) => { if (v) url.searchParams.set(k, v); });
  const resp = await fetch(url, { headers: { Authorization: 'Bearer ' + sesion()?.token } });
  if (!resp.ok) throw new ErrorApi(resp.status, await resp.json().catch(() => null));
  const blob = await resp.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = nombre;
  a.click();
  URL.revokeObjectURL(a.href);
}
