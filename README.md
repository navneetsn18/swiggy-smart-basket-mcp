# Smart Basket MCP

**An AI-native grocery memory layer for Swiggy Instamart.**

Smart Basket MCP is an [MCP](https://modelcontextprotocol.io) server that sits
on top of the **Swiggy Instamart MCP** and gives AI assistants a persistent
grocery memory: reusable baskets, learned substitutions, AI-generated baskets,
and refill prediction.

It does **not** duplicate any Instamart capability. It **orchestrates** them —
Instamart handles commerce (search, cart, checkout, order history), Smart Basket
handles memory, preferences, and prediction.

> **User stays in control.** Smart Basket never places orders, never completes
> payments, and never confirms substitutions automatically. It only populates
> the cart and presents a summary — every final decision belongs to the user.

---

## Why

Most grocery shopping is repetitive — the same milk, eggs, bread, curd every few
days — yet grocery apps don't effectively learn what a household buys together,
how often, which brands, or which substitutes are acceptable. Smart Basket adds
that memory so a user can simply say:

- *"Add my Dairy Basket"*
- *"What am I likely running out of?"*
- *"Order what my mom usually buys"*
- *"Create a basket with 2 milk, 10 eggs, 1 curd"*

…and the AI completes the work against Instamart, pausing for the user to
approve.

---

## Architecture

Smart Basket MCP plays **two MCP roles in a single process**:

- an **MCP server** exposing basket/memory tools to AI clients (Claude, ChatGPT,
  Gemini, Cursor, …), and
- an **MCP client** that calls the Swiggy Instamart MCP for the actual commerce.

```
User
  ↓
AI client (Claude / ChatGPT / Gemini / …)
  ↓  MCP
Smart Basket MCP        ← this project (server + client)
  ↓  MCP
Swiggy Instamart MCP
  ↓
Instamart cart → checkout (user-approved)
```

---

## Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | **User baskets** | Create reusable baskets (Dairy, Breakfast, Monthly, …) |
| 2 | **Save cart as basket** | Snapshot the current Instamart cart for reuse |
| 3 | **AI basket** | Auto-build a basket from order history (frequency/recency) |
| 4 | **Learned substitutions** | Ordered fallback chain when a product is unavailable |
| 5 | **Adaptive learning** | Learns substitutes **only** from explicit user choices — never invents preferences |
| 6 | **Smart refill prediction** | Flags products likely running low from purchase patterns |
| 7 | **Household shopping memory** | Preferred brands, quantities, groups, substitutes |

---

## MCP tools

All tools are scoped to a `user_id` supplied per request by the AI client.

| Tool | Purpose |
|------|---------|
| `create_basket` | Create a named basket |
| `get_basket` | Return basket items |
| `update_basket` | Add/remove an item |
| `delete_basket` | Delete a basket |
| `list_baskets` | List basket names |
| `save_cart_as_basket` | Snapshot the current Instamart cart |
| `learn_substitution` | Store a preferred→fallback substitution |
| `get_substitutions` | Return the fallback chain for a product |
| `generate_ai_basket` | Build a basket from order history |
| `suggest_refill` | Suggest products likely running low |
| `add_basket_to_instamart` | Resolve → search → check availability → substitute → update cart → summary |

**Swiggy Instamart MCP capabilities consumed** (never reimplemented): product
search & discovery, cart retrieval & update, order history, frequently ordered
products, checkout.

---

## Tech stack

- **Java 21** + **Spring Boot 3**
- **Spring AI** — MCP server (SSE) + MCP client starters
- **PostgreSQL** (Spring Data JPA + Flyway migrations)
- Recommendation logic is **rule-based** (frequency / recency / quantity
  consistency) — **no LLM required** in v1.

---

## Data model

- `baskets` (id, user_id, name, created_at)
- `basket_items` (id, basket_id, product_ref, product_name, quantity)
- `substitutions` (id, user_id, preferred_product, fallback_product, priority)
- `purchase_insights` (id, user_id, product_ref, avg_purchase_gap_days,
  last_purchase_date, purchase_count, confidence_score)

`product_ref` is the **exact full Swiggy product name**, re-resolved against
Swiggy search on each run.

---

## Roadmap

1. **Basket CRUD** — tools 1–6, Postgres, MCP server over SSE (mock Swiggy adapter)
2. **Swiggy integration** — wire the real Swiggy Instamart MCP behind the gateway
3. **Substitution engine** — fallback chains + adaptive learning
4. **AI basket** — order-history analysis
5. **Refill prediction** — purchase patterns + confidence scoring

A `SwiggyGateway` interface (mock + real implementations) means most phases are
built and tested before Swiggy access is granted, then switched over in Phase 2.

---

## Compliance with Swiggy MCP terms

- **Orchestrates, never duplicates** Instamart capabilities.
- **No automation past cart population** — no auto-order, auto-pay, or
  auto-substitution.
- **No scraping** beyond the provided APIs; **no competitive intelligence** use.
- **No brand-hiding** — users always know they're interacting with Swiggy
  Instamart.
- Transaction data from Swiggy MCP stays governed by Swiggy's platform terms.

---

## Status

Project proposal / early development. Built to request Swiggy Instamart MCP
access. Until access is granted, development runs against a mock Swiggy gateway.
