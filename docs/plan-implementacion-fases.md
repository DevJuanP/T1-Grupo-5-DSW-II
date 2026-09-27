# Plan de implementación por fases atómicas — T1 Grupo 5 (PAYGO PERÚ)

> Rama: `docs/plan-implementacion-fases` (creada desde `main` según `docs/guia-git.md`).
> Todo el plan toma como única base técnica el repo del profesor **`T1-DAW II`** (Familia A).
> No inventar patrones nuevos: copiar la forma de hacer las cosas de la guía y renombrar al dominio PAYGO.

---

## 1. Qué pide el examen (resumen operativo)

| Pregunta | Puntaje | Qué se evalúa |
|---|---|---|
| P1 — Comunicación sincrónica | 13 pts (Excelente = proveedor + consumidor + capturas) | `ms-tarjetas` registra y expone `GET /tarjetas` y `GET /tarjetas/{id}`. `ms-recargas` registra `id_recarga, id_tarjeta, saldo_disponible, monto_recarga, fecha_recarga`, valida tarjeta vía **OpenFeign**, copia `saldo_disponible` desde Tarjetas, genera `fecha_recarga` automática. Evidencia en `pregunta1_apellido.docx`. |
| P2 — Comunicación asíncrona | 7 pts (Excelente = envío + consumo/registro + capturas) | `ms-recargas` publica cada recarga en cola **`Apellido_Queue`**. Nuevo servicio **riesgo** consume, guarda en tabla **`analisis`** (`id_recarga, id_tarjeta, saldo_disponible, monto_recarga, fecha_recarga, situacion`) con regla `Aprobada ≤ 70% saldo / Observada > 70%`, y expone controlador para listar. Evidencia en `pregunta2_apellido.docx` + consola del broker. |

Decisiones ya tomadas: broker = **RabbitMQ**, cola = **`atuncar_queue`** (cambiar a tu apellido real antes de entregar), `ms-riesgo` en **`:8083`**.

---

## 2. Estado actual de `T1-Grupo-5-DSW-II` (qué dejó tu compañero)

| Pieza | Estado |
|---|---|
| `ms-tarjetas` `:8081` (entity, repo, service, `POST /tarjetas`, `GET /tarjetas`, `GET /tarjetas/{id}`) | ✅ Hecho |
| `ms-recargas` `:8082` (entity, repo, `POST /recargas`, `GET /recargas`, `TarjetaClient` Feign, copia `saldoDisponible`, `fecha=now()`) | ✅ Hecho (~85%) |
| Manejo 404 cuando la tarjeta no existe | ❌ Falta — hoy Feign revienta en 500 |
| `ms-recargas/application.properties` | ⚠️ Roto — contiene caracteres `�` |
| Mensajería (AMQP, cola, producer, consumer) | ❌ Nada — `grep queue|rabbit|kafka|riesgo|analisis` da 0 resultados |
| `ms-riesgo` | ❌ No existe |
| `infrastructure/docker-compose-rabbitmq.yml` | ❌ No existe |
| Postman + `pregunta1/2_apellido.docx` | ❌ No existen |

---

## 3. Mapa de correspondencia PAYGO ↔ T1-DAW II (usar siempre)

