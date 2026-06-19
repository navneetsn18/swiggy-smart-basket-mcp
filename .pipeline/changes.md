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
- AI basket, refill prediction (Phases 4–5).
- ⚠️ Auth before public exposure (issue #1); DB password via .env (done).

---

# Phase 2 + 3 — Swiggy integration + substitutions

**Branch:** `phase-2-3-swiggy-substitutions`

## Phase 3 (substitutions) — verified on mock
- `domain/Substitution`, `SubstitutionRepository`, `substitution/SubstitutionService`
  (learn = append fallback at next priority, idempotent, never invents).
- `basket/FulfillmentService` + tool `add_basket_to_instamart`: per item, search via
  gateway; if out of stock, walk the learned fallback chain; update cart; return
  summary (added / substitutions applied / unavailable). Never checks out.
- Tools `learn_substitution`, `get_substitutions`.
- Tests: `SubstitutionServiceTest` (3), `FulfillmentServiceTest` (2). 10/10 total green.

## Phase 2 (real Swiggy) — wired, not live-verified
- `SwiggyGateway` widened: `searchProduct`, `getCart`, `updateCart`.
- `MockSwiggyGateway` `@Profile("!live")` (default); `RealSwiggyGateway` `@Profile("live")`.
- Live connects via **stdio → `npx mcp-remote https://mcp.swiggy.com/im`** (Spring AI
  1.0.0 / MCP SDK 0.10.0 has no streamable-HTTP client; mcp-remote bridges it and
  handles the OAuth browser flow + token cache).
- `application-live.yml` enables the MCP client only under `live`; default keeps it off.
- ⚠️ Swiggy response JSON field names in `RealSwiggyGateway` are best-effort — confirm
  against the first real call. Needs Node/npx + a one-time browser login.

## To go live
1. `npm i -g npm` not needed; ensure Node/npx installed.
2. (optional) set `SWIGGY_ADDRESS_ID` in `.env`.
3. Run with `--spring.profiles.active=live`; approve Swiggy login in the browser.
