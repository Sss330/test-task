# BFG AI Java Test Task

Минимальное чат-приложение на Spring Boot для тестового задания.

Приложение позволяет создавать пользователей, отправлять сообщения между пользователями, хранить сообщения в базе данных, получать статусы доставки/прочтения и подписываться на асинхронные события через Server-Sent Events.

## Возможности

- создание пользователей;
- получение списка пользователей;
- отправка сообщений между пользователями;
- хранение пользователей и сообщений в БД;
- статусы сообщений: `SENT`, `DELIVERED`, `READ`;
- получение истории переписки двух пользователей;
- отметка сообщения прочитанным;
- асинхронные уведомления через Server-Sent Events;
- упрощённая серверная проверка пользователя через `X-User-Id`;
- запуск локально или через Docker.

## Технологии

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 Database
- Lombok
- Server-Sent Events
- Maven
- Docker

## Требования

Для локального запуска:

- Java 21
- Maven или Maven Wrapper

Для запуска через Docker:

- Docker
- Docker Compose

Переменные окружения не требуются.

## Запуск локально

Через Maven Wrapper на Windows:

```bash
mvnw.cmd spring-boot:run
```

Через Maven Wrapper на Linux/macOS:

```bash
./mvnw spring-boot:run
```

Через установленный Maven:

```bash
mvn spring-boot:run
```

После запуска приложение доступно по адресу:

```text
http://localhost:8080
```

## Запуск через Docker

```bash
docker compose up --build
```

Приложение будет доступно по адресу:

```text
http://localhost:8080
```

Остановить контейнеры:

```bash
docker compose down
```

## H2 Console

H2 console доступна по адресу:

```text
http://localhost:8080/h2-console
```

Параметры подключения:

```text
JDBC URL: jdbc:h2:mem:chatdb
User: sa
Password:
```

Пароль пустой.

## Demo authentication

В приложении используется упрощённая серверная проверка пользователя через HTTP header:

```text
X-User-Id: USER_ID
```

Это не полноценная production-авторизация, но сервер проверяет базовые действия пользователя:

- пользователь не может отправить сообщение от имени другого пользователя;
- пользователь не может подписаться на чужой SSE stream;
- пользователь не может получить чужое сообщение;
- пользователь не может получить чужую переписку;
- пользователь не может отметить сообщение прочитанным от имени другого пользователя.

В production-версии вместо этого следовало бы использовать JWT, session-based authentication или OAuth2/OpenID Connect.

## API

### Создать пользователя

```bash
curl.exe -X POST "http://localhost:8080/api/users" -H "Content-Type: application/json" -d '{ "username": "ivan", "displayName": "Ivan Ivanov" }'
```

Пример ответа:

```json
{
  "id": "USER_1_ID",
  "username": "ivan",
  "displayName": "Ivan Ivanov",
  "createdAt": "2026-06-06T12:00:00Z"
}
```

### Создать второго пользователя

```bash
curl.exe -X POST "http://localhost:8080/api/users" -H "Content-Type: application/json" -d '{ "username": "petr", "displayName": "Petr Petrov" }'
```

Пример ответа:

```json
{
  "id": "USER_2_ID",
  "username": "petr",
  "displayName": "Petr Petrov",
  "createdAt": "2026-06-06T12:01:00Z"
}
```

### Получить всех пользователей

```bash
curl.exe "http://localhost:8080/api/users"
```

### Получить пользователя по id

```bash
curl.exe "http://localhost:8080/api/users/USER_ID"
```

### Подписаться на SSE-события пользователя

В отдельном терминале:

```bash
curl.exe -N "http://localhost:8080/api/messages/stream?userId=RECEIVER_ID" -H "X-User-Id: RECEIVER_ID"
```

После подключения сервер отправит событие:

```text
event:connected
```

### Отправить сообщение

```bash
curl.exe -X POST "http://localhost:8080/api/messages" -H "Content-Type: application/json" -H "X-User-Id: SENDER_ID" -d '{ "senderId": "SENDER_ID", "receiverId": "RECEIVER_ID", "text": "Hello!" }'
```

Если получатель подключён к SSE stream, сообщение сразу получит статус `DELIVERED`.

### Получить сообщение по id

```bash
curl.exe "http://localhost:8080/api/messages/MESSAGE_ID" -H "X-User-Id: USER_ID"
```

Получить сообщение может только отправитель или получатель сообщения.

### Получить историю переписки

```bash
curl.exe "http://localhost:8080/api/messages/conversation?firstUserId=USER_1_ID&secondUserId=USER_2_ID" -H "X-User-Id: USER_1_ID"
```

Получить историю переписки может только один из участников этой переписки.

### Отметить сообщение прочитанным

```bash
curl.exe -X PATCH "http://localhost:8080/api/messages/MESSAGE_ID/read?readerId=RECEIVER_ID" -H "X-User-Id: RECEIVER_ID"
```

Отметить сообщение прочитанным может только получатель сообщения.

## SSE events

Сервер отправляет events:

| Event | Описание |
|---|---|
| `connected` | пользователь подключился к SSE stream |
| `message-received` | пользователь получил новое или pending-сообщение |
| `message-status-updated` | статус сообщения изменился |

## Основной сценарий проверки

1. Запустить приложение.
2. Создать пользователя `ivan`.
3. Создать пользователя `petr`.
4. Скопировать `id` пользователя `ivan` и использовать его как `SENDER_ID`.
5. Скопировать `id` пользователя `petr` и использовать его как `RECEIVER_ID`.
6. Открыть SSE stream для `petr` с header `X-User-Id: RECEIVER_ID`.
7. Отправить сообщение от `ivan` к `petr` с header `X-User-Id: SENDER_ID`.
8. Проверить, что в SSE stream пришло событие `message-received`.
9. Отметить сообщение прочитанным с header `X-User-Id: RECEIVER_ID`.
10. Получить историю переписки и проверить статус `READ`.

## Ошибки

Приложение возвращает ошибки в едином формате:

```json
{
  "timestamp": "2026-06-06T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Error message",
  "path": "/api/messages"
}
```

Основные статусы:

| Status | Причина |
|---|---|
| `400 Bad Request` | некорректный запрос, отсутствует параметр или header |
| `403 Forbidden` | пользователь пытается выполнить действие от имени другого пользователя |
| `404 Not Found` | пользователь или сообщение не найдено |
| `409 Conflict` | username уже занят |
| `500 Internal Server Error` | непредвиденная ошибка сервера |

## Ограничения текущей реализации

В приложении реализована упрощённая demo-идентификация через header `X-User-Id`.

Это закрывает серверную проверку базовых действий: пользователь не может отправить сообщение, прочитать сообщение, получить чужую переписку или подписаться на stream от имени другого пользователя без совпадения `X-User-Id`.

Это не полноценная production-авторизация. В реальном приложении следовало бы использовать JWT, session-based authentication или OAuth2/OpenID Connect.

Отправка сообщения самому себе запрещена намеренно. В рамках этого тестового приложения чат рассматривается как переписка между двумя разными пользователями. В production-версии это правило можно было бы изменить и реализовать отдельный сценарий saved messages.

Клиентская часть не реализована, так как по условиям задания допускается проверка через REST-клиент: Postman или curl.