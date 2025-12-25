# Country-Based Product API (Ktor)

Ktor service that manages a product catalog and applies discounts with database-level idempotency under concurrency.

Key features:
- Persistent storage (PostgreSQL in production; H2 in-memory for local/dev/tests)
- Endpoints:
  - `GET /products?country={country}` returns products with final price (VAT + discounts)
  - `PUT /products/{id}/discount` applies a discount idempotently per product
- Database-enforced guarantee: the same discount cannot be applied more than once to the same product

See `ARCHITECTURE.md` for design notes and sequence diagrams.

## Requirements
- JDK 17+
- Gradle (Wrapper included)
- PostgreSQL (optional; for production run)

## Configure

Application config: `src/main/resources/application.yaml`

Defaults:
```
ktor:
  deployment:
    port: 8080
db:
  embedded: true   # H2 in-memory for local/dev and tests
postgres:
  url: "jdbc:postgresql://localhost/default"
  user: username
  password: password
```

- Set `db.embedded=false` to use PostgreSQL.
- Provide `postgres.url`, `postgres.user`, and `postgres.password` when using PostgreSQL.

## Build & Test

- Build: `./gradlew build`
- Run tests: `./gradlew test`

The test `ProductApiTest.applyDiscount_isIdempotent_underConcurrency` fires many concurrent HTTP requests and verifies only one discount application is persisted.

## Run locally

Using the embedded H2 DB (default):
```
./gradlew run
```

Using PostgreSQL:
1. Set `db.embedded=false` in `application.yaml`
2. Provide `postgres.url`, `postgres.user`, `postgres.password`
3. Run: `./gradlew run`

The server listens on `http://localhost:8080`.

## Example curl commands

- List products for Sweden:
```
curl -s "http://localhost:8080/products?country=Sweden" | jq
```

- Apply a discount idempotently to product `swe-001`:
```
curl -s -X PUT "http://localhost:8080/products/swe-001/discount" \
  -H "Content-Type: application/json" \
  -d '{
        "discountId": "BLACKFRIDAY2025",
        "percent": 10.0
      }'
```

Repeated invocations with the same `discountId` will return `200 OK` with `{ "applied": false }` after the first successful application.

## Notes

- VAT rules hardcoded per requirement:
  - Sweden: 25%
  - Germany: 19%
  - France: 20%
- Final price formula: `final = basePrice × (1 - totalDiscount%) × (1 + VAT%)` rounded to 2 decimals.

