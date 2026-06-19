# Smart Basket MCP — PRD & Build Plan

**Version:** 1.0 (spec for approval)
**Date:** 2026-06-19
**Status:** Decisions resolved — ready for coder stage on approval
**Source:** `idea.md`

---

## 1. Summary

Smart Basket MCP is a **memory/intelligence layer** for grocery shopping. It is an
MCP server consumed by any MCP-compatible AI client (Claude, ChatGPT, Gemini,
Cursor, …) and it **orchestrates** the Swiggy Instamart MCP — it never duplicates
Instamart's commerce capabilities.

It adds on top of Instamart: reusable baskets, grocery memory, learned
substitutions, AI-generated baskets, and refill prediction. **The user always
makes the final decision** — the system never auto-orders, auto-pays, or
auto-confirms substitutions.

---

## 2. Dual MCP role (the key architectural fact)

Smart Basket MCP is simultaneously:

- **MCP server** — exposes tools (`create_basket`, `add_basket_to_instamart`, …)
  to AI clients.
- **MCP client** — calls Swiggy Instamart MCP tools (product search, cart
  update, order history, …) to do the actual commerce.

```
AI client (Claude/ChatGPT/…)
  ↓  (MCP)
Smart Basket MCP   ← this project: server + client in one process
  ↓  (MCP)
Swiggy Instamart MCP
  ↓
Instamart cart → checkout (user-driven)
```

---

## 3. Tech stack — REVISED to Java / Spring Boot

Original `idea.md` proposed Node.js + TypeScript. Switching to Java/Spring Boot
per request. This is fully supported:

| Layer | Choice | Why |
|-------|--------|-----|
| Language/Runtime | **Java 21** (LTS) | Records, pattern matching, virtual threads |
| Framework | **Spring Boot 3.x** | Standard, mature |
| MCP | **Spring AI MCP Server + MCP Client starters** | Spring AI ships both; one app plays both roles |
| Transport | **WebMVC SSE** (Spring AI MCP server starter) | Hosted multi-client from start (decided) |
| Persistence | **Spring Data JPA** | Standard data layer |
| DB | **PostgreSQL** from start | `localhost:5432`, db `smart_basket`, user `postgres` (decided) |
| Migrations | **Flyway** | Versioned schema |
| Secrets | **env vars / Spring config** | ⚠️ plaintext `password` is dev-only |
| Build | **Maven** | Spring AI docs default; Gradle also fine |
| Tests | **JUnit 5 + Mockito** | Stdlib of the Spring world |

> Note vs `idea.md`: Drizzle/Prisma (Node ORMs) are replaced by Spring Data JPA.
> Rule-based AI (no LLM) for v1 is **kept** — frequency/recency/quantity heuristics.

---

## 4. Data model

Carried over from `idea.md`, typed for JPA. `id` = UUID string. SQLite-compatible
types; Flyway migration `V1__init.sql`.

- **baskets**(id, user_id, name, created_at) — unique (user_id, name)
- **basket_items**(id, basket_id FK, product_ref, product_name, quantity)
- **substitutions**(id, user_id, preferred_product, fallback_product, priority) —
  ordered fallback chain per preferred product
- **purchase_insights**(id, user_id, product_ref, avg_purchase_gap_days,
  last_purchase_date, purchase_count, confidence_score)

`product_ref` = **exact full Swiggy product name** (decided Q4); re-searched by exact name each run. `user_id` supplied per-request by the AI client (decided Q1).

---

## 5. MCP tools to implement

Server tools (exposed to AI clients), all scoped by `user_id`:

| Tool | Purpose |
|------|---------|
| `create_basket` | Create named basket |
| `get_basket` | Return basket items |
| `update_basket` | Add/remove item (qty) |
| `delete_basket` | Delete basket |
| `list_baskets` | List basket names |
| `save_cart_as_basket` | Snapshot current Instamart cart → basket |
| `learn_substitution` | Store preferred→fallback with priority |
| `get_substitutions` | Return fallback chain for a product |
| `generate_ai_basket` | Build basket from order history (freq/recency) |
| `suggest_refill` | Products likely running low |
| `add_basket_to_instamart` | Resolve → search → availability → substitute → update cart → summary |

Swiggy MCP capabilities **consumed** (not reimplemented): product search,
product discovery, cart retrieval, cart update, order history, frequently
ordered products, checkout.

---

## 6. Core flows

**Add basket to cart** (`add_basket_to_instamart`):
get_basket → for each item: Swiggy search → check availability → if unavailable,
walk substitution chain → Swiggy cart update → return summary (incl. any
substitutions made, flagged to user).

**Refill suggestion** (`suggest_refill`): read order history → update
purchase_insights → flag products where `now - last_purchase >= avg_gap`.

**Adaptive learning**: when a user manually picks a substitute for an
out-of-stock item, record it via `learn_substitution`. **Never invent
preferences** — only learn from explicit user decisions.

---

## 7. Roadmap (phased, ship-per-phase)

1. **Phase 1 — Basket CRUD.** Tools 1–5 + 6 (save_cart). Postgres + Flyway + JPA.
   MCP server over SSE. *No Swiggy dependency yet — uses mock adapter.*
2. **Phase 2 — Swiggy integration.** MCP client wiring; real product search,
   cart update/retrieval behind a `SwiggyGateway` interface (mock ↔ real swap).
3. **Phase 3 — Substitution engine.** Fallback chains + adaptive learning.
4. **Phase 4 — AI basket.** Order-history analysis → `generate_ai_basket`.
5. **Phase 5 — Refill prediction.** Purchase-pattern analysis + confidence.

Every phase ends with a runnable server + tests. Phases 1, 3, 4, 5 are testable
against the mock adapter without Swiggy access.

---

## 8. Non-negotiables

- User approves every order/payment/substitution. No automation past cart
  population.
- `SwiggyGateway` interface from day 1 → mock implementation unblocks all
  development before Swiggy access is granted.
- Input validation on all tool params (trust boundary = AI client input).
- Respect Swiggy's terms: no scraping beyond APIs, no brand-hiding, no
  competitive use.

---

## 9. Success metrics

Technical: basket-creation success rate, cart-population success rate,
substitution accuracy. Business: reduced ordering friction, faster reorder,
retention.

---

## 10. Out of scope (v1)

Voice ordering, WhatsApp, shared/multi-user households, recipe-to-basket, meal
planning, calendar planning, inventory tracking. (Future enhancements list.)

---

## RESOLVED DECISIONS ✅

1. **User identity.** `user_id` supplied **per-request by the AI client**. Schema
   multi-user from day 1; one test user for MVP. ⚠️ Real auth (API key/OAuth)
   required before public exposure — pre-launch security gate, not MVP-blocking.
2. **Swiggy MCP access.** Mock `SwiggyGateway` now; wire real Swiggy MCP in
   Phase 2 once access is granted.
3. **Transport.** Hosted multi-client **SSE** from the start (Spring AI MCP
   server WebMVC SSE starter). stdio dropped.
4. **Product identity.** `product_ref` = **exact full Swiggy product name**;
   re-search by exact name each run.
5. **DB / deploy.** **PostgreSQL from start** — `localhost:5432`, db
   `smart_basket`, user `postgres`, password `password`. ⚠️ plaintext creds are
   dev-only; move to env/secrets before exposing the server beyond localhost.
