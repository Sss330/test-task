# Prompt

Я выполняю тестовое задание для Java/Kotlin developer.

Сгенерируй полный Java Spring Boot проект для тестового задания.

Нужно реализовать минимальное чат-приложение по типу ICQ.

Проект должен быть полностью компилируемым и запускаемым.

## Ограничения/допущения

Отправка сообщения самому себе запрещена намеренно. В рамках этого тестового приложения чат рассматривается как переписка между двумя разными пользователями. В production-версии это правило можно было бы изменить и реализовать отдельный сценарий saved messages.

Клиентская часть не обязательна. Проверка приложения должна быть возможна через REST-клиент: Postman или curl.

В приложении должна быть реализована упрощённая demo-идентификация пользователя через HTTP header `X-User-Id`. Это не полноценная production-авторизация, но сервер должен проверять, что пользователь не выполняет действия от имени другого пользователя.

## Технологии

Использовать:

- Java 21
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- H2 Database
- Bean Validation
- Lombok
- Server-Sent Events
- Docker
- JUnit 5
- Mockito

## Базовый package

Использовать базовый package:

```text
test.task.bfg
```

## Структура проекта

Создать такую структуру пакетов:

- config
- controller
- exception
- model.dto.request
- model.dto.response
- model.dto.response.error
- model.entity
- model.enums
- repository
- service

## Содержательный кейс

Реализовать минимальное чат-приложение.

Пользователи могут создавать аккаунты, отправлять сообщения друг другу, получать сообщения асинхронно через SSE, просматривать историю переписки и отмечать сообщения прочитанными.

Сообщения и пользователи должны храниться в базе данных.

## Функциональные требования

1. Пользователь может быть создан через REST API.
2. Пользователи должны храниться в БД.
3. Сообщения должны храниться в БД.
4. Пользователь может отправить сообщение другому пользователю.
5. Нельзя отправить сообщение самому себе.
6. Сообщение должно иметь статус:
    - SENT
    - DELIVERED
    - READ
7. Если получатель подключён к SSE stream, сообщение сразу становится DELIVERED.
8. Пользователь может подписаться на личный SSE stream по userId.
9. Каждый пользователь получает только свои SSE-события.
10. При подключении к SSE stream пользователь должен получить pending-сообщения со статусом SENT.
11. При доставке pending-сообщений отправитель должен получить событие об изменении статуса сообщения.
12. Получатель может отметить сообщение прочитанным.
13. Только получатель может отметить сообщение прочитанным.
14. Можно получить историю переписки двух пользователей.
15. Должна быть валидация входящих запросов.
16. Должна быть централизованная обработка ошибок.
17. Должно быть логирование ошибок запросов.
18. Должны быть unit-тесты для service layer.

## Demo authentication

Добавить упрощённую серверную проверку пользователя через HTTP header:

```text
X-User-Id: USER_ID
```

Требования:

- при отправке сообщения `X-User-Id` должен совпадать с `senderId`;
- при отметке сообщения прочитанным `X-User-Id` должен совпадать с `readerId`;
- при подписке на SSE stream `X-User-Id` должен совпадать с `userId`;
- при получении сообщения пользователь должен быть sender или receiver сообщения;
- при получении истории переписки пользователь должен быть одним из участников переписки;
- если проверка не проходит, возвращать `403 Forbidden`.

Это demo-идентификация для тестового проекта, не полноценная production-авторизация. В production-версии следовало бы использовать JWT, session-based authentication или OAuth2/OpenID Connect.

## REST API

Сделать endpoints:

```text
POST /api/users
GET /api/users
GET /api/users/{id}

POST /api/messages
GET /api/messages/{messageId}
GET /api/messages/conversation?firstUserId=...&secondUserId=...
PATCH /api/messages/{messageId}/read?readerId=...
GET /api/messages/stream?userId=...
```

Для endpoints `/api/messages/**` использовать обязательный header:

