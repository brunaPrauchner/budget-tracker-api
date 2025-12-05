# budget-tracker-api

Spring Boot API for creating budget categories, logging expenses, and getting summary views (monthly totals, recent expenses).

## Build
```
mvn clean package
```

## Run
```
mvn spring-boot:run
```
The default profile uses an in-memory H2 database (PostgreSQL mode) and exposes the H2 console at `/h2-console`.

## API
- `POST /api/categories` - create a category `{ "name": "Food Out", "monthlyBudgetLimit": 250.00 }`
- `GET /api/categories` - list categories
- `POST /api/expenses` - log an expense `{ "categoryId": "<uuid>", "name": "Pizza", "amount": 18.50, "currency": "USD", "spentAt": "2024-12-03T18:00:00Z", "location": "Mario's" }`
- `GET /api/expenses/recent?limit=10` - last N expenses (default 10)
- `GET /api/categories/{categoryId}/expenses/recent?limit=5` - last N expenses for a category
- `GET /api/summary/monthly?year=2024&month=12` - totals per category for the given month

Timestamps use OffsetDateTime (ISO 8601 with offset). Amounts are BigDecimal with a 3-letter ISO code.

## Docs
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Holidays (Calendarific)
When creating an expense, the service looks up `spentAt` in Calendarific and annotates the expense with `holiday` (true/false) and `holidayName` when applicable.
Configure your key in `src/main/resources/application.properties`:
```
calendarific.api-key=YOUR_KEY
calendarific.country=US
calendarific.base-url=https://calendarific.com/api/v2
```
