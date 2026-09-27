# T1-Grupo-5-DSW-II
Repositorio correspondiente a la evaluación T1 del Grupo 5 para el curso de Desarrollo de Servicios Web II (fintech PAYGO PERÚ: tarjetas prepago, recargas y riesgo).

## 🧩 Servicios

| Servicio | Puerto | Responsabilidad | BD MySQL |
|---|---|---|---|
| `ms-tarjetas` | 8081 | CRUD tarjetas, `GET /tarjetas` y `GET /tarjetas/{id}` (proveedor Feign) | `paygo_tarjetas` |
| `ms-recargas` | 8082 | Registra recargas, valida tarjeta vía OpenFeign, publica en `atuncar_queue` | `paygo_recargas` |
| `ms-riesgo` | 8083 | Consume `atuncar_queue`, guarda `analisis` (Aprobada/Observada), `GET /analisis` | `paygo_riesgo` |

Infra: MySQL `3307` por defecto (`DB_PORT`, `paygo_tarjetas/recargas/riesgo`, root/1234) + Kafka `9092` (KRaft, sin Zookeeper) + UI `http://localhost:8080` + RabbitMQ `5672` + consola `http://localhost:15672` (guest/guest). Cola/tópico del examen: `atuncar_queue` (cambiar al apellido real antes de entregar). Modo dual `both` por defecto (`app.messaging.mode=kafka|rabbit|both`).

## 🚀 Cómo levantar (en orden)

```bash
# 1. MySQL (desde su carpeta: el compose monta ../infrastructure/mysql-init)
# Puerto host parametrizado como en la guía T1-DAW II: default 3307 para no
# chocar con un MySQL local en 3306. Si tu 3306 está libre usa DB_PORT=3306.
cd database && docker compose up -d && cd ..
# Verificar: docker ps → paygo-mysql healthy; BDs paygo_* creadas por init.sql

# 2. Kafka + RabbitMQ
cd infrastructure && docker compose -f docker-compose-kafka.yml up -d
docker compose -f docker-compose-rabbitmq.yml up -d && cd ..

# 3. Microservicios (una terminal por servicio, en este orden)
# Con MySQL en 3307 (default del compose) exporta DB_URL antes de cada uno:
#   export DB_URL=jdbc:mysql://localhost:3307/paygo_tarjetas  # (o recargas/riesgo)
# Con MySQL en 3306 (DB_PORT=3306 o MySQL local con root/1234) no necesitas nada.
cd ms-tarjetas && ./mvnw spring-boot:run   # :8081 primero
cd ms-recargas && ./mvnw spring-boot:run   # :8082
cd ms-riesgo && ./mvnw spring-boot:run     # :8083
```

También puedes levantar MySQL + Kafka + RabbitMQ con los comandos de arriba y correr los 3 microservicios con Docker (cada `ms-*/Dockerfile` expone su puerto). No hay `docker-compose.yml` en la raíz: no usar `docker compose up` desde la raíz.

Verificación rápida:
- Tarjetas: `GET http://localhost:8081/tarjetas`
- Recargas: `GET http://localhost:8082/recargas`
- Riesgo: `GET http://localhost:8083/analisis`
- Kafka: `http://localhost:8080` → tópico `atuncar_queue` → pestaña Messages
- RabbitMQ: `http://localhost:15672` (guest/guest) → cola `atuncar_queue` → Get Message(s)

## 🧪 Postman (evidencia P1 + P2)

Importar en Postman la colección + environment y seleccionar el environment arriba a la derecha:

- `docs/postman/PAYGO-T1.postman_collection.json` — carpetas `P1 - Sincronica` (5 requests) y `P2 - Asincrona` (3 requests), en orden de capturas.
- `docs/postman/PAYGO-Local.postman_environment.json` — `tarjetas/recargas/riesgo_base_url` a `localhost:8081/8082/8083`.

Cada request indica el resultado esperado y a qué captura corresponde. Evidencia ejecutada el 2026-09-27 (8/8 PASS) en `docs/evidencia/`: `pregunta1_atuncar.docx` (P1) y `pregunta2_atuncar.docx` (P2, con capturas Kafka-UI + RabbitMQ + logs), respuestas crudas en `raw/*.json`, reproducibles con `node docs/evidencia/run-e2e.js` y `node docs/evidencia/build-docx.js`. Renombrar `atuncar` al apellido del entregante antes de subir a BlackBoard.

## 📚 Documentación

- [Guía Git para trabajar en equipo](docs/guia-git.md) — Reglas paso a paso para trabajar con ramas, PRs y mantener `main` siempre estable. Léela antes de empezar.
- [Plan de implementación dual Kafka + Rabbit](docs/plan-implementacion-dual-both.md) — Fases 0–5, modo `both` por defecto.
- [Plan original por fases (archivado)](docs/realizado/plan-implementacion-fases.md) — Decisión inicial solo-RabbitMQ, superada por el modo dual.