```text
X-User-Id: USER_ID
```

Endpoints `/api/users/**` доступны без header, так как используются для создания и получения пользователей в demo-приложении.

## DTO

Создать request DTO:

- CreateUserRequest
- SendMessageRequest

CreateUserRequest поля:

- username, not blank, max 64
- displayName, not blank, max 128

SendMessageRequest поля:

- senderId, not null, UUID
- receiverId, not null, UUID
- text, not blank, max 2000

Создать response DTO:

- UserResponse
- MessageResponse
- ErrorResponse

UserResponse поля:

- id
- username
- displayName
- createdAt

MessageResponse поля:

- id
- senderId
- senderUsername
- receiverId
- receiverUsername
- text
- status
- createdAt
- deliveredAt
- readAt

ErrorResponse поля:

- timestamp
- status
- error
- message
- path

DTO request можно делать через Java record.

DTO response можно делать через Java record или Lombok builder.

## Entity

Создать JPA entity User.

Поля User:

- id UUID
- username unique, not null, max 64
- displayName not null, max 128
- createdAt not null, updatable false

Создать JPA entity Message.

Поля Message:

- id UUID
- sender User, ManyToOne LAZY, not null
- receiver User, ManyToOne LAZY, not null
- text not null, max 2000
- status enum STRING, not null
- createdAt not null, updatable false
- deliveredAt nullable
- readAt nullable

Добавить enum MessageStatus:

- SENT
- DELIVERED
- READ

В Message добавить методы:

- markDelivered()
- markRead()

markDelivered должен менять статус только если текущий статус SENT.

markRead должен менять статус на READ, заполнять readAt и, если deliveredAt пустой, заполнять deliveredAt.

## Repository

Создать UserRepository extends JpaRepository<User, UUID>.

Методы:

- existsByUsername
- findByUsername

Создать MessageRepository extends JpaRepository<Message, UUID>.

Методы:

- findConversation через JPQL для двух пользователей, сортировка по createdAt asc
- findByReceiver_IdAndStatus

## Service

Создать UserService.

Методы:

- create
- findAll
- findById

Создать MessageService.

Методы:

- send(UUID currentUserId, SendMessageRequest request)
- findById(UUID currentUserId, UUID messageId)
- findConversation(UUID currentUserId, UUID firstUserId, UUID secondUserId)
- markAsRead(UUID currentUserId, UUID messageId, UUID readerId)
- subscribe(UUID currentUserId, UUID userId)

Логика send:

- проверить, что `currentUserId` совпадает с `senderId`, иначе вернуть 403;
- проверить, что senderId и receiverId разные, иначе вернуть 400;
- найти sender и receiver;
- создать Message со статусом SENT;
- если receiver подключён к SSE, отметить сообщение DELIVERED;
- сохранить сообщение;
- отправить receiver событие message-received;
- отправить sender событие message-status-updated;
- вернуть MessageResponse.

Логика findById:

- найти сообщение по id;
- проверить, что `currentUserId` является отправителем или получателем сообщения;
- если пользователь не участник сообщения, вернуть 403;
- вернуть MessageResponse.

Логика findConversation:

- проверить, что `currentUserId` является одним из участников переписки;
- если пользователь не участник переписки, вернуть 403;
- проверить существование обоих пользователей;
- вернуть историю переписки, отсортированную по createdAt asc.

Логика markAsRead:

- проверить, что `currentUserId` совпадает с `readerId`, иначе вернуть 403;
- найти сообщение;
- проверить, что readerId является получателем сообщения, иначе вернуть 400;
- отметить сообщение прочитанным;
- отправить sender событие message-status-updated;
- отправить receiver событие message-status-updated;
- вернуть MessageResponse.

Логика subscribe:

