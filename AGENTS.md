# AGENTS.md

Guidance for coding agents working in this repository.

## Hard rules

- **Focus on your own work.** If a change you didn't make breaks the build, report it to the user and stop. Don't
  revert, stash, or discard others' changes — that's likely another agent's in-flight work. Pause until the user
  confirms it is safe to continue.
- **Skill routing — always use the matching skill, never freelance:**
  - Implementing a card (from a backlog file like `backlog/sets/scourge/cards.md`, or by name) → `add-card` skill.
    It handles Scryfall lookup, oracle errata, set registration, and the scenario test.
  - Adding an engine/SDK/server/client feature — a new effect, trigger, condition, keyword, decision flow, or any
    capability that isn't a single card → `add-feature` skill. It enforces composition-over-monoliths, designing
    each new SDK type for the *next* card (not just the one in front of you), full cross-layer tracing
    (SDK → engine → projection/triggers → continuations → server DTO → client), and performance + UX review.
- **Verify MTG rule numbers before citing them.** Rule numbers are easy to misremember (613.8 vs 613.7, 704.5 vs
  704.6, …). Before referencing a rule number in code comments, commit messages, PR descriptions, or chat, verify
  it against the official Comprehensive Rules <https://magic.wizards.com/en/rules>: the plain-text `.txt` is too
  large to fetch into context — download it (e.g. `curl -o`) and `grep` it locally. If you can't verify, describe
  the rule by name instead of guessing a number.
- **Keep the SDK reference in sync.** Any SDK addition or change (effect, trigger, condition, keyword, dynamic
  amount, modal shape, replacement effect, …) must update
  [`docs/card-sdk-language-reference.md`](docs/card-sdk-language-reference.md) in the same change.

## Project overview

Argentum Engine — Magic: The Gathering rules engine + online play platform in Kotlin. Pure ECS, immutable
`GameState`, pure functional `(GameState, GameAction) -> ExecutionResult(GameState, List<GameEvent>)`.

**Stack:** Kotlin / JDK 21 / Gradle / Kotest / Spring Boot (backend); React / TypeScript / Zustand / Vite
(frontend). Exact versions: `gradle/libs.versions.toml` and `web-client/package.json` — don't trust remembered
version numbers.

## Module layout

| Module | Purpose | Deps |
|--------|---------|------|
| `mtg-sdk` | DSLs, data models, primitives — pure data, no logic | — |
| `mtg-sets` | Card definitions (Portal, Alpha, Onslaught, …) | sdk |
| `rules-engine` | Core MTG rules (zero server deps) | sdk |
| `ai` | Built-in AI player + draft/deckbuild advisors | engine, sdk |
| `mtg-search` | Scryfall-style search query language over a `SearchCard` projection | — (pure Kotlin) |
| `gym` / `gym-server` / `gym-trainer` | RL/MCTS env + HTTP transport + self-play SPI | engine, sdk |
| `game-server` | Spring Boot orchestration, WebSocket, state masking | engine, sdk |
| `mtgish-tooling` | Predictive coverage / auto-gen analyzer (see below) | — (scans source as text) |
| `web-client` | React UI (dumb terminal — no game logic) | — |

**Key principle:** engine is pure (no card-specific code), content is data-driven (no execution logic), API is an
anti-corruption layer between engine and clients.

## Commands

The `justfile` is the self-documenting command index — run `just --list` (groups: build, dev, e2e, env, ai).
Most used:

```bash
just build | test | test-rules | test-server | test-gym
just test-class CreatureStatsTest      # one test class (rules-engine)
just server | client                   # game server / web client dev (localhost:5173)
just e2e | e2e-test <pattern>          # Playwright suite / one test
scripts/card-status --set BLB          # implemented vs missing cards (--list / --cards for detail;
                                       #   Draft vs Extra = booster-relevant vs completionist)
```

Direct gradle: `./gradlew :rules-engine:test --tests "CreatureStatsTest"` ·
Web client: `cd web-client && npm run dev | build | typecheck`.

**Multi-agent safety:** always verify through the `just test*` / `just build` recipes, not raw `./gradlew`.
Each worktree is a separate Gradle root, so parallel agents otherwise each spawn their own 4g daemon +
4g Kotlin daemon + 2g test JVM and all grab every core — on a normal box three concurrent `./gradlew test`
runs thrash so hard that tests cross the 300s hang-watchdog cap and every agent times out. The `just`
recipes route through `scripts/gradle-locked`, a machine-global lock (`~/.cache/argentum/gradle.lock`) that
serializes heavy Gradle runs across all worktrees — others queue (with a 30m timeout, `GRADLE_LOCK_TIMEOUT`)
instead of competing.

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
- **Prefer atomic pipeline effects** (Gather → Select → Move via the domain pattern objects) over monolithic
  executors for library/zone mechanics. `Effects.kt` holds foundational atomic facades; compositions like Scry,
  Mill, SearchLibrary live in the `*Patterns.kt` objects, reached through the single `Patterns` index
  (`Patterns.Library`, `Patterns.Hand`, `Patterns.Group`, `Patterns.Exile`, `Patterns.CreatureType`,
  `Patterns.Mechanic`).
- **Adding a mechanic** → prefer composing in the relevant `*Patterns.kt` object first; only add a new `Effect`
  type + executor in `rules-engine/handlers/effects/` when atomic primitives don't suffice (that's `add-feature`
  territory — see Hard rules).
