# Withdrawal Notice System

Enviro365 Investments junior developer assessment (2026). A full-stack system where investors sign in, view their
portfolio, submit withdrawal notices, review their history and download a CSV statement.

- **Backend:** Spring Boot 3.3 (Java 17), Spring Data JPA, Spring Security, Bean Validation, H2
- **Frontend:** plain HTML, CSS and JavaScript, served by Spring Boot from `src/main/resources/static`, styled with
  Bootstrap 5 (vendored locally, no CDN/internet dependency) and an Enviro365 theme (emerald green brand color on
  clean neutrals, following the 60-30-10 rule)
- **Package:** `com.enviro.assessment.junior.kizitokayizzi`

## Setup

Requirements: JDK 17+ and Maven 3.9+.

```bash
mvn clean verify          # compiles and runs all tests
mvn spring-boot:run       # starts the app on http://localhost:8080
```

Open <http://localhost:8080> and sign in with a demo user (password for all: `Password123!`):

| User | Investor | Age | Why it is useful |
|---|---|---|---|
| `thabo` | Thabo Mokoena | 71 | Can withdraw from the retirement product |
| `lerato` | Lerato Dlamini | 36 | Retirement withdrawals are rejected |
| `sipho` | Sipho Nkosi | exactly 65 | Boundary case: rule is `age > 65`, so still rejected |

The H2 database is in memory and is re-seeded on every start. Every investor has a Retirement Annuity and a
Tax-Free Savings product.

## Business rules

Enforced on the server in `WithdrawalPolicy` (the UI repeats them only as a convenience):

1. Retirement withdrawals are only allowed if the investor's age is **greater than 65**.
2. A withdrawal must not exceed the product balance.
3. A withdrawal must not exceed **90% of the product balance** (rounded down to the cent).
4. Invalid input and rule violations return a clear message, shown in the UI.

Rules 2 and 3 are checked against the selected product, not the investor's total across products.

## API

All `/api/**` endpoints need HTTP Basic authentication. Errors always use this shape:

```json
{ "timestamp": "...", "status": 422, "error": "Unprocessable Entity",
  "message": "Withdrawal may not exceed 90% of the balance (maximum 39600.45).",
  "fieldErrors": { "amount": "..." } }
```

`fieldErrors` only appears for validation failures (400).

| Method | Path | Description | Success |
|---|---|---|---|
| GET | `/api/portfolio` | Investor details, products, balances and per-product maximum withdrawal | 200 |
| POST | `/api/withdrawals` | Create a withdrawal notice and deduct the balance | 201 |
| GET | `/api/withdrawals` | Withdrawal history, newest first | 200 |
| GET | `/api/withdrawals/export` | Same data as a CSV download | 200 |

`GET /api/withdrawals` and `/export` accept optional filters: `from` and `to` (`yyyy-MM-dd`, inclusive) and
`productId`.

**POST /api/withdrawals**

```json
{ "productId": 4, "amount": 1000.00 }
```

| Status | When |
|---|---|
| 201 | Notice created |
| 400 | Missing or invalid input (amount < 0.01, more than 2 decimals, no product), bad date, `from` after `to` |
| 401 | Missing or wrong credentials |
| 404 | Product does not exist or belongs to another investor |
| 422 | A business rule is broken (age, balance, 90% limit) |

The investor is taken from the login, never from the URL or body, so one investor cannot read or change another's data.

CSV columns: `Reference, Date, Product, Product Type, Amount, Balance Before, Balance After`.

## Design decisions

- **Layers:** controller (HTTP only) -> service (transactions, orchestration) -> repository (data). Rules live in one
  class, `WithdrawalPolicy`, so they are easy to test and explain.
- **DTOs:** entities are never exposed. Java records carry request and response data.
- **Money:** `BigDecimal` everywhere, never `double`.
- **Concurrency:** the product row is locked (`PESSIMISTIC_WRITE`) while a withdrawal is validated and applied, so two
  simultaneous requests cannot both pass the balance check.
- **History snapshot:** each notice stores `balanceBefore` and `balanceAfter`, so statements stay correct later.
- **Testable time:** a `Clock` bean is injected instead of calling `LocalDate.now()`, so age tests are deterministic.
- **Security:** stateless HTTP Basic with BCrypt-hashed passwords. CSRF protection is disabled on purpose because
  there are no cookie sessions. The 401 response omits `WWW-Authenticate` so the browser shows the page's own login
  form instead of its popup. The frontend keeps the credentials in `sessionStorage`; a production system would use
  tokens or an identity provider instead.
- **XSS:** the frontend writes server data with `textContent`, never `innerHTML`.

## Advanced requirements implemented

- Global exception handling (`GlobalExceptionHandler`)
- Input validation (`WithdrawalRequest` annotations, date and type checks)
- Unit tests (policy, service, CSV writer) plus API integration tests through the real security and validation stack
- UI validation (amount format, balance and 90% limit, date range)

DTOs are also used throughout.

## Tests

`mvn test` runs 25 tests:

- `WithdrawalPolicyTest`: age boundary (65 rejected, 66 allowed), balance, 90% limit and rounding
- `WithdrawalServiceTest`: balance deduction, ownership, no changes when a rule fails
- `CsvStatementWriterTest`: header, rows and escaping
- `WithdrawalApiIntegrationTest`: authentication, validation errors, rules, ownership, CSV export and filters

## AI usage disclosure

This project was built with AI assistance (GitHub Copilot in VS Code, Claude model). The AI generated the initial code
for the backend, frontend and tests, and drafted this README, working from my answers to its questions about design
choices (frameworks, features, business rule interpretation, security approach). I reviewed the result, ran the
build and tests, and checked the UI in a browser. I can explain every part of the code and the decisions above.

## Screenshots

_Add screenshots here: login, portfolio dashboard, withdrawal form with an error, history table._