- проверить, что `currentUserId` совпадает с `userId`, иначе вернуть 403;
- проверить, что user существует;
- создать SSE-подписку через SseNotificationService;
- найти pending сообщения пользователя со статусом SENT;
- каждое pending сообщение отметить DELIVERED;
- отправить пользователю событие message-received;
- отправить отправителю событие message-status-updated;
- вернуть SseEmitter.

Создать SseNotificationService.

Требования к SseNotificationService:

- хранить подписки в ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>;
- при subscribe добавлять emitter по userId;
- удалять emitter при completion, timeout и error;
- иметь метод hasSubscribers(UUID userId);
- иметь метод send(UUID userId, String eventName, Object payload);
- каждый пользователь должен получать только свои события;
- при подключении отправлять событие connected;
- если отправка SSE-события в emitter не удалась, удалить этот emitter из списка подписок.

## Exceptions

Создать custom exceptions:

- NotFoundException
- ConflictException
- BadRequestException
- ForbiddenException

Создать GlobalExceptionHandler через RestControllerAdvice.

Он должен обрабатывать:

- NotFoundException -> 404
- ConflictException -> 409
- BadRequestException -> 400
- ForbiddenException -> 403
- MethodArgumentNotValidException -> 400
- MissingServletRequestParameterException -> 400
- MissingRequestHeaderException -> 400
- MethodArgumentTypeMismatchException -> 400
- Exception -> 500

Ошибки возвращать в формате ErrorResponse.

Также нужно логировать ошибки запросов:

- 4xx ошибки логировать на уровне WARN;
- unexpected 5xx ошибки логировать на уровне ERROR со stacktrace.

## Lombok

Использовать Lombok аккуратно:

- @RequiredArgsConstructor в service и controller
- @Getter для JPA entity
- @NoArgsConstructor(access = AccessLevel.PROTECTED) для JPA entity
- @Builder можно использовать для response DTO
- не использовать @Data на JPA entity

## application.yml

Настроить:

- server.port=8080
- H2 database jdbc:h2:mem:chatdb
- H2 console enabled path /h2-console
- spring.jpa.hibernate.ddl-auto=update
- spring.jpa.open-in-view=false
- spring.jpa.show-sql=true

## Docker

Добавить Dockerfile.

Dockerfile должен:

- использовать maven:3.9-eclipse-temurin-21 для сборки
- копировать pom.xml и src
- выполнять mvn clean package -DskipTests
- использовать eclipse-temurin:21-jre для запуска
- запускать java -jar app.jar
- открывать порт 8080

Добавить docker-compose.yml.

docker-compose.yml должен запускать приложение и пробрасывать порт 8080:8080.

## README

Добавить README.md.

README должен содержать:

- краткое описание проекта
- список возможностей
- технологии
- требования для запуска;
- запуск локально;
- запуск через Docker;
- H2 console;
- описание demo authentication через header `X-User-Id`;
- примеры curl для всех endpoints;
- пример SSE-подписки;
- описание SSE events:
    - connected
    - message-received
    - message-status-updated
- основной сценарий проверки приложения;
- описание формата ошибок;
- ограничения текущей реализации.

## Тесты

Добавить unit-тесты для service layer с JUnit 5 и Mockito.

Минимальные тесты:

UserServiceTest:

- create should create user;
- create should throw ConflictException when username exists;
- findById should throw NotFoundException when user not found.

MessageServiceTest:

- send should create message;
- send should throw ForbiddenException when current user is not sender;
- send should throw BadRequestException when sender sends message to himself;
- markAsRead should allow receiver to mark message as read;
- markAsRead should throw ForbiddenException when current user is not reader;
- markAsRead should throw BadRequestException when reader is not receiver.

## Важно

Код должен быть полным.

Не оставлять пустые классы.

Не использовать заглушки вместо рабочей логики.

Проект должен запускаться командой:

```bash
./mvnw spring-boot:run
```

или:

```bash
mvn spring-boot:run
```

Также проект должен запускаться через:

```bash
docker compose up --build
```