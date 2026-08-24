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
  - Starting a set that has no `backlog/sets/` entry yet → **`create-backlog-for-set`** (Scryfall dump,
    `cards.md` checklist, `definitions/<code>/` scaffold, `mechanics.md`). It runs *before* `set-loop`.
  - Implementing every *Assay-ready* card for a set — the ⚡ badge on the Set Completion view, i.e.
    the cards Argentum Assay reads whole that the set hasn't authored → **`assay-ready-sweep`**
    (`just assay-ready <CODE>` for the four-way split; canonical placement + reprint rows are half
    the job, and the half that gets forgotten).
  - Running the build/test gates and reading the results → **`verify`**.
  - Working autonomously through a whole set, one PR at a time, until it's done → **`set-loop`** (launches
    the harness's own loop — Claude Code `/loop`, Codex `/goal`; every PR it opens is titled
    `[agent-loop: <model-id>]`).
  - Proving a set is *actually* finished once its backlog reads N/N, and archiving it → **`verify-set`**
    (Scryfall field verification of every compiled card, reprint and basic-land coverage, self-play pass).
    A green backlog is a claim; that skill is the proof.
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
| `mtg-sets` | Aggregator — re-exports the whole card corpus; catalog, Scryfall sync, corpus-wide tests | sdk, sets/* |
| `mtg-sets/core` | `CardDiscovery`, token art, the setless `custom/` cards | sdk |
| `mtg-sets/<era>` | Card definitions, one module per fixed release-year range, chained oldest→newest | sdk, sets/core |
| `mtg-sets/<era>/tests` | Card scenario tests for that era's sets | engine, sets |
| `rules-engine` | Core MTG rules (zero server deps) | sdk |
| `ai` | Built-in AI player + draft/deckbuild advisors | engine, sdk |
| `mtg-search` | Scryfall-style search query language over a `SearchCard` projection | — |
| `gym` / `gym-server` / `gym-trainer` | RL/MCTS env + HTTP transport + self-play SPI | engine, sdk |
| `game-server` | Spring Boot orchestration, WebSocket, state masking | engine, sdk |
| `mtgish-tooling` | Predictive coverage / auto-gen analyzer | — (scans source as text) |
| [`oracle-assay`](oracle-assay/README.md) | Argentum Assay — bidirectional Oracle-text parser; touchstone gate (`just assay-gate`), differential gate against the hand-written cards (`just assay-differential`), browser explorer over the live grammar (`just assay-explore`, also framed as a tab on the Set Completion view), the baked whole-card verdict ledger behind that view's "Assay-ready" badges (`just assay-bake`), and the Scenario Builder's dev-gated custom-card sandbox (`just assay compile`) | sdk |
| `web-client` | React UI (dumb terminal — no game logic) | — |

**Key principle:** the engine is pure (no card-specific code), content is data-driven (no execution
logic), and the API is an anti-corruption layer between engine and clients.

**Finding a set's module.** The card corpus and its scenario tests are split by release year so that
no single Kotlin compilation holds all of it — that split is why the build stopped running the
compile daemon out of heap. Every era lives under `mtg-sets/`, cards and tests in one directory:

```
mtg-sets/2003-2007/          :mtg-sets:2003-2007         cards
mtg-sets/2003-2007/tests/    :mtg-sets:2003-2007:tests   scenario tests
```

You never need to memorise which era a set is in:

```bash
just where MRD              # -> mtg-sets/2003-2007/... and its tests/ child
just test-class MyrIncubatorScenarioTest   # finds the file, runs only its module
just test-scenarios 2003-2007              # one era; bare `just test-scenarios` runs all of them
```

Depend on `project(":mtg-sets")` as before — it re-exports every era, so nothing downstream changed.

## Load-bearing rules

These are the ones that have actually caused bugs here.

- **Immutability** — never mutate components in place; return new state.
- **Projected state for battlefield filters** — filtering battlefield permanents by
  type/subtype/color/keywords/P/T MUST read *projected* state, because base state can't see continuous
  effects. `predicateEvaluator.matches(state, projected, entityId, filter, context)` takes `projected` as
  a **required** parameter — pass `state.projectedState` (mid-projection callers such as
  `EffectApplicator` pass their intermediate one instead). Passing it is always correct: entities outside
  the battlefield have no projection entry, so the matchers fall back to base `CardComponent` data on
  their own. The real hazard is reading characteristics off the card directly — always
  `projected.isCreature(entityId)`, never `cardComponent.typeLine.isCreature`.
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
daemons and thrash the box into watchdog timeouts; the `just` recipes limit the machine to two concurrent
builds through a machine-global semaphore. The `verify` skill covers which gate to run for which change
and how to read the results.

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
| [`build-performance-plan.md`](docs/build-performance-plan.md) | Why the card corpus and scenario suite are split into per-era modules; daemon heap and cache tuning |
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
| [`oracle-assay.md`](docs/oracle-assay.md) | Argentum Assay design — first-party Scryfall→SDK Oracle parser; also audits `mtg-sdk` vocabulary. **Phase 1 + the differential gate shipped** ([`:oracle-assay`](oracle-assay/README.md)); the MVP and remaining phases are in [`docs/plans/oracle-assay.md`](docs/plans/oracle-assay.md) |
