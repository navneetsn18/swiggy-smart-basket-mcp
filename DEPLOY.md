# Deploy (AWS)

Hosts the basket/memory/substitution layer. **Swiggy calls are not hosted yet** —
the dev path (`mcp-remote`) needs a browser + a 5-day no-refresh token, which a
headless server can't sustain. Run the `live` profile locally until Swiggy
delegated/on-behalf-of auth is in place (issue #1).

## Build
```bash
docker build -t smart-basket-mcp .
```

## Config (env vars — inject via AWS Secrets Manager / App Runner)
| Var | Purpose |
|-----|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<rds-endpoint>:5432/smart_basket` |
| `SPRING_DATASOURCE_USERNAME` | RDS user |
| `SPRING_DATASOURCE_PASSWORD` | RDS password (Secrets Manager) |
| `SMARTBASKET_API_KEY` | shared API key; clients send `X-API-Key`. Unset = open (local only) |

## AWS stack (recommended)
- **App Runner** — point at this repo/image, port 8080, health check `/sse`.
- **RDS PostgreSQL** (`smart_basket` db) — Flyway migrates on boot.
- **Secrets Manager** — DB creds + API key, injected as the env vars above.
- **Region** ap-south-1.

## Not done (tracked)
- Per-user identity (drop `userId` tool param, derive from caller) — issue #1.
- Hosted Swiggy auth — issue #1 / "decide later".
