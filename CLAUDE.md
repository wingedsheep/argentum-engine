# CLAUDE.md

Guidance for Claude Code working in this repository.

## Collaboration rules

- **Focus on your own work.** If a change you didn't make breaks the build, report it to the user and stop. Don't
  revert, stash, or discard others' changes — that's likely another agent's in-flight work. Pause until the user
  confirms it is safe to continue.
- **Implementing a card from a backlog file** (e.g., `backlog/sets/scourge/cards.md`) → always use the `add-card` skill.

## Project Overview

Argentum Engine — Magic: The Gathering rules engine + online play platform in Kotlin. Pure ECS, immutable `GameState`,
pure functional `(GameState, GameAction) -> ExecutionResult(GameState, List<GameEvent>)`.

**Stack:** Kotlin 2.3.20 / JDK 21 / Gradle 8.14 / Kotest 6.1.5 / Spring Boot 4.0.4 (backend); React 19.2 / TS 5.9 /
Zustand 5.0 / Vite 8.0 (frontend).

## Build Commands

```bash
just build                          # Build entire project
just test | test-rules | test-server
just test-class CreatureStatsTest   # Specific test class
just server | client                # Run game server / web client dev
```

Direct gradle: `./gradlew :rules-engine:test --tests "CreatureStatsTest"` ·
Web client: `cd web-client && npm run dev | build | typecheck` (dev at localhost:5173).

## Module Layout

| Module | Purpose | Deps |
|--------|---------|------|
| `mtg-sdk` | DSLs, data models, primitives — pure data, no logic | — |
| `mtg-sets` | Card definitions (Portal, Alpha, Onslaught, ...) | sdk |
| `rules-engine` | Core MTG rules (zero server deps) | sdk |
| `gym` / `gym-server` / `gym-trainer` | RL/MCTS env + HTTP transport + self-play SPI | engine, sdk |
| `game-server` | Spring Boot orchestration, WebSocket, state masking | engine, sdk |
| `web-client` | React UI (dumb terminal — no game logic) | — |

**Key principle:** engine is pure (no card-specific code), content is data-driven (no execution logic), API is an
anti-corruption layer between engine and clients.

## Load-bearing rules

- **Immutability:** never mutate components in place — always return new state.
- **Projected state for battlefield filters:** filtering battlefield permanents by type/subtype/color/keywords/P/T
  MUST use `predicateEvaluator.matchesWithProjection(state, projected, ...)`, not `.matches(...)`. Same for
  `cardComponent.typeLine.isCreature` → use `projected.isCreature(entityId)`. Non-battlefield zones (hand, library,
  graveyard, stack) can use base state.
- **Layer dependencies (Rule 613.8):** sort effects in the same layer by trial application before falling back to
  timestamp.
- **Events, not silent mutations:** every state change emits a `GameEvent` so triggers and animations can react.
- **Server is authoritative:** never compute legal actions in the client — the server sends them.

## Card / effect authoring

- **Cards are data:** define via `cardDef { }` DSL, not class inheritance. Register in the set file
  (`definitions/{set}/{Set}Set.kt`) — the engine auto-loads via `ServiceLoader`.
- **Use the `Effects.*` facade** (e.g., `Effects.DrawCards(1)`, `Effects.Destroy()`), not raw constructors.
- **Prefer atomic pipeline effects** (Gather → Select → Move via `EffectPatterns`) over monolithic executors for
  library/zone mechanics. `Effects.kt` holds foundational atomic facades; `EffectPatterns.kt` holds compositions like
  Scry, Mill, SearchLibrary.
- **Adding a card** → use the `add-card` skill (handles Scryfall lookup, oracle errata, set registration,
  scenario test).
- **Adding a mechanic** → prefer composing in `EffectPatterns.kt` first; only add a new `Effect` type + executor in
  `rules-engine/handlers/effects/` when atomic primitives don't suffice.

Detailed DSL reference: [`docs/card-definition-guide.md`](docs/card-definition-guide.md). Architectural reasoning
(ECS, continuations, layer system, mana, priority): [`docs/architecture-principles.md`](docs/architecture-principles.md).

## Testing

- **Unit / integration / scenario tests** — Kotest in `rules-engine` and `game-server`.
- **E2E tests** — Playwright in `e2e-scenarios/`, run against full stack. Patterns, scenario config, and `GamePage`
  helper reference: [`docs/e2e-test-patterns.md`](docs/e2e-test-patterns.md).
- Run: `just test` · `just test-rules` · `just test-class <Name>`.

## Documentation index

| Doc | Topic |
|-----|-------|
| [`architecture-principles.md`](docs/architecture-principles.md) | Core design (ECS, continuations, layer system, mana, priority) |
| [`api-guide.md`](docs/api-guide.md) | Adding cards/mechanics step-by-step |
| [`card-definition-guide.md`](docs/card-definition-guide.md) | Full card DSL reference |
| [`continuous-effect-dependency-system.md`](docs/continuous-effect-dependency-system.md) | Rule 613.8 dependency resolution |
| [`managing-complex-and-rare-abilities.md`](docs/managing-complex-and-rare-abilities.md) | Patterns for complex abilities |
| [`engine-server-interface.md`](docs/engine-server-interface.md) | Engine ↔ API contract |
| [`player-input.md`](docs/player-input.md) | Async I/O and decision protocol |
| [`data-contracts.md`](docs/data-contracts.md) | Client/server JSON payloads |
| [`web-client-architecture.md`](docs/web-client-architecture.md) | Frontend architecture, WebSocket API |
| [`e2e-test-patterns.md`](docs/e2e-test-patterns.md) | Playwright fixtures, GamePage helpers, scenario config |
