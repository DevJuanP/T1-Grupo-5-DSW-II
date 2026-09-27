# T1-Grupo-5-DSW-II
Repositorio correspondiente a la evaluación T1 del Grupo 5 para el curso de Desarrollo de Servicios Web II (fintech PAYGO PERÚ: tarjetas prepago, recargas y riesgo).

## 🧩 Servicios

| Servicio | Puerto | Responsabilidad | BD MySQL |
|---|---|---|---|
| `ms-tarjetas` | 8081 | CRUD tarjetas, `GET /tarjetas` y `GET /tarjetas/{id}` (proveedor Feign) | `paygo_tarjetas` |
| `ms-recargas` | 8082 | Registra recargas, valida tarjeta vía OpenFeign, publica en `atuncar_queue` | `paygo_recargas` |
| `ms-riesgo` | 8083 | Consume `atuncar_queue`, guarda `analisis` (Aprobada/Observada), `GET /analisis` | `paygo_riesgo` |

Infra: Kafka `9092` (KRaft, sin Zookeeper) + UI `http://localhost:8080`. Tópico del examen: `atuncar_queue` (cambiar al apellido real antes de entregar).

## 🚀 Cómo levantar (en orden)

```bash
# 1. Kafka
cd infrastructure && docker compose -f docker-compose-kafka.yml up -d

# 2. Microservicios (una terminal por servicio, en este orden)
cd ms-tarjetas && ./mvnw spring-boot:run   # :8081 primero
cd ms-recargas && ./mvnw spring-boot:run   # :8082
cd ms-riesgo && ./mvnw spring-boot:run     # :8083
```

También puedes levantar todo (MySQL + Kafka + los 3 microservicios) con un solo comando desde la raíz del repo: `docker compose up -d --build` (usa `database/docker-compose.yml`, no el de `infrastructure/`).

Verificación rápida:
- Tarjetas: `GET http://localhost:8081/tarjetas`
- Recargas: `GET http://localhost:8082/recargas`
- Riesgo: `GET http://localhost:8083/analisis`
- Kafka: `http://localhost:8080` → tópico `atuncar_queue` → pestaña Messages

## 🧪 Postman (evidencia P1 + P2)

Importar en Postman la colección + environment y seleccionar el environment arriba a la derecha:

- `docs/postman/PAYGO-T1.postman_collection.json` — carpetas `P1 - Sincronica` (5 requests) y `P2 - Asincrona` (3 requests), en orden de capturas.
- `docs/postman/PAYGO-Local.postman_environment.json` — `tarjetas/recargas/riesgo_base_url` a `localhost:8081/8082/8083`.

Cada request indica el resultado esperado y a qué captura corresponde (`pregunta1_apellido.docx` / `pregunta2_apellido.docx`, que se arman manual con las capturas: Postman + Kafka UI (`http://localhost:8080`) + logs del consumer).

## 📚 Documentación

- [Guía Git para trabajar en equipo](docs/guia-git.md) — Reglas paso a paso para trabajar con ramas, PRs y mantener `main` siempre estable. Léela antes de empezar.
- [Plan de implementación por fases](docs/plan-implementacion-fases.md) — Fases 0–6 con su correspondencia al repo guía `T1-DAW II`.
