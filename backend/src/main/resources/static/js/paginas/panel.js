// A1 · Panel general (RF-15, RF-16, RF-20 / reglas de RP-02 y RP-03)
import { api, descargar } from '../api.js';
import { icono, esc, fmt, insignia, avatar, cargando, tablaVacia, errorToast, inicialesNombre } from '../ui.js';

export async function render(el) {
  el.innerHTML = `
    <div class="cabecera">
      <div>
        <h1>Panel general</h1>
        <p>Resumen operativo de AMIGEP · hoy, ${fmt.fechaLarga()}</p>
      </div>
      <div class="acciones">
        <button class="btn btn-borde" disabled title="Indicadores del mes en curso">${icono('calendario')} Este mes</button>
        <button class="btn btn-dorado" id="exportar">${icono('descargar')} Exportar</button>
      </div>
    </div>
    <div id="cuerpo">${cargando()}</div>`;

  el.querySelector('#exportar').addEventListener('click', () =>
    descargar('/reportes/RP-08', { formato: 'csv' }, 'RP-08-resumen-dashboard.csv').catch(errorToast));

  const [d, accesos] = await Promise.all([api('/admin/dashboard'), api('/admin/accesos')]);
  const maxMetodo = Math.max(1, ...d.pagosPorMetodo.map(m => Number(m.monto)));
  const colores = { YAPE_PLIN: 'var(--turquesa)', TARJETA: 'var(--dorado)', EFECTIVO: '#6B7079' };
  const totalRutinas = d.rutinasPorEstado.reduce((s, r) => s + r.cantidad, 0) || 1;

  el.querySelector('#cuerpo').innerHTML = `
    <section class="kpis">
      <div class="kpi">
        <div class="nombre">Clientes activos</div>
        <div class="valor">${fmt.numero(d.clientesActivos)}</div>
        <div class="nota c-turquesa">${icono('tendencia')} +${d.nuevosEsteMes} este mes</div>
      </div>
      <div class="kpi">
        <div class="nombre">Membresías por vencer</div>
        <div class="valor c-dorado">${d.membresiasPorVencer}</div>
        <div class="nota c-dorado">próx. 7 días</div>
      </div>
      <div class="kpi">
        <div class="nombre">Pagos del mes</div>
        <div class="valor">${fmt.soles(d.pagosMes)}</div>
        <div class="nota ${d.variacionPagosPct === null || d.variacionPagosPct >= 0 ? 'c-turquesa' : 'c-rojo'}">
          ${icono('tendencia')} ${d.variacionPagosPct === null ? 'primer mes' : fmt.pct(d.variacionPagosPct) + ' vs. mes anterior'}</div>
      </div>
      <div class="kpi">
        <div class="nombre">Asistencias hoy</div>
        <div class="valor">${d.asistenciasHoy}</div>
        <div class="nota">de ${d.clientesConMembresia} clientes</div>
      </div>
    </section>

    <section class="rejilla-2">
      <div class="tarjeta">
        <div class="tarjeta-titulo"><h2>Membresías próximas a vencer</h2><div class="acciones"><span class="etiqueta-rp">RP-02</span></div></div>
        <div class="tabla-envoltura">
          <table class="tabla">
            <thead><tr><th>Cliente</th><th>Plan</th><th>Vence</th><th>Estado</th></tr></thead>
            <tbody>
              ${d.proximasAVencer.length ? d.proximasAVencer.map(v => `
                <tr class="clic" data-cliente="${v.idCliente}">
                  <td>${esc(v.cliente)}</td><td>${esc(v.plan)}</td><td class="mono">${fmt.fecha(v.vence)}</td>
                  <td>${insignia('POR_VENCER', v.diasRestantes === 0 ? 'Hoy' : v.diasRestantes + (v.diasRestantes === 1 ? ' día' : ' días'))}</td>
                </tr>`).join('') : tablaVacia(4, 'No hay membresías por vencer en los próximos 7 días')}
            </tbody>
          </table>
        </div>
      </div>
      <div class="pila">
        <div class="tarjeta">
          <div class="tarjeta-titulo"><h2>Pagos por método</h2><div class="acciones"><span class="etiqueta-rp">RP-03</span></div></div>
          ${d.pagosPorMetodo.map(m => `
            <div class="barra-item">
              <div class="fila"><span>${esc(m.etiqueta)}</span><strong>${fmt.soles(m.monto)}</strong></div>
              <div class="barra"><span style="width:${(Number(m.monto) / maxMetodo * 100).toFixed(1)}%;background:${colores[m.metodo]}"></span></div>
            </div>`).join('')}
        </div>
        <div class="tarjeta">
          <div class="tarjeta-titulo"><h2>Rutinas por estado</h2><div class="acciones"><span class="etiqueta-rp">RP-08</span></div></div>
          ${d.rutinasPorEstado.map(r => `
            <div class="barra-item">
              <div class="fila"><span>${esc(r.etiqueta)}</span><strong>${r.cantidad}</strong></div>
              <div class="barra"><span style="width:${(r.cantidad / totalRutinas * 100).toFixed(1)}%;background:${r.clave === 'VIGENTE' ? 'var(--turquesa)' : r.clave === 'POR_FINALIZAR' ? 'var(--dorado)' : '#6B7079'}"></span></div>
            </div>`).join('')}
        </div>
      </div>
    </section>

    <section class="tarjeta mt">
      <div class="tarjeta-titulo"><h2>Ingresos de hoy</h2>
        <div class="acciones"><a class="btn btn-borde btn-chico" href="#/acceso">${icono('qr')} Registrar ingreso</a></div></div>
      <div class="tabla-envoltura">
        <table class="tabla">
          <thead><tr><th>Cliente</th><th>Código</th><th>Hora</th><th>Registro</th></tr></thead>
          <tbody>
            ${accesos.length ? accesos.slice(0, 8).map(a => `
              <tr class="clic" data-cliente="${a.idCliente}">
                <td><div class="persona">${avatar(inicialesNombre(a.cliente), a.cliente)}<strong>${esc(a.cliente)}</strong></div></td>
                <td class="mono">${esc(a.codigoCliente)}</td><td class="mono">${fmt.hora(a.fechaHora)}</td><td>${insignia(a.tipo)}</td>
              </tr>`).join('') : tablaVacia(4, 'Aún no hay ingresos registrados hoy')}
          </tbody>
        </table>
      </div>
    </section>`;

  el.querySelectorAll('[data-cliente]').forEach(f => f.addEventListener('click', async () => {
    const { abrirDetalleCliente } = await import('./clientes.js');
    abrirDetalleCliente(Number(f.dataset.cliente));
  }));
}
