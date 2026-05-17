# E-Wallet Service

Backend сервис цифрового кошелька на **Kotlin + Spring Boot 3**.

## Стек технологий

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin 1.9 |
| Framework | Spring Boot 3.3 |
| База данных | PostgreSQL 16 + Flyway |
| Кэш | Redis 7 |
| Очередь | Apache Kafka |
| Auth | Spring Security + JWT (jjwt) |
| Документация | Springdoc OpenAPI / Swagger UI |
| Тесты | JUnit 5 + TestContainers |
| Инфра | Docker Compose |

## Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                      HTTP Client                        │
└──────────────────────────┬──────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │   Spring Boot App       │
              │  ┌───────────────────┐  │
              │  │  JWT Auth Filter  │  │
              │  └────────┬──────────┘  │
              │           │             │
              │  ┌────────▼──────────┐  │
              │  │   Controllers     │  │
              │  │ Auth | Wallet     │  │
              │  └────────┬──────────┘  │
              │           │             │
              │  ┌────────▼──────────┐  │
              │  │    Services       │  │
              │  │ AuthSvc|WalletSvc │  │
              │  └──┬──────────┬─────┘  │
              │     │          │        │
              └─────┼──────────┼────────┘
                    │          │
         ┌──────────▼───┐  ┌───▼──────────────┐
         │  PostgreSQL  │  │  Redis (кэш       │
         │  (основная   │  │  баланса, 5 мин)  │
         │  БД + Flyway)│  └───────────────────┘
         └──────────────┘
                    │
         ┌──────────▼───────────────────────────┐
         │  Kafka Topic: wallet.transactions     │
         │  Producer: после каждой транзакции   │
         │  Consumer: сохраняет в audit_log     │
         └──────────────────────────────────────┘
```

## Ключевые паттерны

### Оптимистичная блокировка (race conditions)
Сущность `Wallet` использует `@Version` — при параллельных переводах JPA
автоматически бросает `ObjectOptimisticLockingFailureException`, которое
перехватывается и повторяется до 3 раз через `@Retryable`.

### Идемпотентность переводов
`POST /api/wallet/transfer` требует `idempotencyKey`. Если запрос уже
выполнялся — возвращается оригинальный результат без дублирования транзакции.

### Event-driven аудит
Каждая транзакция публикует событие в Kafka topic `wallet.transactions`.
Отдельный consumer сохраняет аудит-лог в таблицу `audit_log` (JSONB payload).

### Redis кэш баланса
`GET /api/wallet/balance` читает из Redis (TTL 5 мин). При deposit/transfer
кэш инвалидируется принудительно.

## Быстрый старт

### Требования
- Docker + Docker Compose
- JDK 21 (для локального запуска без Docker)

### Запуск инфраструктуры + приложения
```bash
docker-compose up --build
```

### Только инфраструктура (для разработки)
```bash
docker-compose up postgres redis kafka zookeeper
./gradlew bootRun
```

### Запуск тестов (TestContainers поднимает всё автоматически)
```bash
./gradlew test
```

## API Endpoints

| Метод | URL | Описание | Auth |
|-------|-----|----------|------|
| POST | `/api/auth/register` | Регистрация | — |
| POST | `/api/auth/login` | Вход | — |
| GET | `/api/wallet/balance` | Баланс (Redis кэш) | JWT |
| POST | `/api/wallet/deposit` | Пополнение | JWT |
| POST | `/api/wallet/transfer` | Перевод (идемпотентный) | JWT |
| GET | `/api/wallet/transactions` | История (пагинация) | JWT |

### Swagger UI
После запуска: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Примеры запросов

### Регистрация
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","fullName":"Иван Иванов"}'
```

### Пополнение
```bash
curl -X POST http://localhost:8080/api/wallet/deposit \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000.00}'
```

### Перевод (с idempotency key)
```bash
curl -X POST http://localhost:8080/api/wallet/transfer \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "toWalletId": "uuid-кошелька-получателя",
    "amount": 500.00,
    "idempotencyKey": "unique-key-123",
    "description": "Оплата"
  }'
```

## Структура проекта

```
src/main/kotlin/com/wallet/
├── config/          # Security, JWT, Redis, Kafka, OpenAPI конфиги
├── controller/      # REST контроллеры (Auth, Wallet)
├── domain/          # JPA сущности (User, Wallet, Transaction, AuditLog)
├── dto/             # Request/Response DTO
├── event/           # Kafka producer/consumer + TransactionEvent
├── exception/       # Кастомные исключения + GlobalExceptionHandler (RFC 7807)
├── repository/      # Spring Data JPA репозитории
└── service/         # Бизнес-логика (AuthService, WalletService)
```
