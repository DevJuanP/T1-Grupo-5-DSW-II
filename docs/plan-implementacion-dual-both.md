# Plan de implementación dual Kafka + Rabbit — modo `both` (PAYGO PERÚ)

> **Rama:** `feat/mensajeria-dual-both` (creada desde `main` en Fase 0, `main` no se toca hasta PR final).
> **Objetivo usuario:** soporte dual — mantener Kafka actual y añadir RabbitMQ en paralelo, publicando y consumiendo en ambos.
> **Examen:** `ATUNCAR-JOSE-4697-DESARROLLO DE APLICACIONES WEB II-T1-T6EO-RONALD CASTILLO.docx` — P1 sincrónica 13pts (Feign) + P2 asíncrona 7pts (`Apellido_Queue`, tabla `analisis`, regla `Aprobada ≤70% / Observada >70%`, `GET /analisis`).
> **Guía única:** repo profesor `T1-DAW II` (Familia A) — `backend/sales-services/.../rabbitmq/* + kafka/*`, `backend/products-services/.../rabbitmq/*`, `queue/docker-compose-*.yml`, `backend/flujos-kafka-rabbit.md §3`, `backend/README.md` (orden de levantada).
> **Stack PAYGO:** Maven + Spring Boot `4.1.1` + Java 17 + `application.properties` (guía usa Gradle + Boot `3.2.5` + `application.yml`; el patrón Java es el mismo).

---

## 0. Estado real verificado (base de este plan)

| Pieza | Archivo:línea | Estado |
|---|---|---|
| `ms-tarjetas :8081` `POST /tarjetas`, `GET /tarjetas`, `GET /tarjetas/{id}` | `ms-tarjetas/.../controller/TarjetaController.java:7-33`, `entity/Tarjeta.java:17-21 idTarjeta,nomTitular,saldoAsignado,saldoDisponible` | ✅ P1 proveedor OK (POST devuelve `200`, ideal `201` — ver Fase 4) |
| `ms-recargas :8082` `POST /recargas →201`, `GET /recargas` | `ms-recargas/.../controller/RecargaController.java:15-27`, `entity/Recarga.java:17-21`, `dto/TarjetaResponse.java:9-12`, `dto/RecargaMessage.java:7-13 record 5 campos` | ✅ P1 consumidor OK |
| Feign + copia saldo + fecha auto + 404 | `client/TarjetaClient.java:7-13 @FeignClient(url=${tarjetas.service.url}) GET /tarjetas/{id}`, `service/RecargaService.java:29-40 try/catch NotFound→404 + setSaldoDisponible + setFechaRecarga(now) + save→publish→return`, `application.properties:9 tarjetas.service.url=http://localhost:8081` | ✅ ~90% (solo captura `NotFound`, falta `503/400/409` — ver Fase 4) |
| Kafka producer | `config/KafkaTopicConfig.java:26 ATUNCAR_TOPIC=atuncar_queue + NewTopic p1 r1 + ProducerFactory/KafkaTemplate JsonSerializer`, `messaging/RecargaProducer.java:22 kafkaTemplate.send(ATUNCAR_TOPIC,key,event)`, `pom.xml:47-49 spring-kafka`, `application.properties:8 bootstrap-servers=localhost:9092` | ✅ Funciona, es el path activo |
| Kafka consumer + regla 70% + `GET /analisis` | `ms-riesgo/.../consumer/RiesgoConsumer.java:22 @KafkaListener(topics=ATUNCAR_TOPIC)`, `config/KafkaTopicConfig.java:13 misma constante`, `service/AnalisisService.java:22-24 monto<=0.7*saldo?Aprobada:Observada`, `entity/Analisis.java:21-27 6 campos`, `controller/AnalisisController.java:14-28 GET /analisis`, `application.properties:8-15 group-id=ms-riesgo, JsonDeserializer→RecargaMessage` | ✅ Correcto incluido borde `=70% →Aprobada` |
| Infra Kafka | `infrastructure/docker-compose-kafka.yml:10-33 apache/kafka:3.8.0 KRaft (broker,controller, NODE_ID 1, listeners kafka:19092 + localhost:9092)`, `kafka-ui:37-47 provectuslabs/kafka-ui :8080` | ✅ Activa |
| Infra Rabbit (huérfana) | `infrastructure/docker-compose-rabbitmq.yml:11-16 rabbitmq:3-management, container_name: ligo-rabbitmq, 5672+15672, guest/guest` | ⚠️ Existe pero nada la usa; `container_name` inconsistente (`paygo-*` en resto) |
| Infra MySQL | `infrastructure/mysql-init/init.sql:1-3 CREATE paygo_tarjetas/recargas/riesgo`, `database/docker-compose.yml:3-12 mysql:8.0 3306:3306 root/1234 + volumes mysql_data + ./infrastructure/mysql-init` | ⚠️ **Path roto:** `./infrastructure/...` no existe desde `database/` (es `../infrastructure/...`); `README.md:26 docker compose up desde raíz` pero no hay `docker-compose.yml` en raíz |
| Docs desactualizados | `docs/plan-implementacion-fases.md:16,95-135` exige Rabbit (`DirectExchange paygo-exchange`, `RabbitTemplate`, `@RabbitListener`, `:15672`), `docs/postman/PAYGO-T1.postman_collection.json:133 Requiere RabbitMQ + :195 junto a :15672` | ⚠️ Código es Kafka; actualizar en Fase 5 |
| Artefacto stale | `ms-*/target/.../config/RabbitMQConfig.class` existe sin fuente (ahora `KafkaTopicConfig.java`) | ⚠️ `mvn clean` en Fase 4 |
| Props solo-localhost | `ms-tarjetas:3306/paygo_tarjetas`, `ms-recargas:3306/paygo_recargas + 8081`, `ms-riesgo:3306/paygo_riesgo + 9092` | ⚠️ Rompe en Docker (`mysql:3306`, `kafka:19092`, `http://ms-tarjetas:8081`) — ver Fase 4 |
| Evidencia | `glob **/*.docx = 0`, colección Postman P1 5 req + P2 3 req lista con tests `201/200/404` | ❌ Faltan `pregunta1_apellido.docx` + `pregunta2_apellido.docx` (Fase 6 = Excelente) |

