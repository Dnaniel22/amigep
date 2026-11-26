// Capturas del panel web desplegado (usado por .github/workflows/ci.yml).
// Uso: node capturas-panel.cjs http://localhost:8080 carpeta-de-salida
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

(async () => {
  const [base, salida] = process.argv.slice(2);
  fs.mkdirSync(salida, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1360, height: 840 }, locale: 'es-PE', timezoneId: 'America/Lima' });
  const foto = async (nombre) => {
    await page.waitForTimeout(1500);
    await page.screenshot({ path: path.join(salida, nombre + '.png') });
    console.log('Captura', nombre);
  };

  await page.goto(base + '/#/login', { waitUntil: 'networkidle' });
  await foto('01-login');
  await page.fill('input[name="correo"]', 'admin@angelsgym.pe');
  await page.fill('input[name="contrasena"]', 'Admin123*');
  await page.click('button[type="submit"]');
  await page.waitForURL(/#\/panel/, { timeout: 20000 });
  await page.waitForLoadState('networkidle');
  await foto('02-panel-general');

  for (const [ruta, nombre] of [['clientes', '03-clientes'], ['acceso', '04-control-de-acceso'],
                                ['retencion', '05-retencion-ia'], ['reportes', '06-reportes']]) {
    await page.goto(base + '/#/' + ruta);
    await page.waitForLoadState('networkidle');
    await foto(nombre);
  }

  await page.goto(base + '/actuator/health', { waitUntil: 'networkidle' });
  await foto('07-actuator-health');
  await page.goto(base + '/swagger-ui.html', { waitUntil: 'networkidle' });
  await foto('08-swagger-desactivado');
  await browser.close();
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
