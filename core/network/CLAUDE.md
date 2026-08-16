# core/network

**Purpose:** The wire contract between the embedded server and any client that talks to
it — route path constants and `@Serializable` DTOs. No HTTP client, no server code, no
logic; just shapes and strings.

**Gradle:** `:core:network` · `diva.kmp.library` + kotlinx-serialization
**Depends on:** `:core:model`
**Depended on by:** `:server` and `:composeApp`

## Key files

`src/commonMain/kotlin/com/divafinance/core/network/`

| File | What it holds |
|------|---------------|
| `ApiRoutes.kt` | Every path the server serves, composed from `BASE = "/api"`: auth (`/auth/login`, `/auth/logout`), cards (incl. `/api/cards/{id}/rewards` and `/api/cards/best/{category}`), transactions, accounts, graphs (`/spending`, `/thresholds`), `dashboard`, `feed`, backup (`/export`, `/import`) |
| `dto/CardDto.kt` | Card + reward-rule payloads |
| `dto/TransactionDto.kt` | Transaction payloads |
| `dto/DashboardDto.kt` | `DashboardDto` (totals, counts, recent transactions, category breakdown) and `CategorySpendingDto` |
| `dto/AuthDto.kt` | `AuthRequest` / `AuthResponse` (the response carries a `sessionToken`) |

## Conventions / gotchas

- **Route strings live here, not in `:server`.** `server/src/commonMain/.../routing/`
  should reference `ApiRoutes` constants so a path can't drift between the two sides.
- Path parameters use Ktor's `{id}` / `{category}` syntax, so the constants are usable
  directly in `get(ApiRoutes.CARD_BY_ID)`.
- **Known duplication:** `DashboardDto.kt` also declares `AuthRequestDto` /
  `AuthResponseDto`, which overlap with `AuthRequest` / `AuthResponse` in `AuthDto.kt`.
  The `AuthDto.kt` pair is the one the server uses. Prefer it; don't add a third.
- DTOs are separate from `core:model` on purpose — the wire format is allowed to lag
  behind or flatten the domain model. Convert at the route handler.

## Tests

None. The contract is exercised by `server/src/androidUnitTest/.../DivaServerTest.kt`.
