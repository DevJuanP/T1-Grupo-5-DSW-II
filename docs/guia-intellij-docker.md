# Guía IntelliJ IDEA + Docker — T1-Grupo-5-DSW-II (PAYGO PERÚ)

> Cómo levantar todo el proyecto para hacer las pruebas de Pregunta 1 y 2 y ver los mensajes en Kafka y RabbitMQ. Complementa el [README](../README.md#--puertos-a-levantar) (tabla de puertos) y la [Guía Git](guia-git.md) (flujo de ramas/PRs).

## 1. Requisitos

- JDK 17 (el proyecto usa `java.version=17`, parent Boot `4.1.1`).
- IntelliJ IDEA (Community o Ultimate) con plugin Maven + Docker.
- Docker Desktop corriendo.
- Puertos libres (ver tabla en README): `8081, 8082, 8083, 3307, 9092, 8080, 5672, 15672`.

## 2. Abrir el proyecto en IntelliJ

1. `File → Open` → selecciona la carpeta `T1-Grupo-5-DSW-II` (la que contiene `ms-tarjetas/`, `ms-recargas/`, `ms-riesgo/`, `database/`, `infrastructure/`).
2. Confía en el proyecto cuando IntelliJ lo pida. Espera a que indexe y descargue dependencias Maven (barra inferior `Maven`).
3. Verifica SDK: `File → Project Structure → Project → SDK 17`. Si no aparece, agrégalo (`Add SDK → Download JDK 17` o apunta a tu Temurin 17).
4. En la ventana `Maven` deben aparecer 3 proyectos: `ms-tarjetas`, `ms-recargas`, `ms-riesgo`. Si falta alguno: clic derecho en su `pom.xml → Add as Maven Project`.

## 3. Levantar infraestructura con Docker

Usa la terminal integrada de IntelliJ (`Alt+F12`) desde la raíz del repo. Orden obligatorio:

```bash
# 1. MySQL (crea paygo_tarjetas, paygo_recargas, paygo_riesgo vía init.sql)
cd database && docker compose up -d && cd ..
# Verifica: docker ps → paygo-mysql healthy (puerto host 3307 → contenedor 3306)

# 2. Kafka (KRaft) + Kafka-UI
cd infrastructure && docker compose -f docker-compose-kafka.yml up -d

# 3. RabbitMQ
docker compose -f docker-compose-rabbitmq.yml up -d && cd ..
# Verifica: docker ps → paygo-kafka (9092), paygo-kafka-ui (8080), paygo-rabbitmq (5672, 15672)
```

Consolas web:

- Kafka-UI: `http://localhost:8080` → cluster `paygo` → tópico `atuncar_queue` → pestaña Messages.
- RabbitMQ: `http://localhost:15672` (guest/guest) → Queues → `atuncar_queue` → Get Message(s).

> No hay `docker-compose.yml` en la raíz: no ejecutar `docker compose up` desde la raíz.

## 4. Correr los 3 microservicios en IntelliJ (en orden)

Crea una Run Configuration por servicio (o usa 3 terminales). Orden: tarjetas → recargas → riesgo.

### Opción A: Run Configurations (recomendado para capturas)

1. `Run → Edit Configurations → + → Spring Boot` (o `Application` si tu edición no trae Spring).
2. Repite 3 veces:

| Config | Main class | Working dir / módulo | Variable de entorno `DB_URL` |
|---|---|---|---|
| `ms-tarjetas` | `com.paygo.tarjetas.MsTarjetasApplication` | `ms-tarjetas` | `jdbc:mysql://localhost:3307/paygo_tarjetas` |
| `ms-recargas` | `com.paygo.recargas.MsRecargasApplication` | `ms-recargas` | `jdbc:mysql://localhost:3307/paygo_recargas` |
| `ms-riesgo` | `com.paygo.riesgo.MsRiesgoApplication` | `ms-riesgo` | `jdbc:mysql://localhost:3307/paygo_riesgo` |

3. En cada config: `Environment variables → DB_URL=...` según la tabla. El resto usa defaults: `KAFKA_BOOT=localhost:9092`, `TARJETAS_URL=http://localhost:8081`, Rabbit `localhost:5672` guest/guest.
4. `Run` en orden `ms-tarjetas` → `ms-recargas` → `ms-riesgo`. Espera `Started ... in X seconds` antes de lanzar el siguiente.

### Opción B: Terminal (Maven wrapper)

```bash
# Terminal 1
cd ms-tarjetas
$env:DB_URL="jdbc:mysql://localhost:3307/paygo_tarjetas"
.\mvnw spring-boot:run   # :8081

# Terminal 2
cd ms-recargas
$env:DB_URL="jdbc:mysql://localhost:3307/paygo_recargas"
.\mvnw spring-boot:run   # :8082

# Terminal 3
cd ms-riesgo
$env:DB_URL="jdbc:mysql://localhost:3307/paygo_riesgo"
.\mvnw spring-boot:run   # :8083
```

> Si tu MySQL está en `3306` (local o `DB_PORT=3306`), no necesitas `DB_URL`.

## 5. Verificación rápida (antes de las capturas)

- `GET http://localhost:8081/tarjetas` → `200` (vacío `[]` la primera vez).
- `GET http://localhost:8082/recargas` → `200`.
- `GET http://localhost:8083/analisis` → `200`.
- `http://localhost:8080` abre Kafka-UI, `http://localhost:15672` abre RabbitMQ.

Colección Postman lista: `docs/postman/PAYGO-T1.postman_collection.json` + `docs/postman/PAYGO-Local.postman_environment.json` (bases `8081/8082/8083`).

## 6. Flujo para ver mensajes en Kafka y RabbitMQ (P2)

1. `POST :8081/tarjetas {idTarjeta:1, nomTitular:"...", saldoAsignado:1000, saldoDisponible:1000}`.
2. `POST :8082/recargas {idRecarga:11, idTarjeta:1, montoRecarga:100}` → el servicio valida por Feign, copia `saldoDisponible`, pone `fechaRecarga=now`, guarda y publica el mismo evento en **ambos** brokers (modo `both`).
3. Mira Kafka-UI `atuncar_queue → Messages`: aparece el JSON `idRecarga:11`.
4. Mira RabbitMQ `atuncar_queue → Get Message(s)`: aparece el mismo JSON.
5. `GET :8083/analisis`: `idRecarga:11 → Aprobada` (100 ≤ 70% de 1000). Repite con `montoRecarga:900` → `Observada`.

## 7. Problemas comunes

| Síntoma | Causa | Fix |
|---|---|---|
| `Communications link failure` / `Unknown database` | Apuntas a `3306` pero el compose expone `3307` | Pon `DB_URL=jdbc:mysql://localhost:3307/paygo_*` en la Run Config |
| `Port already in use 8080/8081/9092...` | Otro proceso o contenedor viejo | `docker ps`, `docker compose down` del compose viejo, o cierra la app que ocupa el puerto |
| `Servicio de tarjetas no disponible` (503) | `ms-tarjetas :8081` apagado o `TARJETAS_URL` mal | Levanta `ms-tarjetas` primero, verifica `GET :8081/tarjetas/1` |
| Kafka-UI vacío | El tópico se crea por código al publicar | Haz primero un `POST /recargas` válido, luego refresca Messages |
| Rabbit `Ready=0` tras publicar | `ms-riesgo` ya lo consumió (es lo esperado) | Detén `ms-riesgo` un momento, publica, mira `Ready=1`, vuelve a levantarlo |

## 8. Apagado

```bash
# Detén los 3 servicios en IntelliJ (Stop rojo) y luego:
cd database && docker compose down && cd ..
cd infrastructure && docker compose -f docker-compose-kafka.yml down
docker compose -f docker-compose-rabbitmq.yml down && cd ..
```

`down` conserva volúmenes (`mysql_data`, `rabbitmq_data`). Solo usa `down -v` si quieres borrar las BDs y empezar de cero.
