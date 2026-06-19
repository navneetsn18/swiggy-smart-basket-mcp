# Swiggy MCP Access Application — Smart Basket MCP

**Team / Project Name:** Smart Basket MCP

**What are you building?**
An MCP server that adds a persistent grocery-memory layer on top of Swiggy
Instamart MCP — reusable baskets, learned substitutions, AI-generated baskets,
and refill prediction. It orchestrates Instamart's existing commerce tools
(product search, cart, order history) and never auto-orders or auto-pays — the
user makes every final decision (cart population only).

**Integration type:** AI Agent / Copilot (an MCP server consumed by AI clients
such as Claude, ChatGPT, Gemini).

**Tech stack & architecture:**
Java 21 + Spring Boot 3 + Spring AI. The service plays two MCP roles in one
process: an **MCP server** (SSE transport) exposing basket/memory tools to AI
clients, and an **MCP client** that calls Swiggy Instamart MCP for product
search, cart update/retrieval, and order history. PostgreSQL for persistence
(baskets, substitutions, purchase insights). Recommendation logic is rule-based
(frequency/recency) — no LLM. Smart Basket never duplicates Instamart
capabilities; it only orchestrates them.

**Redirect URI(s) for auth flows:**
⚠️ NEEDS YOUR INPUT — depends on hosting domain. Dev placeholder:
`http://localhost:8080/login/oauth2/code/swiggy`. Production URI TBD once domain
is chosen.

**Expected request volume:** < 1K/day (MVP / development).

**Demo link / GitHub repo:** In development — no public repo yet; can share on
request.

---

## Fields the form may also ask (from idea.md) — NEED YOUR INPUT
- Static IP range(s) / gateway IP — unknown until deploy target chosen.
- Security contact — your email/name.
- Data handling & privacy declaration — Swiggy transaction data stays governed
  by Swiggy terms; baskets/preferences stored in our Postgres; no scraping
  beyond APIs; no competitive use.
- SOC2 / ISO — N/A (individual developer / early stage).