Decisión cerrada con usuario: **mantener Kafka + añadir Rabbit en paralelo (`app.messaging.mode=both` por defecto, valores `kafka|rabbit|both`)**.

---

## 1. Reglas Git (main intacto)

```bash
cd "D:/Cibertec/6to ciclo/DAW II/T1/T1-Grupo-5-DSW-II"
git checkout main && git pull origin main
git checkout -b feat/mensajeria-dual-both   # YA HECHO en Fase 0
# ... trabajar solo aquí, 1 commit por fase, push a origin/feat/...
# PR a main solo al cierre, squash, borrar rama solo tras merge. Nunca push directo a main.
```

---

## 2. FASE 0 — Rama + archivado + este documento [ESTA EJECUCIÓN, SIN CÓDIGO]

**Objetivo:** aislar el trabajo, archivar el plan viejo (decisión `broker=RabbitMQ` superada), dejar este plan como única fuente.

**Cambios (solo docs/git, cero Java):**

1. `git checkout -b feat/mensajeria-dual-both` desde `main` limpio (`43b34c4`).
2. `mkdir docs/realizado && git mv docs/plan-implementacion-fases.md docs/realizado/plan-implementacion-fases.md`.
3. Crear este archivo `docs/plan-implementacion-dual-both.md`.
4. Commit: `docs: archiva plan fases en docs/realizado y añade plan dual both (fase 0)`.
5. Push: `git push -u origin feat/mensajeria-dual-both`.

**Verificación:**

```bash
git branch --show-current          # feat/mensajeria-dual-both
git status                        # limpio tras commit
git log --oneline -3              # muestra commit fase 0
git ls-files docs/realizado/      # plan-implementacion-fases.md
git ls-files docs/*.md            # guia-git.md + plan-implementacion-dual-both.md
git checkout main --dry-run 2>&1; git diff main...HEAD --stat  # main no modificado (solo rama adelanta)
ls docs/realizado/plan-implementacion-fases.md
```

**DONE:** rama existe en remoto, `main` en `43b34c4` sin cambios, historial viejo preservado. **STOP aquí en esta ejecución.**

---

## 3. FASE 1 — Flag `app.messaging.mode` + producer Rabbit en `ms-recargas` (futura)

**Base guía:** `T1-DAW II/backend/sales-services/.../rabbitmq/RabbitMQConfig.java + rabbitmq/StockReserveEvent.java + rabbitmq/StockReserveProducer.java (convertAndSend + log) + negocio/SaleService.createSaleWithRabbitReserve() (save→publish→return)` + `build.gradle: spring-boot-starter-amqp` + `application.yml: spring.rabbitmq.host/port/guest`.

**Cambios:**

1. `ms-recargas/pom.xml` (junto a `spring-kafka:47-49`, parent Boot `4.1.1`): añadir
   ```xml
   <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-amqp</artifactId></dependency>
   <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
   ```
2. `ms-recargas/src/main/resources/application.properties` añadir:
   ```properties
   app.messaging.mode=both
   spring.rabbitmq.host=localhost
   spring.rabbitmq.port=5672
   spring.rabbitmq.username=guest
   spring.rabbitmq.password=guest
   ```
   No tocar `spring.kafka.bootstrap-servers=localhost:9092` ni `tarjetas.service.url`.
