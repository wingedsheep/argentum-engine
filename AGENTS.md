# AGENTS.md

Argentum Engine — Magic: The Gathering rules engine + online play platform in Kotlin. Pure ECS,
immutable `GameState`, pure functional `(GameState, GameAction) -> ExecutionResult(GameState, List<GameEvent>)`.

**Stack:** Kotlin / JDK 21 / Gradle / Kotest / Spring Boot; React / TypeScript / Zustand / Vite. Exact
versions live in `gradle/libs.versions.toml` and `web-client/package.json` — don't trust remembered ones.

This file is a map plus the gotchas that aren't discoverable from the code. Depth lives in the skills and
docs it points at; load those when the work needs them.

## Hard rules

- **Focus on your own work.** If a change you didn't make breaks the build, report it and stop. Never
  revert, stash, or discard others' changes — that's likely another agent's in-flight work. Pause until
  the user confirms it's safe to continue. If the user confirms or explicitly asks for a PR despite the
  unrelated failure, opening the PR is allowed; disclose the failure and the verification that did pass
  in the PR body.
- **Route to the matching skill, don't freelance:**
  - Implementing a card — or a batch of them — from a backlog file or by name → **`add-card`** (Scryfall
    lookup, oracle errata, canonical-printing placement, scenario test).
  - Any engine/SDK/server/client capability that isn't a single card — effect, trigger, condition,
    keyword, decision flow → **`add-feature`** (composition-first design, cross-layer tracing, perf + UX).
  - Running the build/test gates and reading the results → **`verify`**.
  - Working autonomously through a whole set, one PR at a time, until it's done → **`set-loop`** (launches
    the harness's own loop — Claude Code `/loop`, Codex `/goal`; every PR it opens is titled
    `[agent-loop: <model-id>]`).
- **Verify MTG rule numbers before citing them** in code, comments, commit messages, PR bodies, or chat.
  613.8 vs 613.7 and 704.5 vs 704.6 are easy to swap. Check the official Comprehensive Rules
  <https://magic.wizards.com/en/rules> — the plain-text `.txt` is too large to fetch into context, so
  `curl -o` it and `grep` locally. If you can't verify, describe the rule by name instead of guessing.
- **One card, one test file — never batch cards into a shared test.** A scenario test covers exactly one
  card: `<CardName>ScenarioTest.kt` holding that card's tests. Implementing five cards means five test
  files, not one `FooBatchScenarioTest`. Batched files hide which card a failure belongs to, make
  `just test-class` useless for a single card, and turn every later edit into a merge conflict between
  agents. Engine-level tests (a mechanic, a replacement effect) are the exception — those are named for
  the mechanic and may exercise several cards. This bans the shared test *file*, not the shared *PR*:
  bundling several cards that all compose existing primitives into one PR is the house shape, and
  `CONTRIBUTING.md` encourages it. A card needing new engine vocabulary still gets a PR of its own.
- **Keep [`docs/card-sdk-language-reference.md`](docs/card-sdk-language-reference.md) in sync.** Every SDK
  addition or change — effect, trigger, condition, keyword, dynamic amount, modal shape, replacement
  effect — updates it in the *same* change. It's the canonical catalog; drift makes it useless.

## Module layout

| Module | Purpose | Deps |
|--------|---------|------|
| `mtg-sdk` | DSLs, data models, primitives — pure data, no logic | — |
| `mtg-sets` | Card definitions (Portal, Alpha, Onslaught, …) | sdk |
| `rules-engine` | Core MTG rules (zero server deps) | sdk |
| `ai` | Built-in AI player + draft/deckbuild advisors | engine, sdk |
| `mtg-search` | Scryfall-style search query language over a `SearchCard` projection | — |
| `gym` / `gym-server` / `gym-trainer` | RL/MCTS env + HTTP transport + self-play SPI | engine, sdk |
| `game-server` | Spring Boot orchestration, WebSocket, state masking | engine, sdk |
| `mtgish-tooling` | Predictive coverage / auto-gen analyzer | — (scans source as text) |
| `web-client` | React UI (dumb terminal — no game logic) | — |

**Key principle:** the engine is pure (no card-specific code), content is data-driven (no execution
logic), and the API is an anti-corruption layer between engine and clients.

## Load-bearing rules

These are the ones that have actually caused bugs here.

- **Immutability** — never mutate components in place; return new state.
- **Projected state for battlefield filters** — filtering battlefield permanents by
  type/subtype/color/keywords/P/T MUST use `predicateEvaluator.matchesWithProjection(state, projected, …)`,
  not `.matches(…)`; `cardComponent.typeLine.isCreature` → `projected.isCreature(entityId)`. Non-battlefield
  zones (hand, library, graveyard, stack) can read base state.
