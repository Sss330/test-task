# BFG AI Java Test Task

Минимальное чат-приложение на Spring Boot.

## Возможности

- создание пользователей;
- отправка сообщений между пользователями;
- хранение сообщений в БД;
- статусы сообщений: SENT, DELIVERED, READ;
- получение истории переписки;
- асинхронные уведомления через Server-Sent Events;
- Dockerfile и docker-compose.

## Технологии

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 Database
- SSE
- Maven
- Docker

## Запуск локально

```bash
mvn spring-boot:run