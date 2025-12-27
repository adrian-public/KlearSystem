# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

KLEAR System is a microservice-based, event-driven demonstration trading platform implementing order submission, validation, execution, clearing, and settlement. The architecture is deliberately decoupled using Spring Boot microservices communicating via Redis Pub/Sub.

**Key Architecture Principles:**
- Event-driven architecture with Redis Pub/Sub for all inter-service communication
- Each microservice has a single, well-defined responsibility (Single Responsibility Principle)
- Independent threading model: inbound and outbound message processing are decoupled
- Designed for horizontal, vertical, and orthogonal scaling
- Stateless processing (except Trade Service's in-memory status store)

## Building and Running

### Environment Setup

**JAVA_HOME must be set before building:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

### Build Commands

Build all modules from the root:
```bash
mvn clean install
```

Build individual services:
```bash
# From root directory, use -pl flag
mvn clean install -pl shared-libs
mvn clean install -pl trade-controller
mvn clean install -pl trade-service
mvn clean install -pl services/account-service
mvn clean install -pl services/execution-service
mvn clean install -pl services/clearing-service
mvn clean install -pl services/settlement-service
```

### Running with Docker Compose (Recommended)

Start all services:
```bash
docker compose up
```

Start in detached mode:
```bash
docker compose up -d
docker compose logs -f  # follow logs
```

Restart individual services:
```bash
docker compose up -d --force-recreate trade-controller
docker compose up -d --force-recreate trade-service
```

Stop all services:
```bash
docker compose down
```

### Running Services Manually

Start Redis:
```bash
docker run -d -p 6379:6379 --name klear-redis redis:7-alpine
```

Start each service (requires 6 terminals):
```bash
# Trade Controller Service
cd trade-controller
mvn spring-boot:run

# Trade Service
cd trade-service
mvn spring-boot:run

# Account Service
cd services/account-service
mvn spring-boot:run

# Execution Service
cd services/execution-service
mvn spring-boot:run

# Clearing Service
cd services/clearing-service
mvn spring-boot:run

# Settlement Service
cd services/settlement-service
mvn spring-boot:run
```

### Testing the System

Submit a trade:
```bash
curl -X POST http://localhost:8080/api/trades/submit \
  -H "Content-Type: application/json" \
  -d '{
        "clientId": "123456",
        "stockSymbol": "AAPL",
        "quantity": 100,
        "price": 150.0
      }'
```

Query trade status:
```bash
curl -X GET http://localhost:8080/api/trades/{ORDER_ID}/status
```

## System Architecture

### Service Overview

1. **Trade Controller Service** (`trade-controller/`)
   - Public-facing REST API (port 8080)
   - Endpoints: POST `/api/trades/submit`, GET `/api/trades/{orderId}/status`
   - Entry point for external clients
   - Communicates with Trade Service via Redis

2. **Trade Service** (`trade-service/`)
   - Core orchestrator managing trade lifecycle state machine
   - Maintains in-memory `ConcurrentHashMap<String, Trade>` for trade status
   - Coordinates with all downstream services
   - State transitions: UNKNOWN → VALIDATED → EXECUTED → CLEARED → SETTLED
   - **Build Note**: Configured with `<classifier>exec</classifier>` in spring-boot-maven-plugin to create both:
     - `trade-service-1.0-SNAPSHOT.jar` (library JAR for dependencies)
     - `trade-service-1.0-SNAPSHOT-exec.jar` (executable JAR for running)

3. **Account Service** (`services/account-service/`)
   - Validates account eligibility (margin, funds, limits)
   - First stage in trade lifecycle after submission

4. **Execution Service** (`services/execution-service/`)
   - Simulates trade execution (smart order routing placeholder)
   - In production: connects to exchanges/venues

5. **Clearing Service** (`services/clearing-service/`)
   - Handles post-trade clearing
   - In production: integrates with clearing houses

6. **Settlement Service** (`services/settlement-service/`)
   - Final settlement stage (asset/cash movements)
   - In production: integrates with payment systems

### Shared Libraries (`shared-libs/`)

Critical reusable components:

- **Communication Layer** (`com.klear.communication.core`)
  - `ServiceClient`: Abstract base for all service clients
  - `JedisPubSubAsync`: Async Redis pub/sub implementation
  - `ServiceClientMessage`: Message envelope for inter-service communication
  - Each service client extends `ServiceClient` (AccountServiceClient, ExecutionServiceClient, etc.)

- **Model Layer** (`com.klear.model`)
  - `Order`: Client order representation
  - `Trade`: Internal trade representation with lifecycle state
  - `OrderStatus`: Enum (UNKNOWN, VALIDATED, EXECUTED, CLEARED, SETTLED)
  - Response objects: `AccountResponse`, `ExecutionResponse`, `ClearingResponse`, `SettlementResponse`

### Message Flow Pattern

All services follow this pattern:

1. Service publishes message to `{service}_channel_OUT`
2. Target service subscribes to `{service}_channel_OUT`
3. Return messages use dynamically created return channels: `{service}_channel_RET_{UUID}`
4. Callback handlers process responses asynchronously

### Trade Lifecycle Flow

```
Client → Trade Controller
  → Trade Service (SUBMITTED)
  → Account Service (validation)
  → Trade Service (VALIDATED)
  → Execution Service (execution)
  → Trade Service (EXECUTED)
  → Clearing Service (clearing)
  → Trade Service (CLEARED)
  → Settlement Service (settlement)
  → Trade Service (SETTLED)
```

## Configuration

### Redis Connection

All services use these properties (configurable via environment variables):
- `redis_ip`: Redis host (default: localhost, Docker: redis)
- `redis_port`: Redis port (default: 6379)

### Channel Names

Defined in `application.properties` for each service:
- `account_service_channel_name = account_service_channel`
- `trade_service_channel_name = trade_service_channel`
- `execution_service_channel_name = execution_service_channel`
- `clearing_service_channel_name = clearing_service_channel`
- `settlement_service_channel_name = settlement_service_channel`

## Debugging

### Remote Debugging with IntelliJ

All services are configured with JDWP debug ports in docker-compose.yml:

| Service | Debug Port | Container Name |
|---------|-----------|----------------|
| trade-controller | 5005 | klear-trade-controller |
| trade-service | 5006 | klear-trade-service |
| account-service | 5007 | klear-account-service |
| execution-service | 5008 | klear-execution-service |
| clearing-service | 5009 | klear-clearing-service |
| settlement-service | 5010 | klear-settlement-service |

**IntelliJ Setup:**
1. Go to `Run` → `Edit Configurations...`
2. Click `+` → `Remote JVM Debug`
3. Configure:
   - Name: `Debug <service-name>`
   - Host: `localhost`
   - Port: `<debug-port>`
   - Module: `<service-name>`
4. Start the service with Docker Compose
5. Attach debugger from IntelliJ

**Verify debug connection:**
```bash
docker logs klear-trade-controller 2>&1 | grep -i "Listening for transport"
```

### Debugging Redis Pub/Sub

**Monitor all Redis activity:**
```bash
docker exec -it klear-redis redis-cli MONITOR
```

**List active channels:**
```bash
docker exec -it klear-redis redis-cli PUBSUB CHANNELS
```

**Subscribe to specific channel:**
```bash
docker exec -it klear-redis redis-cli SUBSCRIBE trade_service_channel_OUT
```

**Monitor all channels with pattern:**
```bash
docker exec -it klear-redis redis-cli PSUBSCRIBE "*"
```

**Debug a complete trade flow:**
```bash
# Terminal 1: Monitor Redis
docker exec -it klear-redis redis-cli MONITOR

# Terminal 2: Watch service logs
docker compose logs -f

# Terminal 3: Submit trade
curl -X POST http://localhost:8080/api/trades/submit \
  -H "Content-Type: application/json" \
  -d '{"clientId": "123", "stockSymbol": "AAPL", "quantity": 100, "price": 150.0}'
```

**Filter MONITOR output:**
```bash
docker exec -it klear-redis redis-cli MONITOR | grep PUBLISH
docker exec -it klear-redis redis-cli MONITOR | grep "trade_service"
```

### Following Trade Lifecycle in Logs

```bash
# Grep for specific orderId across all services
docker compose logs -f | grep {orderId}

# Watch specific service
docker compose logs -f trade-service
```

## Development Notes

### Threading Model

Each service implements a two-thread model:
- **Inbound thread**: Listens for incoming Redis messages
- **Outbound thread**: Sends messages to Redis
- This decoupling maximizes throughput and prevents blocking

### Concurrency

- Trade Service uses `ConcurrentHashMap` for thread-safe trade state management
- Multiple trades can be processed simultaneously at different lifecycle stages
- Services are designed to be stateless (except for Trade Service's status store)

### Spring Boot Integration

Services use Spring's dependency injection:
- `@Service` for business logic
- `@Component` for shared libraries
- `@PostConstruct` for initialization (Redis connections, callbacks)
- `@Value` for property injection
- `@Autowired` for dependency injection

### Important Build Considerations

**trade-service JAR Configuration:**
- Uses `<classifier>exec</classifier>` in spring-boot-maven-plugin
- This creates TWO jars:
  - `trade-service-1.0-SNAPSHOT.jar` - Normal JAR for use as a dependency by trade-controller
  - `trade-service-1.0-SNAPSHOT-exec.jar` - Executable fat JAR for running the service
- Docker Compose uses the `-exec.jar` version
- Without this configuration, trade-controller cannot depend on trade-service

**Module Build Order:**
The parent POM defines the correct build order:
1. shared-libs
2. account-service
3. execution-service
4. clearing-service
5. settlement-service
6. trade-service
7. trade-controller

### Adding a New Service

1. Create new module in `services/` or root
2. Add dependency on `shared-libs` in POM
3. Create `{Service}Application` class with `@SpringBootApplication`
4. Create `{Service}Manager` to handle Redis subscriptions
5. Implement business logic in `{Service}` class
6. Add service to `docker-compose.yml` with unique debug port
7. Add module to parent `pom.xml`

### Modifying Trade Lifecycle

1. Update `OrderStatus` enum in `shared-libs/src/main/java/com/klear/model/order/OrderStatus.java`
2. Add new callback method in `TradeServiceCallbackHandler` interface
3. Implement callback in `TradeService`
4. Update relevant service client to handle new status
5. Test entire lifecycle with curl commands

## Known Limitations & Future Work

1. **State Machine**: Only implements happy path (all validations pass)
2. **Logging**: Lacks structured logging with correlation IDs (currently uses System.out.println)
3. **Service Discovery**: Hardcoded Redis location (no Eureka/Consul)
4. **Resilience**: No circuit breakers, retries, or bulkheads
5. **Persistence**: Trade state is in-memory only
6. **Security**: No authentication, authorization, or TLS
7. **Business Logic**: Services contain placeholder/simulated logic

## Technology Stack

- **Java**: 17+ (21 tested)
- **Framework**: Spring Boot 3.2.10
- **Message Broker**: Redis (Jedis client)
- **Build Tool**: Maven 3.9.9+
- **Container Runtime**: Docker with Docker Compose
- **Serialization**: Jackson (JSON)

## Troubleshooting

### Build Issues

**"JAVA_HOME is not defined correctly":**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
mvn clean install
```

**trade-controller cannot find trade-service classes:**
- Ensure trade-service/pom.xml has `<classifier>exec</classifier>` in spring-boot-maven-plugin
- Rebuild: `mvn clean install -pl trade-service`

### Runtime Issues

**Services cannot connect to Redis:**
- Check Redis is running: `docker ps | grep redis`
- Verify environment variables in docker-compose.yml set `REDIS_IP=redis`

**Port conflicts:**
- Check ports 8080, 6379, 5005-5010 are available
- Use `docker ps` to see port mappings

**Service won't start:**
- Check logs: `docker compose logs <service-name>`
- Verify JAR was built: `ls -la services/*/target/*.jar trade-*/target/*.jar`
