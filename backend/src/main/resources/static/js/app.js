// Punto de entrada del panel web: inicio de sesión del administrador, menú lateral y enrutador.
import { api, sesion, guardarSesion, cerrarSesion } from './api.js';
import { icono, esc, campo, leerFormulario, mostrarErrores, modal, toast, conCarga } from './ui.js';

const PAGINAS = [
  { ruta: 'panel', titulo: 'Panel general', icono: 'panel', modulo: './paginas/panel.js' },
  { ruta: 'clientes', titulo: 'Clientes', icono: 'usuarios', modulo: './paginas/clientes.js' },
  { ruta: 'membresias', titulo: 'Membresías', icono: 'tarjeta', modulo: './paginas/membresias.js' },
  { ruta: 'pagos', titulo: 'Pagos', icono: 'dolar', modulo: './paginas/pagos.js' },
  { ruta: 'acceso', titulo: 'Control de acceso', icono: 'qr', modulo: './paginas/acceso.js' },
  { ruta: 'entrenadores', titulo: 'Entrenadores', icono: 'medalla', modulo: './paginas/entrenadores.js' },
  { ruta: 'retencion', titulo: 'Retención IA', icono: 'ia', modulo: './paginas/retencion.js' },
  { ruta: 'reportes', titulo: 'Reportes', icono: 'archivo', modulo: './paginas/reportes.js' },
  { ruta: 'configuracion', titulo: 'Configuración', icono: 'config', modulo: './paginas/configuracion.js' }
];

const raiz = document.getElementById('raiz');
let limpiarPagina = null;
let navegacionActual = 0;

function iniciar() {
  const s = sesion();
  if (!s?.token || s.rol !== 'ADMINISTRADOR') {
    pantallaLogin();
    return;
  }
  estructura(s);
  window.addEventListener('hashchange', navegar);
  navegar();
}

// ----------------------------------------------------------------- Login (RF-01, BPM Fig. 2)
function pantallaLogin() {
  raiz.innerHTML = `
    <main class="login">
      <form class="login-caja" id="form-login" novalidate>
        <div class="login-marca">
          <div class="logo">A</div>
          <h1>AMIGEP</h1>
          <p>Panel web del administrador · Angels GYM</p>
        </div>
        <div class="aviso error oculto" data-error-general></div>
        ${campo('Correo electrónico', '<input class="entrada" name="correo" type="email" autocomplete="username" placeholder="admin@angelsgym.pe" required>')}
        ${campo('Contraseña', '<input class="entrada" name="contrasena" type="password" autocomplete="current-password" placeholder="••••••••" required>')}
        <div style="text-align:right;margin:-4px 0 18px"><a href="#" id="olvide">¿Olvidaste tu contraseña?</a></div>
        <button class="btn btn-turquesa btn-bloque" type="submit">${icono('escudo')} Iniciar sesión</button>
        <p class="login-pie">El cliente y el entrenador ingresan desde la aplicación móvil.</p>
      </form>
    </main>`;
  const form = document.getElementById('form-login');
  form.addEventListener('submit', async e => {
    e.preventDefault();
    const datos = leerFormulario(form);
    const errores = {};
    if (!datos.correo) errores.correo = 'Ingrese su correo';
    if (!datos.contrasena) errores.contrasena = 'Ingrese su contraseña';
    if (Object.keys(errores).length) return mostrarErrores(form, { message: 'Complete los campos', campos: errores });
    await conCarga(form.querySelector('button[type=submit]'), async () => {
      try {
        const r = await api('/auth/login', { metodo: 'POST', cuerpo: { ...datos, rol: 'ADMINISTRADOR' } });
        guardarSesion(r);
        location.hash = '#/panel';
        location.reload();
      } catch (err) {
        mostrarErrores(form, err);
      }
    });
  });
  document.getElementById('olvide').addEventListener('click', e => { e.preventDefault(); recuperarContrasena(); });
}

