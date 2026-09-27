// E2E T1 PAYGO dual-both: ejecuta P1 (5 req) + P2 (3 req) en orden de la colección Postman.
// Guarda evidencia cruda en docs/evidencia/raw/*.json
const fs = require('fs');
const path = require('path');
const outDir = path.join(__dirname, 'raw');
fs.mkdirSync(outDir, { recursive: true });

const results = [];
async function req(label, method, url, body, expect) {
  const t0 = Date.now();
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* texto plano */ }
  const r = { label, method, url, body, expect, status: res.status, ms: Date.now() - t0, response: json !== null ? json : text };
  r.pass = res.status === expect;
  results.push(r);
  console.log(`${r.pass ? 'PASS' : 'FAIL'} ${label}: ${method} ${url} -> ${res.status} (esperado ${expect})`);
  fs.writeFileSync(path.join(outDir, `${results.length}-${label.replace(/[^a-z0-9]+/gi, '-').toLowerCase()}.json`), JSON.stringify(r, null, 2));
  return r;
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

(async () => {
  const T = 'http://localhost:8081', R = 'http://localhost:8082', G = 'http://localhost:8083';
  // P1
  await req('1-crear-tarjeta', 'POST', `${T}/tarjetas`, { idTarjeta: 1, nomTitular: 'Juan Perez', saldoAsignado: 1000, saldoDisponible: 1000 }, 201);
  await req('2-listar-tarjetas', 'GET', `${T}/tarjetas`, null, 200);
  await req('3-buscar-tarjeta-1', 'GET', `${T}/tarjetas/1`, null, 200);
  await req('4-recarga-ok-feign', 'POST', `${R}/recargas`, { idRecarga: 10, idTarjeta: 1, montoRecarga: 100 }, 201);
  await req('5-recarga-tarjeta-inexistente', 'POST', `${R}/recargas`, { idRecarga: 99, idTarjeta: 999, montoRecarga: 50 }, 404);
  // P2
  await req('6-recarga-aprobada', 'POST', `${R}/recargas`, { idRecarga: 11, idTarjeta: 1, montoRecarga: 100 }, 201);
  await req('7-recarga-observada', 'POST', `${R}/recargas`, { idRecarga: 12, idTarjeta: 1, montoRecarga: 900 }, 201);
  await sleep(8000); // dar tiempo a ambos consumers (Kafka + Rabbit)
  await req('8-listar-analisis', 'GET', `${G}/analisis`, null, 200);
  const fails = results.filter((r) => !r.pass);
  console.log(`\nTOTAL: ${results.length - fails.length}/${results.length} PASS`);
  if (fails.length) process.exit(1);
})().catch((e) => { console.error('E2E ERROR:', e.message); process.exit(2); });