| PAYGO (lo que entregamos) | T1-DAW II (fuente guía, copiar patrón) |
|---|---|
| `Tarjeta` / `/tarjetas` | `backend/products-services/.../entidades/Product.java` + `rest/ProductController.java` + `negocio/ProductService.java` (CRUD + `ResponseStatusException 404`) |
| `Recarga` / `/recargas` + Feign | `backend/sales-services/.../entidades/Sale.java` + `rest/SaleController.java` + `negocio/SaleService.java` + `client/ProductClient.java` (`@FeignClient(name, url)`) |
| Publicar recarga en `atuncar_queue` | `backend/sales-services/.../rabbitmq/RabbitMQConfig.java` + `StockReserveEvent.java` (record) + `StockReserveProducer.java` (`RabbitTemplate.convertAndSend` + log) + `SaleService.createSaleWithRabbitReserve()` (patrón `save → publish → return`) |
| Consumir y guardar `analisis` | `backend/products-services/.../rabbitmq/StockReserveConsumer.java` (`@RabbitListener(queues=...)` + log) + `negocio/ProductService.decreaseStock()` (lógica + `404/CONFLICT`) |
| `GET /analisis` listar | `rest/ProductController.getAllProducts()` / `rest/SaleController.getAllSales()` |
| `spring.rabbitmq.*` props | `backend/sales-services/src/main/resources/application.yml` y `backend/products-services/src/main/resources/application.yml` (`host: localhost, port: 5672, guest/guest`) |
| Dependencia AMQP | `backend/sales-services/build.gradle` → `implementation 'org.springframework.boot:spring-boot-starter-amqp'` (en PAYGO va en `pom.xml` porque usamos Maven) |
| Infra RabbitMQ | `queue/docker-compose-rabbitmq.yml` (= `infrastructure/docker-compose-rabbitmq.yml`): imagen `rabbitmq:3-management`, `5672:5672`, `15672:15672`, consola `guest/guest` |
| Cómo probar y qué capturar | `backend/flujos-kafka-rabbit.md` §3 (truco detener consumidor para ver mensaje en `Ready` + `Get Message(s)`, luego levantar y ver consumo) + `backend/README.md` (orden de levantada) |
| BD MySQL vía Docker | `database/docker-compose.yml` + `database/.env` (`DB_IMAGE=mysql:8.0`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWD`) |

> Diferencias a respetar: PAYGO usa **Maven + Spring Boot 4.1.1 + `application.properties`**; la guía usa **Gradle + Boot 3.2.5 + `application.yml`**. El patrón Java es el mismo, solo cambia el archivo de build/config.

Puertos y BDs objetivo:

| Servicio | Puerto | BD |
|---|---|---|
| `ms-tarjetas` | 8081 (ya) | `paygo_tarjetas` (ya) |
| `ms-recargas` | 8082 (ya) | `paygo_recargas` (ya) |
| `ms-riesgo` | 8083 (nuevo, mismo que `jwt-sales-services` en la guía) | `paygo_riesgo` (nueva) |
| RabbitMQ | 5672 + consola 15672 | — |

---

## 4. Fases atómicas (cada fase = 1 commit testeable)

### FASE 0 — Infra RabbitMQ (copia directa de la guía)
- **Objetivo:** tener broker local idéntico al de clase.
- **Base guía:** `T1-DAW II/queue/docker-compose-rabbitmq.yml`.
- **Cambios:**
  - Crear `infrastructure/docker-compose-rabbitmq.yml` (copia literal: `rabbitmq:3-management`, `ligo-rabbitmq`, `5672`, `15672`).
- **Verificación:**
  ```bash
  cd infrastructure && docker compose -f docker-compose-rabbitmq.yml up -d
  docker ps  # ligo-rabbitmq Up
  # abrir http://localhost:15672 (guest/guest) → pestaña Queues
  ```
- **DONE:** consola responde y `docker ps` muestra el contenedor. Captura para `pregunta2`.

### FASE 1 — Pulir P1: 404 Feign + limpiar properties (cierra el 15% restante)
- **Objetivo:** que `POST /recargas` con tarjeta inexistente devuelva 404, no 500.
- **Base guía:** `negocio/ProductService.java` (patrón `orElseThrow(() -> new ResponseStatusException(NOT_FOUND, ...))`) y `negocio/SaleService.java` (manejo de errores de negocio). En Feign el 404 del proveedor llega como `FeignException.NotFound`.
- **Cambios:**
  - `ms-recargas/.../service/RecargaService.java`: envolver `tarjetaClient.buscarPorId()` en `try/catch (FeignException.NotFound)` → `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarjeta no existe")`.
  - `ms-recargas/src/main/resources/application.properties`: eliminar `�`, dejar solo `spring.application.name`, `server.port=8082`, datasource, `spring.jpa.*`, `tarjetas.service.url=http://localhost:8081`.
  - Opcional: `RecargaController` → `201 Created` en POST.
- **Verificación:**
  ```http
  POST http://localhost:8081/tarjetas  → 200
  GET  http://localhost:8081/tarjetas  → 200 (lista)
  GET  http://localhost:8081/tarjetas/1 → 200
  POST http://localhost:8082/recargas {"idRecarga":1,"idTarjeta":1,"montoRecarga":50} → 200, con saldoDisponible copiado y fechaRecarga auto
  POST http://localhost:8082/recargas {"idRecarga":99,"idTarjeta":999,...} → 404 Tarjeta no existe
  ```
- **DONE:** los 5 casos dan el código esperado. Capturas para `pregunta1`.

### FASE 2 — Productor en `ms-recargas` (patrón `save → publish`)
- **Objetivo:** cada recarga guardada se publica en `atuncar_queue`.
- **Base guía:** `sales-services/build.gradle` (dep `spring-boot-starter-amqp`), `rabbitmq/RabbitMQConfig.java` (Exchange directo + Queue + Binding + `Jackson2JsonMessageConverter`), `rabbitmq/StockReserveEvent.java` (record), `rabbitmq/StockReserveProducer.java` (`convertAndSend(EXCHANGE, KEY, event)` + `LOGGER.info`), `negocio/SaleService.createSaleWithRabbitReserve()`, `application.yml` (`spring.rabbitmq.*`).
- **Cambios:**
  1. `ms-recargas/pom.xml`: agregar `org.springframework.boot:spring-boot-starter-amqp` (misma versión del parent 4.1.1).
  2. Nuevo `.../recargas/config/RabbitMQConfig.java`: constantes `PAYGO_EXCHANGE="paygo-exchange"`, `ATUNCAR_QUEUE="atuncar_queue"`, `ROUTING_KEY="atuncar.routing"`; beans `DirectExchange`, `Queue`, `Binding`, `Jackson2JsonMessageConverter`. Espejo de `RabbitMQConfig` de la guía.
  3. Nuevo `.../recargas/dto/RecargaMessage.java`: `idRecarga, idTarjeta, saldoDisponible, montoRecarga, fechaRecarga` (record o Lombok, igual que `StockReserveEvent`).
  4. Nuevo `.../recargas/messaging/RecargaProducer.java`: inyecta `RabbitTemplate`, método `publish(RecargaMessage)` con `convertAndSend` + log. Espejo de `StockReserveProducer`.
  5. Modificar `RecargaService.registrar()`: después de `recargaRepository.save()` → `producer.publish(new RecargaMessage(...))` → return. Espejo de `createSaleWithRabbitReserve()`.
  6. `application.properties`: agregar `spring.rabbitmq.host=localhost`, `port=5672`, `username=guest`, `password=guest` (valores de la guía).
- **Verificación (truco de `flujos-kafka-rabbit.md` §3.1):**
  - Con `ms-riesgo` aún apagado, `POST /recargas` → 200; en `http://localhost:15672` → `atuncar_queue` con 1 mensaje en `Ready` → `Get Message(s)` muestra el JSON.
- **DONE:** mensaje visible en consola RabbitMQ. Primera captura de `pregunta2`.

### FASE 3 — Scaffold `ms-riesgo` `:8083` (módulo nuevo, sin lógica aún)
- **Objetivo:** tercer microservicio compilando y levantando.
- **Base guía:** estructura `products-services` (paquetes `rest, negocio, repositorio, entidades, rabbitmq, dto, config`) + `application.yml` (datasource `createDatabaseIfNotExist`, `ddl-auto: update`, `rabbitmq`, `server.port`) + `build.gradle` (deps `web, amqp, data-jpa, mysql, lombok`).
- **Cambios (Maven, espejo de `ms-tarjetas/pom.xml` + AMQP):**
  - Copiar `ms-tarjetas/` como plantilla → renombrar a `ms-riesgo`, `group com.paygo`, `artifact ms-riesgo`, package base `com.paygo.riesgo`, clase `MsRiesgoApplication.java`.
  - `pom.xml`: mismos starters que tarjetas + `spring-boot-starter-amqp`.
  - `application.properties`: `spring.application.name=ms-riesgo`, `server.port=8083`, datasource `jdbc:mysql://localhost:3306/paygo_riesgo`, `username/password` igual que los otros, `ddl-auto=update`, `show-sql=true`, `spring.rabbitmq.*` igual que recargas.
- **Verificación:** `./mvnw spring-boot:run` → `Tomcat started on port 8083`, `GET http://localhost:8083/analisis` → 200 `[]` (tras Fase 5; en esta fase basta que levante).
- **DONE:** compila (`./mvnw -q compile`) y levanta en 8083.

### FASE 4 — Dominio `analisis` (entidad + repo)
- **Objetivo:** tabla que pide el examen.
- **Base guía:** `products-services/.../entidades/Product.java` (JPA `@Entity/@Table/@Id`) + `repositorio/ProductRepository extends JpaRepository`.
- **Cambios:**
  - Nuevo `.../riesgo/entity/Analisis.java` `@Table(name="analisis")`: `idRecarga @Id Long`, `idTarjeta Long`, `saldoDisponible Double`, `montoRecarga Double`, `fechaRecarga LocalDateTime`, `situacion String` (+ Lombok `@Data/@NoArgs/@AllArgs`, igual que `Tarjeta`/`Recarga`).
  - Nuevo `.../riesgo/repository/AnalisisRepository extends JpaRepository<Analisis, Long>`.
- **Verificación:** levantar `ms-riesgo` → MySQL crea tabla `paygo_riesgo.analisis` (`ddl-auto=update`).
- **DONE:** tabla existe con las 6 columnas.

### FASE 5 — Consumer + regla 70% + `GET /analisis`
- **Objetivo:** cerrar el flujo async completo.
- **Base guía:** `products-services/.../rabbitmq/StockReserveConsumer.java` (`@Component`, `@RabbitListener(queues=...)`, log) + `rabbitmq/RabbitMQConfig.java` (declarar misma queue/exchange del lado consumidor) + `rest/ProductController.getAllProducts()` (listar) + `negocio/ProductService` (servicio fino).
- **Cambios:**
  1. `.../riesgo/config/RabbitMQConfig.java`: mismas constantes que Fase 2 (`paygo-exchange`, `atuncar_queue`, `atuncar.routing`) + `Jackson2JsonMessageConverter`. La guía declara la infra en ambos lados (sales y products); hacemos lo mismo.
  2. `.../riesgo/dto/RecargaMessage.java`: mismo shape que el producer (para deserializar).
  3. Nuevo `.../riesgo/consumer/RiesgoConsumer.java`: `@RabbitListener(queues="atuncar_queue") onMessage(RecargaMessage m)` → `situacion = m.montoRecarga() <= 0.7*m.saldoDisponible() ? "Aprobada" : "Observada"` → `analisisRepository.save(new Analisis(...))` + log. Espejo de `onStockReserve()`.
  4. Nuevo `.../riesgo/service/AnalisisService.java` (`listar()` → `findAll()`) + `.../riesgo/controller/AnalisisController.java` (`@RequestMapping("/analisis")`, `GET → ResponseEntity<List<Analisis>>`). Espejo de `ProductController`/`SaleController`.
- **Verificación end-to-end:**
  ```http
  POST /recargas {"idRecarga":10,"idTarjeta":1,"montoRecarga":100}  # con saldo 1000 → Aprobada
  POST /recargas {"idRecarga":11,"idTarjeta":1,"montoRecarga":900}  # con saldo 1000 → Observada
  GET  http://localhost:8083/analisis → 200 con 2 filas y situacion correcta
  ```
  + log de `ms-riesgo` muestra consumo; `Ready` en consola vuelve a 0.
- **DONE:** ambos casos (Aprobada/Observada) verificados. Núcleo de `pregunta2`.

### FASE 6 — Evidencia y cierre (lo que da el Excelente)
- **Objetivo:** rúbricas exigen capturas; sin esto es Bueno (12/6) en vez de Excelente (13/7).
- **Base guía:** `backend/flujos-kafka-rabbit.md` (qué capturar: POST, Kafka-UI/consola Rabbit, logs productor y consumidor, GET final) + colecciones en `docs/postman/` de la guía como ejemplo de orden.
- **Cambios:**
  - Crear colección Postman (o `docs/postman/` con requests P1+P2) en el orden: `POST /tarjetas` → `GET /tarjetas` → `GET /tarjetas/{id}` → `POST /recargas` ok → `POST /recargas` 404 → `GET /analisis`.
  - Armar `pregunta1_apellido.docx` (Postman ambos servicios) y `pregunta2_apellido.docx` (POST recarga + mensaje en `15672` + log consumer + `GET /analisis`).
  - Actualizar `README.md` raíz: tabla puertos/BDs, `docker compose` infra, orden de levantada (`tarjetas → recargas → riesgo`, igual que `products → sales` en `backend/README.md` de la guía).
- **DONE:** dos `.docx` listos para BlackBoard + README permite reproducir la demo.

---

## 5. Orden de ejecución sugerido y commits

| Paso | Rama/Commit sugerido |
|---|---|
| Este plan | `docs/plan-implementacion-fases` → PR a `main` (este archivo) |
| Fase 0 | `chore/rabbitmq-infra` |
| Fase 1 | `fix/recargas-404-y-props` |
| Fase 2 | `feat/recargas-producer-rabbit` |
| Fase 3 | `feat/ms-riesgo-scaffold` |
| Fase 4 | `feat/riesgo-analisis-dominio` |
| Fase 5 | `feat/riesgo-consumer-y-listado` |
| Fase 6 | `docs/evidencia-postman-readme` |

Seguir `docs/guia-git.md`: una rama por tarea, PR con revisión, no pushear directo a `main`, `git pull origin main` cada 1–2 días.

## 6. Riesgos y notas

- **Versión Boot 4.1.1 (PAYGO) vs 3.2.5 (guía):** el starter `spring-boot-starter-amqp` y `Jackson2JsonMessageConverter` existen en ambas; si hay cambio de API menor, ajustar import sin cambiar el diseño.
- **Nombre de cola:** todo el plan usa `atuncar_queue` (del archivo `ATUNCAR-JOSE-...docx`). Si el que entrega tiene otro apellido, renombrar la constante en los dos `RabbitMQConfig` + `consumer` antes de grabar capturas.
- **IDs manuales:** `Tarjeta`/`Recarga` usan `@Id` sin `@GeneratedValue`; las capturas deben mandar `idTarjeta/idRecarga` explícitos. No cambiar a auto sin avisar al grupo (rompe requests ya probadas).
