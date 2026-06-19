# Phase 1 — Basket CRUD (coder stage)

**Branch:** `phase-1-basket-crud`
**Status:** Implemented, tested, boots clean against local Postgres.

## What was built

Spring Boot 3.4 + Java 21 + Spring AI 1.0.0 MCP server (SSE/WebMVC) over
PostgreSQL. A `SwiggyGateway` interface with a mock implementation stands in for
Swiggy Instamart MCP until access lands.

### Files
- `pom.xml` — Spring Boot parent, Spring AI BOM, MCP server (webmvc), JPA,
  Postgres driver, Flyway (+ postgres module), test starter.
- `src/main/resources/application.yml` — datasource `localhost:5432/smart_basket`,
  `ddl-auto: validate`, Flyway on, MCP server config (SSE endpoint `/mcp/messages`).
  Password reads `SPRING_DATASOURCE_PASSWORD` env, defaults to dev `password`.
- `src/main/resources/db/migration/V1__init.sql` — all 4 tables (baskets,
  basket_items, substitutions, purchase_insights). Latter two created now, used
  in later phases.
- `domain/Basket.java`, `domain/BasketItem.java` — JPA entities. `Basket` owns
  add/remove/merge logic. `product_ref == exact full product name` (spec Q4).
- `domain/BasketRepository.java` — Spring Data JPA.
- `basket/BasketService.java` — CRUD + `save_cart_as_basket`, input validation.
- `swiggy/SwiggyGateway.java` + `swiggy/MockSwiggyGateway.java` — boundary +
  fixed sample cart.
- `tools/BasketTools.java` — `@Tool` methods → MCP tools.
- `SmartBasketApplication.java` — registers tools via `MethodToolCallbackProvider`.

### MCP tools live
`create_basket`, `get_basket`, `update_basket`, `delete_basket`,
`list_baskets`, `save_cart_as_basket`. (`user_id` per-request, spec Q1.)

## Verification
- `mvn test` → **5/5 pass** (`BasketServiceTest`: duplicate reject, blank reject,
  add-merge, remove-missing, save-cart mapping).
- App boots: Flyway applied V1; JPA `validate` passed (entities match schema);
  Tomcat 8080; `GET /sse` → 200; 4 tables + `flyway_schema_history` present in
  Postgres.

## Carried forward (not in Phase 1)
- Real Swiggy MCP client behind `SwiggyGateway` (Phase 2).
- Substitution engine, AI basket, refill prediction (Phases 3–5).
- ⚠️ Auth before public exposure; move DB password to env/secrets.
