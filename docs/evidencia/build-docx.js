// Genera pregunta1_atuncar.docx y pregunta2_atuncar.docx (OOXML mínimo con PNG embebidos).
// Uso: node docs/evidencia/build-docx.js
// Requiere: raw/*.json, *.png, logs-*.txt en docs/evidencia/
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const EV = path.join(__dirname);
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const P = (t, style) => `<w:p>${style ? `<w:pPr><w:pStyle w:val="${style}"/></w:pPr>` : ''}<w:r><w:t xml:space="preserve">${esc(t)}</w:t></w:r></w:p>`;
const H1 = (t) => P(t, 'Heading1');
const H2 = (t) => P(t, 'Heading2');
const CODE = (t) => `<w:p><w:pPr><w:shd w:fill="F2F2F2" w:val="clear"/></w:pPr><w:r><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/><w:sz w:val="18"/><w:t xml:space="preserve">${esc(t)}</w:t></w:r></w:p>`;
const TABLE_BORDERS = `<w:tblBorders><w:top w:val="single" w:sz="4"/><w:left w:val="single" w:sz="4"/><w:bottom w:val="single" w:sz="4"/><w:right w:val="single" w:sz="4"/><w:insideH w:val="single" w:sz="4"/><w:insideV w:val="single" w:sz="4"/></w:tblBorders>`;
const row = (cells, bold) => `<w:tr>${cells.map((c) => `<w:tc><w:p><w:r>${bold ? '<w:b/>' : ''}<w:t xml:space="preserve">${esc(c)}</w:t></w:r></w:p></w:tc>`).join('')}</w:tr>`;
const TABLE = (headers, rows) => `<w:p/><w:tbl><w:tblPr>${TABLE_BORDERS}</w:tblPr>${row(headers, true)}${rows.map((r) => row(r)).join('')}</w:tbl><w:p/>`;

let imgSeq = 0;
const docMedia = []; // {name, src}
function IMG(srcFile, maxWemu = 5859000) {
  const buf = fs.readFileSync(path.join(EV, srcFile));
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  const name = `image${++imgSeq}.png`;
  docMedia.push({ name, src: srcFile });
  let cx = w * 9525, cy = h * 9525;
  if (cx > maxWemu) { cy = Math.round((cy * maxWemu) / cx); cx = maxWemu; }
  const rId = `rIdImg${imgSeq}`;
  return `<w:p><w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0">`
    + `<wp:extent cx="${cx}" cy="${cy}"/><wp:docPr id="${imgSeq}" name="${name}"/>`
    + `<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">`
    + `<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="${imgSeq}" name="${name}"/><pic:cNvPicPr/></pic:nvPicPr>`
    + `<pic:blipFill><a:blip r:embed="${rId}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>`
    + `<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${cx}" cy="${cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></a:spPr>`
    + `</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>
<w:p><w:r><w:i/><w:t xml:space="preserve">${esc(srcFile)}</w:t></w:r></w:p>`;
}

const raw = (f) => JSON.parse(fs.readFileSync(path.join(EV, 'raw', f), 'utf8'));
const short = (o, n = 420) => { const s = JSON.stringify(o); return s.length > n ? s.slice(0, n) + '…' : s; };
function reqBlock(r) {
  let s = H2(`${r.label} — ${r.method} ${r.url.replace('http://localhost:808', ':808')}`);
  s += P(`Esperado ${r.expect} · Obtenido ${r.status} · ${r.ms} ms · ${r.pass === false ? 'FALLA' : 'OK'}`);
  if (r.body) s += CODE('Body: ' + JSON.stringify(r.body));
  s += CODE('Respuesta: ' + short(r.response));
  return s;
}

// ---------- Pregunta 1 ----------
function buildP1() {
  imgSeq = 0; docMedia.length = 0;
  let d = H1('Pregunta 1 — Comunicación sincrónica (13 pts) — PAYGO PERÚ · Grupo 5 · rama feat/mensajeria-dual-both');
  d += P('Proveedor ms-tarjetas :8081 (POST/GET /tarjetas, GET /tarjetas/{id}) + consumidor ms-recargas :8082 que valida la tarjeta vía OpenFeign, copia saldo_disponible y genera fecha_recarga automática. Evidencia ejecutada el 2026-09-27 contra MySQL Docker (DB_PORT=3307).');
  ['1-1-crear-tarjeta.json', '2-2-listar-tarjetas.json', '3-3-buscar-tarjeta-1.json', '4-4-recarga-ok-feign.json', '5-5-recarga-tarjeta-inexistente.json'].forEach((f) => { d += reqBlock(raw(f)); });
  d += H2('Verificaciones de la rúbrica');
  d += P('· La recarga 10 copió saldoDisponible=1000 desde ms-tarjetas y fechaRecarga automática (ver 4-4-recarga-ok-feign.json).');
  d += P('· Tarjeta inexistente (idTarjeta 999) devuelve 404 "Tarjeta no existe" vía catch FeignException.NotFound → ResponseStatusException (ver 5-5).');
  d += P('· GET /tarjetas/1 es el endpoint que ms-recargas consume con OpenFeign (TarjetaClient).');
  d += P('Colección Postman: docs/postman/PAYGO-T1.postman_collection.json (carpeta P1, 5 requests) + environment PAYGO-Local.');
  return d;
}

