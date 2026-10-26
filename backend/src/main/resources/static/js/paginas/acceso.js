// Control de acceso por código QR (RF-06, RF-17 · BPM Fig. 6)
// El personal escanea el QR que el cliente muestra en su app (cámara o lector USB) o registra por DNI.
import { api } from '../api.js';
import { icono, esc, fmt, insignia, avatar, inicialesNombre, tablaVacia, errorToast, conCarga } from '../ui.js';

export async function render(el) {
  el.innerHTML = `
    <div class="cabecera">
      <div><h1>Control de acceso</h1><p>RF-17 · escanee el código QR personal del cliente para validar su membresía y registrar la asistencia</p></div>
    </div>
    <section class="rejilla-2b">
      <div class="pila">
        <div class="tarjeta">
          <div class="tarjeta-titulo"><h2>Escanear código QR</h2>
            <div class="acciones"><button class="btn btn-turquesa btn-chico" id="camara">${icono('camara')} Activar cámara</button></div></div>
          <div class="lector" id="lector">
            <div class="placeholder">${icono('qr')}<div>Active la cámara o use un lector de códigos USB:<br>el código se escribe en el campo de abajo.</div></div>
          </div>
          <form id="f-qr" style="display:flex;gap:10px;margin-top:14px">
            <input class="entrada" name="codigoQR" id="codigo" placeholder="Código QR (AMIGEP-...)" autocomplete="off">
            <button class="btn btn-turquesa" type="submit">Validar</button>
          </form>
        </div>
        <div class="tarjeta">
          <div class="tarjeta-titulo"><h2>Registro manual por DNI</h2></div>
          <p class="c-apagado" style="margin:-6px 0 12px;font-size:13px">Use esta opción cuando el QR no se pueda leer o no sea identificado.</p>
          <form id="f-dni" style="display:flex;gap:10px">
            <input class="entrada" name="dni" id="dni" placeholder="DNI del cliente" inputmode="numeric" maxlength="8">
            <button class="btn btn-borde" type="submit">Registrar</button>
          </form>
        </div>
      </div>
      <div class="pila">
        <div id="resultado" class="resultado-acceso">
          <div class="c-apagado">${icono('escudo')}<p style="margin:8px 0 0">El resultado de la validación aparecerá aquí.</p></div>
        </div>
        <div class="tarjeta">
          <div class="tarjeta-titulo"><h2>Ingresos de hoy</h2><div class="acciones"><span class="etiqueta-rp" id="conteo">0</span></div></div>
          <div class="tabla-envoltura" style="max-height:360px;overflow:auto">
            <table class="tabla"><thead><tr><th>Cliente</th><th>Hora</th><th>Registro</th></tr></thead><tbody id="hoy"></tbody></table>
          </div>
        </div>
      </div>
    </section>`;

  const resultado = el.querySelector('#resultado');
  const mostrar = r => {
    resultado.className = 'resultado-acceso ' + (r.permitido ? 'permitido' : 'denegado');
    resultado.innerHTML = `
      ${r.cliente ? avatar(r.iniciales, r.cliente) : `<div style="margin-bottom:10px">${icono('alerta', 'c-rojo')}</div>`}
      <div class="estado ${r.permitido ? 'c-turquesa' : 'c-rojo'}">${r.permitido ? (r.motivo === 'YA_REGISTRADO' ? 'Ingreso ya registrado' : 'Ingreso autorizado') : 'Acceso no autorizado'}</div>
      ${r.cliente ? `<h3>${esc(r.cliente)}</h3><div class="c-apagado" style="font-size:13px">${esc(r.codigoCliente)}</div>` : ''}
      <p style="margin:12px 0 0;color:var(--texto-2)">${esc(r.mensaje)}</p>
      ${r.membresia ? `<div style="margin-top:12px">${insignia(r.membresia.estado, `Plan ${r.membresia.plan} · vence ${fmt.fecha(r.membresia.fechaCaducidad)}`)}</div>` : ''}
      ${r.motivo === 'SIN_MEMBRESIA' ? `<div style="margin-top:14px"><a class="btn btn-dorado btn-chico" href="#/membresias">Renovar membresía</a></div>` : ''}`;
  };
  const cargarHoy = async () => {
    const lista = await api('/admin/accesos');
    el.querySelector('#conteo').textContent = `${lista.length} ingresos`;
    el.querySelector('#hoy').innerHTML = lista.length ? lista.map(a => `
      <tr><td><div class="persona">${avatar(inicialesNombre(a.cliente), a.cliente)}<div><strong>${esc(a.cliente)}</strong><small>${esc(a.codigoCliente)}</small></div></div></td>
      <td class="mono">${fmt.hora(a.fechaHora)}</td><td>${insignia(a.tipo)}</td></tr>`).join('') : tablaVacia(3, 'Sin ingresos todavía');
  };
  const validar = async (ruta, cuerpo, boton) => conCarga(boton, async () => {
    try {
      mostrar(await api(ruta, { metodo: 'POST', cuerpo }));
      cargarHoy();
    } catch (e) { errorToast(e); }
  });

  el.querySelector('#f-qr').addEventListener('submit', e => {
    e.preventDefault();
    const codigo = el.querySelector('#codigo').value.trim();
    if (!codigo) return;
    validar('/admin/accesos/qr', { codigoQR: codigo }, e.submitter || el.querySelector('#f-qr button'));
    el.querySelector('#codigo').value = '';
  });
  el.querySelector('#f-dni').addEventListener('submit', e => {
    e.preventDefault();
    const dni = el.querySelector('#dni').value.trim();
    if (!/^\d{8}$/.test(dni)) return errorToast(new Error('El DNI debe tener 8 dígitos'));
    validar('/admin/accesos/manual', { dni }, e.submitter || el.querySelector('#f-dni button'));
    el.querySelector('#dni').value = '';
  });

  // Lector con cámara (html5-qrcode, incluido localmente)
  let lector = null;
  let ultimo = { codigo: '', hora: 0 };
  const detener = async () => {
    if (lector) { try { await lector.stop(); } catch { /* ya detenido */ } lector = null; }
  };
  el.querySelector('#camara').addEventListener('click', async e => {
    const boton = e.currentTarget;
    if (lector) {
      await detener();
      boton.innerHTML = `${icono('camara')} Activar cámara`;
      el.querySelector('#lector').innerHTML = `<div class="placeholder">${icono('qr')}<div>Cámara detenida.</div></div>`;
      return;
    }
    if (!window.Html5Qrcode) return errorToast(new Error('El lector de QR aún no se ha cargado'));
    el.querySelector('#lector').innerHTML = '<div id="video-qr" style="width:100%"></div>';
    lector = new window.Html5Qrcode('video-qr');
    try {
      await lector.start({ facingMode: 'environment' }, { fps: 10, qrbox: 220 }, texto => {
        const ahora = Date.now();
        if (texto === ultimo.codigo && ahora - ultimo.hora < 4000) return; // evita lecturas repetidas
        ultimo = { codigo: texto, hora: ahora };
        validar('/admin/accesos/qr', { codigoQR: texto }, boton);
      });
      boton.innerHTML = `${icono('cerrar')} Detener cámara`;
    } catch (err) {
      lector = null;
      el.querySelector('#lector').innerHTML = `<div class="placeholder">${icono('alerta')}<div>No se pudo acceder a la cámara.<br>Use el campo de código o un lector USB.</div></div>`;
    }
  });

  el.querySelector('#codigo').focus();
  await cargarHoy();
  return () => { detener(); };
}
