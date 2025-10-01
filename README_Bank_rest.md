# Bank Cards API

Проект: REST API для работы с банковскими картами и переводами.  
Реализованы основные сценарии: авторизация через JWT, управление картами, переводы между картами.

---

## 🚀 Возможности
- Регистрация и авторизация с JWT
- Управление банковскими картами (создание, блокировка, обновление срока действия, удаление)
- Получение списка карт (админ и пользовательские эндпойнты)
- Переводы между картами пользователя
- Swagger-документация

---

## 🛠️ Технологии
- Java 17+
- Spring Boot 3 (Web, Data JPA, Security, Validation)
- PostgreSQL + Liquibase
- JWT (io.jsonwebtoken)
- Swagger (springdoc-openapi)
- JUnit 5 + Mockito + Spring Security Test

---

## 📦 Сборка и запуск

### Требования
- **Java 17+** (Corretto, OpenJDK, Oracle JDK)
- **Maven 3.9+**
- **PostgreSQL** (локально или в Docker)

### Настройка базы
Создай базу данных и пропиши доступы в `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bankcards
    username: postgres
    password: postgres

Запуск

# сборка
mvn clean package

# запуск
mvn spring-boot:run


⸻

📖 Swagger UI

После запуска перейди:
👉 http://localhost:8080/swagger-ui/index.html

⸻

🔑 Авторизация

API защищено JWT.
	1.	Сначала войди:

curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"qwerty"}'

Пример ответа:

{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "expiry": "2030-01-01T00:00:00Z"
}

	2.	Используй токен в заголовке Authorization:

-H "Authorization: Bearer <TOKEN>"


⸻

📌 Примеры запросов

Создать карту пользователю (админ)

curl -X POST http://localhost:8080/cards/admin/owner/42 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"pan":"4111111111111111","expMonth":12,"expYear":2030}'

Получить список карт (админ, с фильтрами и пагинацией)

curl -X GET "http://localhost:8080/cards/admin?status=ACTIVE&page=0&size=10&sort=id,desc" \
  -H "Authorization: Bearer <TOKEN>"

Получить свои карты

curl -X GET "http://localhost:8080/cards/me?page=0&size=10" \
  -H "Authorization: Bearer <TOKEN>"

Заблокировать карту

curl -X PATCH http://localhost:8080/cards/77/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"status":"BLOCKED"}'

Перевод между картами

curl -X POST http://localhost:8080/transfers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"fromCardId":11,"toCardId":22,"amount":50.00,"description":"test move"}'


⸻

🧪 Тесты

Запуск всех тестов:

mvn test


⸻

📌 Структура проекта

src/main/java/com/example/bankcards
 ├── controller     # REST-контроллеры
 ├── dto            # DTO для запросов/ответов
 ├── entity         # JPA-сущности
 ├── service        # Бизнес-логика
 ├── security       # JWT + Spring Security
 └── web            # GlobalExceptionHandler


⸻

✅ Статус

Проект завершён.
Swagger доступен, тесты покрывают контроллеры, вся основная логика реализована.

⸻

