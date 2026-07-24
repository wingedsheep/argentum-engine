# e2e-scenarios

Playwright E2E tests against the full stack. Each test calls `createGame(ScenarioRequest)` to POST
to `/api/dev/scenarios` on the running server, then drives two browser contexts (one per player).

## Commands

```bash
npm test                                       # All E2E tests (auto-starts server + client)
npm run test:portal | test:onslaught | test:general
npm run test:ui                                # Playwright UI mode (debug)
npm run test:headed                            # Visible browser
npx playwright test tests/onslaught/sparksmith # Single file
```

## Prerequisites

- `playwright.config.ts`'s `webServer` auto-starts server + client. Set `SKIP_WEB_SERVER=true` to use
  manually-running instances.
- The server needs `GAME_DEV_ENDPOINTS_ENABLED=true` to expose `/api/dev/scenarios`.

## Where to look

- **Patterns, scenario config schema, GamePage helpers, gotchas**:
  [`../docs/e2e-test-patterns.md`](../docs/e2e-test-patterns.md)
- **Client/server JSON payloads**: [`../docs/data-contracts.md`](../docs/data-contracts.md)
- Fixtures and helpers under `fixtures/` and `helpers/` — read directly; names are descriptive.

## Layout

Tests organized by card set: `tests/portal/`, `tests/onslaught/`, `tests/general/`.