function recuperarContrasena() {
  const m = modal({
    titulo: 'Recuperar contraseña',
    subtitulo: 'Recibirá un código de 6 dígitos en su correo',
    contenido: `
      <form id="f-rec" novalidate>
        <div class="aviso error oculto" data-error-general></div>
        ${campo('Correo electrónico', '<input class="entrada" name="correo" type="email">')}
        <div id="paso2" class="oculto">
          <div class="aviso ok" id="aviso-codigo"></div>
          ${campo('Código', '<input class="entrada" name="codigo" inputmode="numeric" maxlength="6">')}
          ${campo('Nueva contraseña', '<input class="entrada" name="nuevaContrasena" type="password">')}
        </div>
      </form>`,
    pie: '<button class="btn btn-borde" data-cerrar>Cancelar</button><button class="btn btn-turquesa" id="b-rec">Enviar código</button>'
  });
  const form = m.el.querySelector('#f-rec');
  const boton = m.el.querySelector('#b-rec');
  let paso = 1;
  boton.addEventListener('click', () => conCarga(boton, async () => {
    const d = leerFormulario(form);
    try {
      if (paso === 1) {
        const r = await api('/auth/recuperar', { metodo: 'POST', cuerpo: { correo: d.correo, rol: 'ADMINISTRADOR' } });
        form.querySelector('#aviso-codigo').textContent = r.mensaje + (r.codigoDesarrollo ? ` (Código de prueba: ${r.codigoDesarrollo})` : '');
        form.querySelector('#paso2').classList.remove('oculto');
        mostrarErrores(form, null);
        paso = 2;
        boton.textContent = 'Cambiar contraseña';
      } else {
        await api('/auth/restablecer', { metodo: 'POST', cuerpo: { correo: d.correo, rol: 'ADMINISTRADOR', codigo: d.codigo, nuevaContrasena: d.nuevaContrasena } });
        m.cerrar();
        toast('Contraseña actualizada. Ya puede iniciar sesión.');
      }
    } catch (err) {
      mostrarErrores(form, err);
    }
  }));
}

// ----------------------------------------------------------------- Estructura con menú lateral persistente
function estructura(s) {
  raiz.innerHTML = `
    <div class="app">
      <aside class="lateral">
        <div class="marca"><div class="logo">A</div><strong>AMIGEP</strong><small>ADMIN</small></div>
        <nav class="menu" id="menu">
          ${PAGINAS.map(p => `<a href="#/${p.ruta}" data-ruta="${p.ruta}">${icono(p.icono)}<span>${p.titulo}</span></a>`).join('')}
        </nav>
        <div class="usuario-lateral">
          <div class="logo" style="width:30px;height:30px;font-size:12px">${esc(s.iniciales)}</div>
          <div style="min-width:0"><strong style="font-size:13px">${esc(s.nombre)} ${esc(s.apellido.split(' ')[0])}</strong><small>${esc(s.correo)}</small></div>
        </div>
        <button class="salir" id="salir">${icono('salir')} Cerrar sesión</button>
      </aside>
      <main class="contenido" id="contenido"></main>
    </div>`;
  document.getElementById('salir').addEventListener('click', cerrarSesion);
}

async function navegar() {
  const ruta = (location.hash.replace(/^#\/?/, '').split('?')[0]) || 'panel';
  const pagina = PAGINAS.find(p => p.ruta === ruta) || PAGINAS[0];
  document.querySelectorAll('#menu a').forEach(a => a.classList.toggle('activo', a.dataset.ruta === pagina.ruta));
  document.title = `${pagina.titulo} · AMIGEP`;
  if (typeof limpiarPagina === 'function') limpiarPagina();
  limpiarPagina = null;
  // Contenedor nuevo por navegación: si una página anterior termina de cargar tarde,
  // escribe en un nodo ya desconectado y no pisa la página actual.
  const contenedor = document.createElement('div');
  document.getElementById('contenido').replaceChildren(contenedor);
  const token = ++navegacionActual;
  try {
    const modulo = await import(pagina.modulo);
    const limpiar = await modulo.render(contenedor);
    if (token === navegacionActual) limpiarPagina = limpiar;
    else if (typeof limpiar === 'function') limpiar();
  } catch (e) {
    if (token !== navegacionActual) return;
    console.error(e);
    contenedor.innerHTML = `<div class="aviso error">No se pudo cargar la página: ${esc(e.message)}</div>`;
  }
}

iniciar();