- **Layer dependencies (Rule 613.8)** — sort same-layer effects by trial application before falling back
  to timestamp. Never `toMutableSet()` a `ContinuousEffect` list; it dedupes equal lord effects.
- **Events, not silent mutations** — every state change emits a `GameEvent` so triggers and animations
  can react.
- **Server is authoritative** — never compute legal actions in the client; the server sends them.
- **Last-known information** — dies/leaves triggers read `triggerLastKnownPower`,
  `lastKnownCardDefinitionId`, and `lastKnownCounters` off the `ZoneChangeEvent`; the entity is already
  gone when the trigger resolves.

## Card / effect authoring

- **Cards are data** — `cardDef { }` DSL, never class inheritance. No registration step: `CardDiscovery`
  scans `definitions/{set}/cards/` for top-level `val`s, so a card in the right package is picked up
  automatically. A new *set* only needs an `MtgSet` object under `definitions/` — `MtgSetCatalog` finds it
  the same way.
- **Facades, not raw constructors** — `Effects.DrawCards(1)`, `Effects.Destroy()`. `FacadeBoundaryTest`
  enforces this for cards. Compositions (Scry, Mill, SearchLibrary) live behind the single `Patterns`
  index: `Patterns.Library`, `.Hand`, `.Group`, `.Exile`, `.CreatureType`, `.Mechanic`, `.Sideboard`.
- **Composition before new types** — a new `Effect` + executor is the last resort, and it's `add-feature`
  territory. The bar and the reusability rules: [`docs/sdk-design-principles.md`](docs/sdk-design-principles.md).
- **Reprints** — only a card's *earliest real printing* gets a full `card(...)`; later sets get a
  `Printing(...)` row, never a duplicate canonical. `just check-card-printing "<Card>"` is the gate.

## Commands

`just --list` is the self-documenting index (groups: build, dev, e2e, env, ai).

**Always run heavy builds through `just`, never raw `./gradlew`.** Parallel agents each spawn their own
daemons and thrash the box into watchdog timeouts; the `just` recipes serialize through a machine-global
lock. The `verify` skill covers which gate to run for which change and how to read the results.

## mtgish coverage + auto-gen tooling

`:mtgish-tooling` maps the [mtgish](https://github.com/i5jb/mtgish) oracle-IR corpus onto our SDK
capabilities — for backlog triage ("which feature unlocks the most cards?") and blank-page drafts of easy
cards. `just coverage-dashboard` is the TUI over it; recipe docs live in the `justfile` comments and
[`mtgish-tooling/README.md`](mtgish-tooling/README.md).

It is **predictive and non-authoritative — never a card loader.** Two rules follow from that:

- Generated `.kt` are drafts in a staging dir. `coverage-verify` proves *compile + capabilities*, not
  behaviour — a human-reviewed `cardDef` with a passing scenario test is the only ground truth.
- When output is wrong, **fix the emitter, not the generated card.** Render correctly or decline to the
  SCAFFOLD tier; never silently emit a lossy approximation.

## Documentation index

| Doc | Topic |
|-----|-------|
| [`architecture-principles.md`](docs/architecture-principles.md) | Core design (ECS, continuations, layer system, mana, priority) |
| [`sdk-design-principles.md`](docs/sdk-design-principles.md) | The bar for a new SDK type: composition, reusability, naming |
| [`card-sdk-language-reference.md`](docs/card-sdk-language-reference.md) | Full card SDK / DSL catalog — update on any SDK change |
| [`api-guide.md`](docs/api-guide.md) | Adding cards/mechanics step-by-step |
| [`continuous-effect-dependency-system.md`](docs/continuous-effect-dependency-system.md) | Rule 613.8 dependency resolution |
| [`managing-complex-and-rare-abilities.md`](docs/managing-complex-and-rare-abilities.md) | Patterns for complex abilities |
| [`engine-server-interface.md`](docs/engine-server-interface.md) | Engine ↔ API contract |
| [`accounts-and-persistence.md`](docs/accounts-and-persistence.md) | Opt-in accounts, magic-link auth, PostgreSQL |
| [`player-input.md`](docs/player-input.md) | Async I/O and decision protocol |
| [`data-contracts.md`](docs/data-contracts.md) | Client/server JSON payloads |
| [`web-client-architecture.md`](docs/web-client-architecture.md) | Frontend architecture, WebSocket API |
| [`e2e-test-patterns.md`](docs/e2e-test-patterns.md) | Playwright fixtures, GamePage helpers, scenario config |
| [`gym-deckbuild-env.md`](docs/gym-deckbuild-env.md) | Sealed deckbuild gym env + custom win-rate reward |
| [`gym-self-play-testing.md`](docs/gym-self-play-testing.md) | Driving the gym server over HTTP to surface broken cards |
| [`agent-loops/`](docs/agent-loops/) | Long-running set-implementation prompts for Claude Code `/loop` and Codex `/goal` |