// ---------- Pregunta 2 ----------
function buildP2() {
  imgSeq = 0; docMedia.length = 0;
  let d = H1('Pregunta 2 — Comunicación asíncrona (7 pts) — PAYGO PERÚ · Grupo 5 · modo dual both');
  d += P('ms-recargas publica cada recarga en atuncar_queue por Kafka Y RabbitMQ (app.messaging.mode=both). ms-riesgo :8083 consume por ambos (@KafkaListener + @RabbitListener), guarda en analisis con regla Aprobada ≤ 70% saldo / Observada > 70% (idempotente por idRecarga) y expone GET /analisis.');
  d += H2('Requests de evidencia');
  ['6-6-recarga-aprobada.json', '7-7-recarga-observada.json', '8-8-listar-analisis.json'].forEach((f) => { d += reqBlock(raw(f)); });
  const fin = raw('8-8-listar-analisis.json').response;
  d += TABLE(['id_recarga', 'monto', 'saldo', 'situación'], fin.map((a) => [String(a.idRecarga), String(a.montoRecarga), String(a.saldoDisponible), a.situacion]));
  d += H2('Kafka-UI http://localhost:8080 — tópico atuncar_queue (4 mensajes, offsets 0-3)');
  d += IMG('kafka-ui-messages.png');
  d += H2('Kafka-UI — overview del tópico');
  d += IMG('kafka-ui-atuncar_queue.png');
  d += H2('RabbitMQ http://localhost:15672 — cola atuncar_queue (durable, 1 consumer, 0 ready = todo consumido)');
  d += IMG('rabbitmq-atuncar_queue.png');
  d += H2('Logs productor dual (ms-recargas :8082 — cada recarga se publica en ambos brokers)');
  fs.readFileSync(path.join(EV, 'logs-producer-dual.txt'), 'utf8').split('\n').filter(Boolean).forEach((l) => { d += CODE(l.slice(0, 300)); });
  d += H2('Logs consumers (ms-riesgo :8083 — RiesgoConsumer Kafka + RiesgoRabbitConsumer Rabbit)');
  fs.readFileSync(path.join(EV, 'logs-consumers-dual.txt'), 'utf8').split('\n').filter(Boolean).forEach((l) => { d += CODE(l.slice(0, 300)); });
  d += H2('Prueba dual extra');
  d += P('Recarga 13 (monto 200): AMBOS consumers registraron "evaluada idRecarga=13 Aprobada" y analisis quedó con 4 filas únicas (ver 9-9-recarga-13-dual.json). Regla borde: monto = 70% exacto → Aprobada (<=).');
  return d;
}

function writeDocx(docBody, outName) {
  const stage = path.join(EV, '.stage-' + outName.replace('.docx', ''));
  fs.rmSync(stage, { recursive: true, force: true });
  fs.mkdirSync(path.join(stage, 'word', '_rels'), { recursive: true });
  fs.mkdirSync(path.join(stage, 'word', 'media'), { recursive: true });
  fs.mkdirSync(path.join(stage, '_rels'), { recursive: true });
  const rels = docMedia.map((m, i) => `<Relationship Id="rIdImg${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/${m.name}"/>`).join('');
  docMedia.forEach((m) => fs.copyFileSync(path.join(EV, m.src), path.join(stage, 'word', 'media', m.name)));
  const pngTypes = docMedia.length ? '<Default Extension="png" ContentType="image/png"/>' : '';
  fs.writeFileSync(path.join(stage, '[Content_Types].xml'),
    `<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/>${pngTypes}<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>`);
  fs.writeFileSync(path.join(stage, '_rels', '.rels'),
    `<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>`);
  fs.writeFileSync(path.join(stage, 'word', '_rels', 'document.xml.rels'),
    `<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">${rels}</Relationships>`);
  fs.writeFileSync(path.join(stage, 'word', 'document.xml'),
    `<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><w:body>${docBody}<w:sectPr><w:pgMar w:top="720" w:bottom="720" w:left="720" w:right="720"/></w:sectPr></w:body></w:document>`);
  const out = path.join(EV, outName);
  execSync(`java "${path.join(EV, 'ZipDocx.java')}" "${stage}" "${out}"`, { stdio: 'inherit' });
  fs.rmSync(stage, { recursive: true, force: true });
  console.log('OK', out, fs.statSync(out).size, 'bytes');
}

writeDocx(buildP1(), 'pregunta1_atuncar.docx');
writeDocx(buildP2(), 'pregunta2_atuncar.docx');