3. Nuevo `.../recargas/config/RabbitMQConfig.java` (espejo guía):
   ```java
   @Configuration
   public class RabbitMQConfig {
     public static final String PAYGO_EXCHANGE="paygo-exchange";
     public static final String ATUNCAR_QUEUE="atuncar_queue";
     public static final String ROUTING_KEY="atuncar.routing";
     @Bean DirectExchange paygoExchange();
     @Bean Queue atuncarQueue(); // durable
     @Bean Binding binding(Queue q, DirectExchange ex);
     @Bean Jackson2JsonMessageConverter converter();
   }
   ```
4. Nuevo `.../recargas/messaging/RecargaRabbitProducer.java` (espejo `StockReserveProducer`):
   ```java
   @Component @RequiredArgsConstructor @Slf4j
   public class RecargaRabbitProducer {
     private final RabbitTemplate rabbitTemplate;
     public void publish(RecargaMessage m){
       rabbitTemplate.convertAndSend(RabbitMQConfig.PAYGO_EXCHANGE, RabbitMQConfig.ROUTING_KEY, m);
       log.info("[ms-recargas-rabbit] Publicado idRecarga={}", m.idRecarga());
     }
   }
   ```
   Reutilizar `dto/RecargaMessage.java` existente sin cambio.
5. Modificar `service/RecargaService.java:15-48`: inyectar `RecargaRabbitProducer + @Value("${app.messaging.mode:both}") String mode`; validar `idTarjeta/montoRecarga null o monto<=0 →400`; ampliar catch `RetryableException/ServiceUnavailable →503`; tras `save`: `if(mode kafka|both) recargaProducer.publish(msg); if(mode rabbit|both) rabbitProducer.publish(msg);`

**Verificación (riesgo apagado, truco `flujos-kafka-rabbit.md §3.1`):**

```bash
cd ms-recargas && ./mvnw -q compile
docker compose -f ../infrastructure/docker-compose-kafka.yml up -d
docker compose -f ../infrastructure/docker-compose-rabbitmq.yml up -d
curl -s -X POST localhost:8082/recargas -H "Content-Type: application/json" -d '{"idRecarga":20,"idTarjeta":1,"montoRecarga":50}'
# Kafka-UI http://localhost:8080 → paygo → atuncar_queue → Messages = 1 JSON
# RabbitUI http://localhost:15672 (guest/guest) → Queues → atuncar_queue Ready=1 → Get Message(s) mismo JSON
```

**DONE:** mismo evento en ambos brokers. Commit `feat: producer rabbit en recargas con flag both`.

---

## 4. FASE 2 — Consumer Rabbit + idempotencia en `ms-riesgo` (futura)

**Base guía:** `T1-DAW II/backend/products-services/.../rabbitmq/StockReserveConsumer.java (@Component @RabbitListener + log) + negocio/ProductService.decreaseStock()` + misma `RabbitMQConfig` declarada en consumidor.

**Cambios:**

1. `ms-riesgo/pom.xml`: mismas 2 deps AMQP + validation.
2. `ms-riesgo/.../application.properties`: mismo bloque `app.messaging.mode + spring.rabbitmq.*` de Fase 1 (Kafka `group-id=ms-riesgo` intacto).
3. Nuevo `.../riesgo/config/RabbitMQConfig.java` idéntico al de recargas.
4. Nuevo `.../riesgo/consumer/RiesgoRabbitConsumer.java`:
   ```java
   @Component @RequiredArgsConstructor @Slf4j
   public class RiesgoRabbitConsumer {
     private final AnalisisService analisisService;
     @RabbitListener(queues="atuncar_queue")
     public void onRecarga(RecargaMessage m){
       log.info("[ms-riesgo-rabbit] Recibida idRecarga={}", m.idRecarga());
       analisisService.evaluar(m);
     }
   }
   ```
   Mantener `RiesgoConsumer.java:22 @KafkaListener` pero que llame a `evaluar` (renombrar `registrar→evaluar` o delegar).
5. `service/AnalisisService.java:21-32` hacer idempotente (dual = doble entrega):
   ```java
   public Analisis evaluar(RecargaMessage m){
     if (analisisRepository.existsById(m.idRecarga())) return analisisRepository.getReferenceById(m.idRecarga());
     String situacion = m.montoRecarga() <= 0.7 * m.saldoDisponible() ? "Aprobada" : "Observada";
     return analisisRepository.save(new Analisis(m.idRecarga(),m.idTarjeta(),m.saldoDisponible(),m.montoRecarga(),m.fechaRecarga(),situacion));
   }
   ```
   Regla `<=` y strings `Aprobada/Observada` ya correctos, no cambiar.

