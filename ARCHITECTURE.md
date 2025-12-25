### Architecture Overview

This Ktor service provides a simple Product API backed by a relational database. It stores products and applied discounts, calculates final prices including country VAT, and enforces idempotent, concurrency-safe discount application at the database level.

#### Tech stack
- Ktor (server, routing, content negotiation)
- JDBC (raw) with H2 for local/dev/tests and PostgreSQL for production
- Jackson + Kotlin serialization support for JSON

#### Data model
- Table `products(id, name, base_price, country)`
- Table `product_discounts(product_id, discount_id, percent)`, with primary key `(product_id, discount_id)`

The composite primary key enforces the constraint: the same `discount_id` cannot be applied more than once to the same product.

#### VAT
VAT rules are fixed as per requirements:
- Sweden: 25%
- Germany: 19%
- France: 20%

Final price formula used in responses:
```
final = basePrice × (1 - totalDiscount%) × (1 + VAT%)
```
Rounded to 2 decimals.

#### Concurrency and idempotency
The `PUT /products/{id}/discount` endpoint attempts to insert a row into `product_discounts`. Under contention, the first request will succeed; subsequent concurrent inserts with the same `(product_id, discount_id)` will fail with a unique/duplicate key violation, which is caught and treated as an idempotent success (no state change, `applied=false`). This guarantees database-level enforcement even under heavy concurrent load.

### Sequence diagrams

#### GET /products
```mermaid
sequenceDiagram
  participant C as Client
  participant K as Ktor Server
  participant DB as Database

  C->>K: GET /products?country=Sweden
  K->>DB: SELECT p.*, d.* FROM products LEFT JOIN product_discounts ... WHERE country = 'Sweden'
  DB-->>K: Rows (products with discounts)
  K->>K: Compute finalPrice per product (VAT + discounts)
  K-->>C: 200 OK [JSON array of products]
```

#### PUT /products/{id}/discount
```mermaid
sequenceDiagram
  participant C as Client
  participant K as Ktor Server
  participant DB as Database

  C->>K: PUT /products/{id}/discount {discountId, percent}
  K->>DB: SELECT 1 FROM products WHERE id = ?
  DB-->>K: Exists?
  alt Product not found
    K-->>C: 404 Not Found
  else Product exists
    K->>DB: INSERT INTO product_discounts(product_id, discount_id, percent)
    alt First time
      DB-->>K: 1 row inserted
      K-->>C: 200 OK {applied: true}
    else Already applied
      DB-->>K: Unique/duplicate key violation
      K->>K: Catch and treat as idempotent
      K-->>C: 200 OK {applied: false}
    end
  end
```

### Important notes
- The schema is created on startup, and a few products are seeded if the table is empty.
- For production, configure PostgreSQL by setting `db.embedded=false` and providing connection settings in `application.yaml`.
- The concurrency test (`ProductApiTest.applyDiscount_isIdempotent_underConcurrency`) simulates many simultaneous HTTP PUTs against the same product/discount and asserts only one persisted application.

### Example curl commands

- List products for Sweden:
```bash
curl -s "http://localhost:8080/products?country=Sweden"
```

- Apply a discount idempotently to product `swe-001`:
```bash
curl -s -X PUT "http://localhost:8080/products/swe-001/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId":"BLACKFRIDAY2025","percent":10.0}'
```

Re-running the same PUT with the same `discountId` will return `200 OK` with `{ "applied": false }` after the first successful application.