- **Reprints:** only a card's *earliest real printing* gets a full `card(...)`; later sets get a `Printing(...)`
  row, never a duplicate canonical. Validate with `just check-card-printing "<Card>"`.

Full DSL catalog (effects, triggers, conditions, filters, costs, keywords, dynamic amounts, …):
[`docs/card-sdk-language-reference.md`](docs/card-sdk-language-reference.md). Architectural reasoning (ECS,
continuations, layer system, mana, priority): [`docs/architecture-principles.md`](docs/architecture-principles.md).

## Testing

- **Unit / integration / scenario tests** — Kotest in `rules-engine` and `game-server`.
- **Card snapshot net** — `CardDefinitionSnapshotTest` (in `mtg-sets`) pins every registered card's compiled JSON
  tree against a committed golden per set, so any SDK change shows up as a reviewable per-card diff across the
  whole corpus. After an *intentional* change, re-bless with
  `./gradlew :mtg-sets:test --tests "*CardDefinitionSnapshotTest" -DupdateSnapshots=true`.
- **Card lint net** — `CardLintTest` (in `mtg-sets`) runs `CardLinter` over every registered card:
  pipeline-variable reads must have writers in scope, `ContextTarget`/`BoundVariable` must resolve
  against the owning ability's requirements, choice-slot reads need declarations. A new SDK type
  that reads/writes a named pipeline variable must be classified in `CardLinter.dataflowFields`
  (the hygiene check fails otherwise). See `card-sdk-language-reference.md` §21.
- **E2E tests** — Playwright in `e2e-scenarios/`, run against the full stack. Patterns, scenario config, and
  `GamePage` helper reference: [`docs/e2e-test-patterns.md`](docs/e2e-test-patterns.md).
- **Manual self-play** — drive a full game over the gym server's HTTP step loop to shake out new-set cards that
  don't behave as printed: [`docs/gym-self-play-testing.md`](docs/gym-self-play-testing.md).

## mtgish coverage + auto-gen tooling

A **predictive, non-authoritative** toolchain (`:mtgish-tooling`) that maps the
[mtgish](https://github.com/i5jb/mtgish) oracle-IR corpus onto our SDK capabilities — for backlog triage and
drafting easy cards. It is never a card loader: ground truth stays a human-authored `cardDef` whose scenario test
passes. Full recipe docs live in the `justfile` comments and [`mtgish-tooling/README.md`](mtgish-tooling/README.md).

```bash
just coverage-dashboard              # interactive TUI over everything below
just coverage --set TMP              # implemented / FREE / blocked + which feature unlocks the most cards
just coverage-gaps --set TMP         # of the unimplemented cards: AUTOGEN / SCAFFOLD / BLOCKED
just coverage-generate --set TMP     # draft .kt for AUTOGEN cards -> mtgish-tooling/generated/<set>/  (staging!)
just coverage-verify --set POR       # the real gate: compile emitted cards + diff capabilities vs golden
just coverage-fixtures --rebless     # re-bless EmitterGoldenTest golden after an intentional emitter change
```

**When to use:** spoiler-season/backlog triage (which feature unlocks the most cards); deciding whether a missing
card is pure authoring vs needs `add-feature`; a blank-page head-start on simple cards.

**Hard rules:**
- Generated `.kt` are **drafts in a staging dir** — they must compile, get a scenario test, and be human-reviewed
  before moving into a set's `cards/` package. `coverage-verify` proves *compile + capabilities*, not behaviour —
  the scenario test is the real gate.
- When auto-gen output is wrong, **fix the emitter, not the generated cards** — render correctly or decline to
  SCAFFOLD tier; never silently emit a lossy approximation.
- Keep using the `add-card` skill for real implementation.

## Documentation index

| Doc | Topic |
|-----|-------|
| [`architecture-principles.md`](docs/architecture-principles.md) | Core design (ECS, continuations, layer system, mana, priority) |
| [`api-guide.md`](docs/api-guide.md) | Adding cards/mechanics step-by-step |
| [`card-sdk-language-reference.md`](docs/card-sdk-language-reference.md) | Full card SDK / DSL reference — update on any SDK change |
| [`continuous-effect-dependency-system.md`](docs/continuous-effect-dependency-system.md) | Rule 613.8 dependency resolution |
| [`managing-complex-and-rare-abilities.md`](docs/managing-complex-and-rare-abilities.md) | Patterns for complex abilities |
| [`engine-server-interface.md`](docs/engine-server-interface.md) | Engine ↔ API contract |
| [`accounts-and-persistence.md`](docs/accounts-and-persistence.md) | Opt-in accounts, magic-link auth, PostgreSQL (saved decks + stats) |
| [`player-input.md`](docs/player-input.md) | Async I/O and decision protocol |
| [`data-contracts.md`](docs/data-contracts.md) | Client/server JSON payloads |
| [`web-client-architecture.md`](docs/web-client-architecture.md) | Frontend architecture, WebSocket API |
| [`e2e-test-patterns.md`](docs/e2e-test-patterns.md) | Playwright fixtures, GamePage helpers, scenario config |
| [`gym-deckbuild-env.md`](docs/gym-deckbuild-env.md) | Sealed deckbuild gym env (build → play pipeline) + custom win-rate reward |
| [`gym-self-play-testing.md`](docs/gym-self-play-testing.md) | Driving the gym server over HTTP to manually self-play and surface broken cards |