**Verificación E2E:**

```http
POST :8082/recargas {"idRecarga":21,"idTarjeta":1,"montoRecarga":100} → 201 (100<=700 → Aprobada)
POST :8082/recargas {"idRecarga":22,"idTarjeta":1,"montoRecarga":900} → 201 (900>700 → Observada)
GET  :8083/analisis → 200 con 2 filas únicas (aunque lleguen x2 brokers)
```

+ logs `RiesgoConsumer` y `RiesgoRabbitConsumer`, `Ready` vuelve a 0 en Rabbit y offset avanza en Kafka.

**DONE:** consumo dual sin duplicados. Commit `feat: consumer rabbit en riesgo + idempotencia`.

---

## 5. FASE 3 — Infra + robustez (futura, commit `fix/infra-robustez`)

1. `database/docker-compose.yml:12` → `../infrastructure/mysql-init:/docker-entrypoint-initdb.d`. Crear `docker-compose.yml` en raíz (mysql + kafka + rabbit + 3 ms) o corregir `README.md:26`.
2. `infrastructure/docker-compose-rabbitmq.yml:12` `ligo-rabbitmq` → `paygo-rabbitmq`.
3. `./mvnw clean` en los 3 ms (borra `RabbitMQConfig.class` stale).
4. Props Docker-ready (env con fallback local):
   ```properties
   spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/paygo_recargas}
   spring.kafka.bootstrap-servers=${KAFKA_BOOT:localhost:9092}
   tarjetas.service.url=${TARJETAS_URL:http://localhost:8081}
   ```
   (replicar por servicio con su BD). Compose raíz define `DB_URL=mysql:3306`, `KAFKA_BOOT=kafka:19092`, `TARJETAS_URL=http://ms-tarjetas:8081`.
5. Nuevo `.../recargas/exception/GlobalExceptionHandler.java (@RestControllerAdvice)`: `DataIntegrityViolation→409`, `MethodArgumentNotValid→400`. Añadir `@NotNull/@Positive` en `Recarga`/`Tarjeta` sin cambiar `@Id` manual. `TarjetaController POST →201` para igualar recargas.
6. Verificación: `./mvnw test`, `docker compose config`, `POST duplicado→409`, `tarjetas caído→503`, `sin monto→400`.

---

## 6. FASE 4 — Postman dual (futura, commit `docs/postman-dual`)

- `docs/postman/PAYGO-T1.postman_collection.json:133` → `Capturas pregunta2. Requiere Kafka-UI :8080 y/o Rabbit :15672, mode=both`; `request 8:195` → `junto a :8080 (Kafka Messages) + :15672 (Queues Get Message) + logs consumer`.
- Añadir nota `solo-Rabbit: parar kafka, mode=rabbit, POST, Ready=1, levantar riesgo, Ready=0` (guía `flujos-kafka-rabbit.md §3`).
- `PAYGO-Local.postman_environment.json` sin cambio (`8081/8082/8083` OK). Tests `200/201/404` ya listos.

---

## 7. FASE 5 — Evidencia Excelente (futura, manual, commit `docs/evidencia`)

- `pregunta1_apellido.docx` (P1, 5 req colección): `POST /tarjetas`, `GET /tarjetas`, `GET /tarjetas/1` (el que usa Feign), `POST /recargas OK 201 con saldo copiado+fecha auto`, `POST /recargas 999→404`.
- `pregunta2_apellido.docx` (P2, 3 req + brokers): `POST 11→Aprobada`, `POST 12→Observada`, Kafka-UI `atuncar_queue/Messages`, RabbitUI `atuncar_queue/Get Message`, logs `Evento Kafka/Rabbit publicado + Recarga evaluada`, `GET :8083/analisis` 2 filas.
- Renombrar `atuncar_queue` al apellido del entregante **antes** de grabar. Actualizar `README.md` tabla `Kafka 9092/UI 8080 + Rabbit 5672/UI 15672`, orden `mysql→kafka+rabbit→8081→8082→8083`.

---

## 8. Cierre y riesgos

```bash
./mvnw -q -DskipTests compile  # en cada ms-*
docker compose config
# Postman 1-8 en orden, luego armar docx
git push origin feat/mensajeria-dual-both  # PR a main, squash, borrar rama solo tras merge
```

- Boot `4.1.1` vs guía `3.2.5`: `Jackson2JsonMessageConverter` igual, ajustar import si cambia.
- Sin idempotencia el dual duplica filas — por eso Fase 2 es bloqueante.
- `Double <=` borde flotante aceptable para examen.
- Rollback: `git checkout main`, `docker compose down -v`.
