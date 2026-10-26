// A3 · Retención de clientes con el módulo inteligente (RF-18, RF-19 · BPM Fig. 12)
import { api } from '../api.js';
import { icono, esc, fmt, insignia, cargando, tablaVacia, modal, campo, opciones, leerFormulario, mostrarErrores, conCarga, toast, errorToast } from '../ui.js';
import { abrirDetalleCliente } from './clientes.js';

const TIPOS = ['Recordatorio de renovación', 'Llamada de seguimiento', 'Oferta de nueva rutina IA', 'Descuento en renovación', 'Mensaje motivacional'];
const ICONO_TIPO = t => (/llamada/i.test(t) ? 'telefono' : /rutina/i.test(t) ? 'pesa' : /descuento|oferta/i.test(t) ? 'etiqueta' : /mensaje/i.test(t) ? 'mensaje' : 'campana');

export async function render(el) {
  let filtro = '';
  el.innerHTML = `
    <div class="cabecera">
      <div><h1>Retención de clientes</h1><p>Módulo inteligente · predicción de riesgo de abandono y acciones de retención</p></div>
      <div class="acciones"><button class="btn btn-turquesa" id="actualizar">${icono('ia')} Actualizar predicciones</button></div>
    </div>
    <section class="kpis" id="kpis">${cargando()}</section>
    <section class="rejilla-2">
      <div class="tarjeta">
        <div class="tarjeta-titulo"><h2>Clientes con mayor riesgo de abandono</h2>
          <div class="acciones"><span class="etiqueta-rp">Predicción</span></div></div>
        <div class="chips" id="chips" style="margin-bottom:6px">
          <button class="chip activo" data-f="">En riesgo</button><button class="chip" data-f="ALTO">Alto</button>
          <button class="chip" data-f="MEDIO">Medio</button><button class="chip" data-f="TODOS">Todos</button>
        </div>
        <div class="tabla-envoltura">
          <table class="tabla">
            <thead><tr><th>Cliente</th><th>Probabilidad</th><th>Factores de riesgo</th><th>Estado</th><th></th></tr></thead>
            <tbody id="predicciones"><tr><td colspan="5">${cargando()}</td></tr></tbody>
          </table>
        </div>
      </div>
      <div class="tarjeta columna-flex">
        <div class="tarjeta-titulo"><h2>Acciones de retención</h2><div class="acciones"><span class="etiqueta-rp">Notificaciones</span></div></div>
        <div id="acciones" class="relleno" style="max-height:520px;overflow:auto"></div>
        <button class="btn btn-dorado btn-bloque" id="registrar" style="margin-top:12px">Registrar acción de retención</button>
      </div>
    </section>
    <section class="tarjeta mt" id="modelo"></section>`;

  let predicciones = [];
  const pintarPredicciones = () => {
    const lista = predicciones.filter(p => filtro === 'TODOS' || (filtro ? p.estado === filtro : p.estado !== 'BAJO'));
    el.querySelector('#predicciones').innerHTML = lista.length ? lista.map(p => `
      <tr class="clic" data-pred="${p.idPrediccion}">
        <td style="white-space:nowrap"><strong style="font-weight:600">${esc(p.cliente)}</strong></td>
        <td class="prob ${p.estado === 'ALTO' ? 'c-rojo' : p.estado === 'MEDIO' ? 'c-dorado' : 'c-turquesa'}">${Number(p.probabilidad).toFixed(2)}</td>
        <td class="factores">${esc((p.factoresRiesgo || []).join(' · ') || 'Sin factores relevantes')}</td>
        <td>${insignia(p.estado)}</td>
        <td class="num"><button class="btn-icono" title="Revisar y registrar acción">${icono('derecha')}</button></td>
      </tr>`).join('') : tablaVacia(5, 'No hay clientes en esta categoría');
    el.querySelectorAll('[data-pred]').forEach(tr => tr.addEventListener('click', () =>
      revisarCliente(predicciones.find(p => p.idPrediccion === Number(tr.dataset.pred)))));
  };

  const cargar = async () => {
    const [r, preds, acciones] = await Promise.all([
      api('/admin/retencion/resumen'), api('/admin/retencion/predicciones'), api('/admin/retencion/acciones')
    ]);
    predicciones = preds;
    el.querySelector('#kpis').innerHTML = `
      <div class="kpi"><div class="nombre">Riesgo alto</div><div class="valor c-rojo">${r.riesgoAlto}</div><div class="nota c-rojo">clientes</div></div>
      <div class="kpi"><div class="nombre">Riesgo medio</div><div class="valor c-dorado">${r.riesgoMedio}</div><div class="nota c-dorado">clientes</div></div>
      <div class="kpi"><div class="nombre">Acciones ejecutadas</div><div class="valor">${r.accionesMes}</div><div class="nota">este mes</div></div>
      <div class="kpi"><div class="nombre">Clientes retenidos</div><div class="valor c-turquesa">${r.clientesRetenidosPct}%</div>
        <div class="nota c-turquesa">${icono('tendencia')} ${r.retenidos} de ${r.contactados} contactados</div></div>`;
    pintarPredicciones();
    el.querySelector('#acciones').innerHTML = acciones.length ? acciones.map(a => `
      <div class="accion-item">
        <div class="icono-caja">${icono(ICONO_TIPO(a.tipoAccion))}</div>
        <div style="min-width:0"><strong>${esc(a.tipoAccion)}</strong><small>${esc(a.cliente)} · ${fmt.fecha(a.fechaAccion)}</small></div>
        <span data-accion="${a.idAccion}" title="Cambiar resultado">${insignia(a.resultado || 'PENDIENTE')}</span>
      </div>`).join('') : '<p class="c-apagado">Aún no se registraron acciones.</p>';
    el.querySelectorAll('[data-accion]').forEach(b => b.addEventListener('click', () => cambiarResultado(Number(b.dataset.accion))));
    const mo = r.modelo;
    el.querySelector('#modelo').innerHTML = `
      <div class="tarjeta-titulo"><h2>${icono('ia', 'c-turquesa')} Modelo de predicción de abandono</h2>
        <div class="acciones"><button class="btn btn-borde btn-chico" id="entrenar">${icono('refrescar')} Reentrenar con el historial</button></div></div>
      <div class="resumen-reporte" style="margin:0">
        <div class="dato"><small>Tipo</small><strong>${esc(mo.tipo)}</strong></div>
        <div class="dato"><small>Estado</small><strong>${mo.entrenado ? 'Entrenado' : 'Pesos iniciales'}</strong></div>
        <div class="dato"><small>Ejemplos</small><strong>${mo.muestras}</strong></div>
        <div class="dato"><small>Exactitud</small><strong>${mo.exactitud !== null ? (mo.exactitud * 100).toFixed(1) + '%' : '—'}</strong></div>
        <div class="dato"><small>Sensibilidad</small><strong>${mo.sensibilidad !== null ? (mo.sensibilidad * 100).toFixed(1) + '%' : '—'}</strong></div>
        <div class="dato"><small>Última predicción</small><strong>${fmt.fecha(r.ultimaActualizacion)}</strong></div>
      </div>
      <p class="c-apagado" style="font-size:12.5px;margin:12px 0 0">Variables: días sin asistir, caída de asistencia, membresía vencida o por vencer, rutina activa, pagos rechazados y frecuencia mensual.
        Riesgo alto ≥ 0.70 · medio ≥ 0.40. Se ejecuta automáticamente cada día a las 6:00 a.m.</p>`;
    el.querySelector('#entrenar').addEventListener('click', e => conCarga(e.currentTarget, async () => {
      try {
        const res = await api('/admin/retencion/modelo/entrenar', { metodo: 'POST' });
        toast(res.mensaje, res.entrenado ? 'ok' : 'error');
        if (res.entrenado) await api('/admin/retencion/predicciones/actualizar', { metodo: 'POST' });
        refrescar();
      } catch (err) { errorToast(err); }
    }));
  };
  const refrescar = () => cargar().catch(errorToast);

  const revisarCliente = p => {
    const m = modal({
      titulo: p.cliente,
      subtitulo: `Probabilidad de abandono ${Number(p.probabilidad).toFixed(2)} · predicción del ${fmt.fecha(p.fechaPrediccion)}`,
      contenido: `
        <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:14px">${insignia(p.estado, 'Riesgo ' + fmt.capital(p.estado))} ${insignia(p.estadoMembresia)}</div>
        <div class="campo"><label>Factores de riesgo</label>
          <ul style="margin:0;padding-left:18px;color:var(--texto-2)">${(p.factoresRiesgo || []).map(f => `<li>${esc(f)}</li>`).join('') || '<li>Sin factores relevantes</li>'}</ul></div>`,
      pie: `<button class="btn btn-borde" id="ver">Ver ficha del cliente</button><button class="btn btn-dorado" id="accion">Registrar acción</button>`
    });
    m.el.querySelector('#ver').addEventListener('click', () => { m.cerrar(); abrirDetalleCliente(p.idCliente); });
    m.el.querySelector('#accion').addEventListener('click', () => { m.cerrar(); registrarAccion(p.idPrediccion); });
  };

  const registrarAccion = (idPrediccion = '') => {
    const enRiesgo = predicciones.filter(p => p.estado !== 'BAJO');
    const lista = (enRiesgo.length ? enRiesgo : predicciones).map(p => [p.idPrediccion, `${p.cliente} · ${Number(p.probabilidad).toFixed(2)} (${fmt.capital(p.estado)})`]);
    const m = modal({
      titulo: 'Registrar acción de retención',
      subtitulo: 'La notificación llega al cliente en su app móvil (RF-19)',
      contenido: `
        <form id="f-accion" novalidate>
          <div class="aviso error oculto" data-error-general></div>
          ${campo('Cliente en riesgo *', `<select class="entrada" name="idPrediccion">${opciones(lista, idPrediccion, { vacio: 'Seleccione' })}</select>`)}
          ${campo('Tipo de acción *', `<select class="entrada" name="tipoAccion">${opciones(TIPOS.map(t => [t, t]), TIPOS[0])}</select>`)}
          ${campo('Descripción', '<textarea class="entrada" name="descripcion" placeholder="Detalle de la acción o acuerdos con el cliente"></textarea>')}
          <label class="casilla"><input type="checkbox" name="enviarNotificacion" checked> Enviar notificación al cliente</label>
          <div id="notif">
            ${campo('Título de la notificación', '<input class="entrada" name="tituloNotificacion" maxlength="100" placeholder="Por defecto, el tipo de acción">')}
            ${campo('Mensaje', '<textarea class="entrada" name="mensajeNotificacion" placeholder="Si lo deja vacío se usa un mensaje sugerido según el tipo de acción"></textarea>')}
          </div>
        </form>`,
      pie: `<button class="btn btn-borde" data-cerrar>Cancelar</button><button class="btn btn-dorado" id="guardar">Registrar acción</button>`
    });
    const form = m.el.querySelector('#f-accion');
    form.querySelector('[name=enviarNotificacion]').addEventListener('change', e => form.querySelector('#notif').classList.toggle('oculto', !e.target.checked));
    const boton = m.el.querySelector('#guardar');
    boton.addEventListener('click', () => conCarga(boton, async () => {
      const d = leerFormulario(form);
      if (!d.idPrediccion) return mostrarErrores(form, { message: 'Seleccione el cliente', campos: { idPrediccion: 'Seleccione el cliente' } });
      d.idPrediccion = Number(d.idPrediccion);
      try {
        await api('/admin/retencion/acciones', { metodo: 'POST', cuerpo: d });
        m.cerrar();
        toast(d.enviarNotificacion ? 'Acción registrada y notificación enviada' : 'Acción registrada');
        refrescar();
      } catch (err) { mostrarErrores(form, err); }
    }));
  };

  const cambiarResultado = idAccion => {
    const m = modal({
      titulo: 'Resultado de la acción',
      contenido: `<div class="chips">${['PENDIENTE', 'ENVIADA', 'RETENIDO', 'NO_RETENIDO'].map(r => `<button class="chip" data-r="${r}">${fmt.capital(r)}</button>`).join('')}</div>`
    });
    m.el.querySelectorAll('[data-r]').forEach(b => b.addEventListener('click', async () => {
      try {
        await api(`/admin/retencion/acciones/${idAccion}`, { metodo: 'PATCH', cuerpo: { resultado: b.dataset.r } });
        m.cerrar();
        toast('Resultado actualizado');
        refrescar();
      } catch (e) { errorToast(e); }
    }));
  };

  el.querySelector('#chips').addEventListener('click', e => {
    const b = e.target.closest('[data-f]');
    if (!b) return;
    filtro = b.dataset.f;
    el.querySelectorAll('#chips .chip').forEach(c => c.classList.toggle('activo', c === b));
    pintarPredicciones();
  });
  el.querySelector('#registrar').addEventListener('click', () => registrarAccion());
  el.querySelector('#actualizar').addEventListener('click', e => conCarga(e.currentTarget, async () => {
    try {
      const r = await api('/admin/retencion/predicciones/actualizar', { metodo: 'POST' });
      toast(`${r.clientesEvaluados} clientes evaluados · ${r.porEstado.ALTO} en riesgo alto, ${r.porEstado.MEDIO} medio`);
      await cargar();
    } catch (err) { errorToast(err); }
  }));
  await cargar();
}
