# Prompt

Я выполняю тестовое задание для Java/Kotlin developer.

Цель задания - показать умение использовать LLM для генерации проекта, затем критически оценить сгенерированный код, исправить ошибки, улучшить архитектуру, добавить документацию и упаковать приложение.

Итоговый результат должен содержать один воспроизводимый prompt.md, по которому можно сгенерировать рабочий проект за один запуск LLM.

Сгенерируй полный Java Spring Boot проект для тестового задания Middle Java Developer.
Нужно реализовать минимальное чат-приложение по типу ICQ.

Проект должен быть полностью компилируемым и запускаемым.


## Ограничения/допущения
Отправка сообщения самому себе запрещена намеренно. В рамках этого тестового приложения чат рассматривается как переписка между двумя разными пользователями. В production-версии это правило можно было бы изменить и реализовать отдельный сценарий "Saved messages" как в TG.

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

## Базовый package

Использовать базовый package:

test.task.bfg

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

## REST API

Сделать endpoints:

POST /api/users
GET /api/users
GET /api/users/{id}

POST /api/messages
GET /api/messages/{messageId}
GET /api/messages/conversation?firstUserId=...&secondUserId=...
PATCH /api/messages/{messageId}/read?readerId=...
GET /api/messages/stream?userId=...

Также нужно логировать ошибки запросов:
- 4xx ошибки логировать на уровне WARN;
- unexpected 5xx ошибки логировать на уровне ERROR со stacktrace.

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

- send
- findById
- findConversation
- markAsRead
- subscribe

Логика send:

- проверить, что senderId и receiverId разные
- найти sender и receiver
- создать Message со статусом SENT
- если receiver подключён к SSE, отметить сообщение DELIVERED
- сохранить сообщение
- отправить receiver событие message-received
- отправить sender событие message-status-updated
- вернуть MessageResponse

Логика subscribe:

- проверить, что user существует
- создать SSE-подписку через SseNotificationService
- найти pending сообщения пользователя со статусом SENT
- каждое pending сообщение отметить DELIVERED
- отправить пользователю событие message-received
- отправить отправителю событие message-status-updated
- вернуть SseEmitter

Создать SseNotificationService.

Требования к SseNotificationService:

- хранить подписки в ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>
- при subscribe добавлять emitter по userId
- удалять emitter при completion, timeout и error
- иметь метод hasSubscribers(UUID userId)
- иметь метод send(UUID userId, String eventName, Object payload)
- каждый пользователь должен получать только свои события
- при подключении отправлять событие connected

## Exceptions

Создать custom exceptions:

- NotFoundException
- ConflictException
- BadRequestException

Создать GlobalExceptionHandler через RestControllerAdvice.

Он должен обрабатывать:

- NotFoundException -> 404
- ConflictException -> 409
- BadRequestException -> 400
- MethodArgumentNotValidException -> 400
- MissingServletRequestParameterException -> 400
- MethodArgumentTypeMismatchException -> 400
- Exception -> 500

Ошибки возвращать в формате ErrorResponse.

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
- запуск локально
- запуск через Docker
- H2 console
- примеры curl для всех endpoints
- пример SSE-подписки
- описание SSE events:
    - connected
    - message-received
    - message-status-updated

## Тесты

Добавить unit-тесты для service layer с JUnit 5 и Mockito.

Минимальные тесты:

UserServiceTest:

- create should create user
- create should throw ConflictException when username exists
- findById should throw NotFoundException when user not found

MessageServiceTest:

- send should create message
- send should throw BadRequestException when sender sends message to himself
- markAsRead should allow receiver to mark message as read
- markAsRead should throw BadRequestException when reader is not receiver

## Важно

Код должен быть полным.
Не оставлять пустые классы.
Не использовать заглушки вместо рабочей логики.
Проект должен запускаться командой:

./mvnw spring-boot:run

или:

mvn spring-boot:run

Также проект должен запускаться через:

docker compose up --build