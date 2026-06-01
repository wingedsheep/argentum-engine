# Testing Strategy — what to test where, and how to get there

_Scope: the whole repo's test story — SDK, engine, server, client, and the
self-play/parity tooling. Goal: a single, unambiguous answer to "where does this
test go?" plus a low-risk path from where we are today._

## Bottom line

**The engine is the source of truth, so card and rules behavior is proven in
`rules-engine`.** The server and client are an anti-corruption layer and a dumb
terminal respectively — they get tested for *the things only they do*
(masking, DTO transformation, protocol, rendering, interaction), not for
whether Lightning Bolt deals 3.

Today that principle is ~90% true in practice (455 engine scenario tests) but
muddied by history: 104 scenario tests live in `game-server` even though they
have **zero** game-server dependencies — they're engine tests that happened to
be written against a harness that lives in the server module. And the engine has
*two* setup styles with no shared "nice" builder.

The plan is **not** a big-bang migration. It's: (1) name the convention, (2)
give the engine one nice harness by **relocating** (not duplicating) the good
builder into the engine, (3) let the misplaced tests drift home as they're
touched.

---

## Principles

1. **Test where the behavior lives.** A card is `cardDef { }` data interpreted by
   the engine; the only way to prove it works is to run that data through the real
   `ActionProcessor`. That is an engine test by definition — there is no "card
   logic" layer above the engine to test instead.
2. **Each layer tests only what it uniquely owns.** Re-proving engine behavior at
   the server or E2E layer adds latency and flake without adding signal.
3. **Integration over isolation for cards.** A card test exercises
   data → engine → projection/triggers → result as one unit. That composition *is*
   the unit of value; don't mock it apart.
4. **Most cards need no bespoke test.** If a card only reuses mechanics already
   covered, the mechanic's tests protect it. Write a card scenario test when the
   card introduces or newly combines mechanics. (The `add-card` skill already
   gates this with "only if the card uses NEW effects/…".)
5. **One harness per setup style, not per module.** Duplicated harnesses drift.

---

## What is tested where (target)

| Layer                         | Module / location                                      | What belongs here                                                                                                                                                                                                                                                                                                               | What does **not**                                                                   |
|-------------------------------|--------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| **SDK data**                  | `mtg-sdk` unit tests                                   | A new data type serializes, round-trips, and composes as intended. Pure-data invariants.                                                                                                                                                                                                                                        | Anything requiring a running game.                                                  |
| **Engine unit / integration** | `rules-engine/.../{mechanics,handlers,predicates,...}` | An executor / projector / detector / solver in isolation: construct `GameState` directly, assert on the result. Layer ordering, mana solving, trigger detection paths.                                                                                                                                                          | End-to-end card flows (use a scenario instead).                                     |
| **Engine scenario** ⭐         | `rules-engine/.../engine/scenarios/`                   | **The primary home for card behavior and rules.** A card or mechanic exercised through the real `ActionProcessor` on a realistic board. Every CR rule/ruling you researched gets a paired assertion. Edge cases: fizzle, "may" declined, source leaves, zero/multiple instances, replacement vs trigger order, last-known info. | Frontend concerns; network.                                                         |
| **Server**                    | `game-server`                                          | **Only what the server uniquely does:** state masking (hidden info per viewer), `ClientStateTransformer` DTO shape, session/idempotency (`lastProcessedMessageId`), tournament/lobby orchestration, the engine↔client message protocol.                                                                                         | Card behavior. Rules correctness.                                                   |
| **E2E**                       | `e2e-scenarios/` (Playwright, 53 specs)                | Full-stack player flows: a decision actually renders and is clickable, targeting/priority/stack UX, animations driven by events. The frontend↔engine↔frontend round trip.                                                                                                                                                       | Exhaustive rules coverage (one representative flow per UI surface, not every card). |
| **Self-play / smoke**         | `gym` HTTP step loop                                   | Shake out new-set cards that don't behave as printed by driving full games; surfaces crashes/soft-locks no unit test predicted.                                                                                                                                                                                                 | Deterministic assertions (it's exploratory).                                        |
| **Differential (future)**     | `forge-parity-harness`                                 | Record-and-replay diff vs Forge as oracle for rules divergences.                                                                                                                                                                                                                                                                | (Aspirational — see its own backlog doc.)                                           |

⭐ = where the bulk of card/rules work should land.

### The one-line decision rule

> **Does the assertion depend on the server masking state, transforming a DTO,
> or the browser rendering something?** If no → `rules-engine`. If yes → that
> layer. Cards are almost always "no".

---

## Current state (2026-06)

- **`rules-engine` scenario tests: 455** under `engine/scenarios/`, plus ~65 more
  focused engine tests (520 engine test files total). These use **`GameTestDriver`**
  (in `rules-engine/src/testFixtures`) + **`TestCards.all`**, which registers the
  *entire* `MtgSetCatalog` (every set's cards + basic lands) plus test-only cards.
  This is already the de-facto primary card-test home.
- **`game-server` scenario tests: 104**, all extending **`ScenarioTestBase`**
  (`game-server/.../gameserver/ScenarioTestBase.kt`). Crucially, this base imports
  **only engine classes** (`ActionProcessor`, `GameState`, `ClientStateTransformer`)
  — no Spring, no WebSocket, no `@SpringBootTest`. **These are engine tests living
  in the wrong module** for historical reasons. They are not slower or less pure
  than engine tests; they're just mislocated.
- **Two setup styles, no shared builder:**
    - `GameTestDriver` — *live game*: real `GameInitializer`, decks, advance through
      real turns/priority/mana. Imperative. Per-test `registerCards(...)` boilerplate.
    - `ScenarioTestBase.ScenarioBuilder` — *static board*: fluent
      `scenario().withCardOnBattlefield(2,"X",tapped=true).withLifeTotal(1,5).inPhase(...).build()`,
      then a `TestGame` with name-based `castSpell("Bolt", target)`,
      `declareAttackers(mapOf("Bear" to 2))`, and decision sugar
      (`answerYesNo`, `selectTargets`, `submitDistribution`, …). Far nicer to author,
      but it lives in the server module.
- **Docs guidance already corrected** (committed): `add-feature` and `add-card`
  skills + `docs/RULES.md` now say engine tests are primary and game-server is only
  for frontend↔engine concerns. But the skill **templates still teach the
  server-only `ScenarioTestBase`**, which the engine can't import — a live
  inconsistency.

### Problems to fix

1. The nicest authoring harness lives in the module we're steering tests *away* from.
2. Skill templates point at a harness unavailable in the target module.
3. 104 engine tests are mislocated (low harm, but they model the wrong convention
   for newcomers and contradict game-server's own "no game logic" charter).

---

## Target state

- **One canonical scenario harness in `rules-engine/src/testFixtures`**, offering
  *both* setup styles against the real engine:
    - live-game flow (today's `GameTestDriver`), and
    - the fluent static-board builder + name-based action/decision API (today's
      `ScenarioBuilder`/`TestGame`), backed by `TestCards.all`.
- **`game-server` consumes that harness** via `testFixtures(project(":rules-engine"))`;
  its own `ScenarioTestBase` becomes a one-line shim (or is deleted once empty), so
  the truly server-level tests (masking/DTO/protocol/tournament) keep a home.
- **Skill templates teach the engine harness**, so every new card/feature test
  lands in `rules-engine`.
- **Misplaced tests migrate on touch**, not in a risky big bang.

Acceptable end state: two *setup styles* coexist in one module — they're
complementary (live-flow vs static-board), not redundant. Fully converging them
into a single entry point is optional polish, not required.

---

## How we get there (phased, low-risk)

### Phase 0 — Guidance (DONE)

- `add-feature`/`add-card` skills + `docs/RULES.md` state engine-primary; game-server
  only for frontend↔engine. ✅ (committed `2e2a971c4`)

### Phase 1 — Relocate the nice harness into the engine

**The keystone. Relocation, not duplication.**

1. Move `ScenarioTestBase` (the `ScenarioBuilder` + `TestGame`) into
   `rules-engine/src/testFixtures/.../support/ScenarioTestBase.kt`.
2. Replace its hand-maintained ~60-set `register(...)` list with `TestCards.all`
   (the full `MtgSetCatalog` — strictly a superset, so nothing un-registers).
3. It uses only engine classes already, so no dependency surgery is needed inside it.
4. Wire `game-server` to see it: add
   `testImplementation(testFixtures(project(":rules-engine")))` to
   `game-server/build.gradle.kts`.
5. Collapse game-server's `ScenarioTestBase` to a shim so the 104 existing tests
   compile untouched:
   ```kotlin
   abstract class ScenarioTestBase : com.wingedsheep.engine.support.ScenarioTestBase()
   ```
6. `just test-rules` + `just test-server` green.

_Outcome: engine gains the fluent builder immediately; one harness, no duplication;
zero churn on existing game-server tests._

### Phase 2 — Point the skills at the engine harness

- Update `add-card` `examples.md` scenario template + the `add-feature`
  "`ScenarioTestBase` scope" bullet to teach the engine harness
  (`com.wingedsheep.engine.support.ScenarioTestBase`, package
  `com.wingedsheep.engine.scenarios`). Resolves the template inconsistency Phase 0
  left open.
- Document both styles in one place (probably `docs/` or a testFixtures KDoc):
  "live-game (`GameTestDriver`) vs static-board (`scenario { }`) — when to use which."

### Phase 3 — Migrate misplaced tests *on touch* (no big bang)

- Rule: when you edit a `game-server` scenario test that has no server dependency,
  move it to `rules-engine/.../engine/scenarios/` (package + import swap; the
  superclass name is unchanged thanks to the shim, so it's nearly verbatim).
- Optionally do a one-time sweep later if/when game-server's scenario folder is
  mostly empty and the shim can be deleted. Track remaining server-level tests
  (masking/DTO/tournament) — those *stay* and justify keeping the folder.
- **Do not** schedule a mass relocation as a milestone; the payoff (organizational
  tidiness) doesn't justify 90+ simultaneous moves and their regression risk.

### Phase 4 (optional) — Converge setup styles

- If the two-style split causes friction, fold the static-board builder into
  `GameTestDriver` as an alternate setup path that returns the same driver, so
  there's one action/query/assertion vocabulary with two ways to seed the board.
- Pure polish; only do it if authors actually trip over the split.

---

## Authoring quick-reference (target)

**A card / rules behavior** → `rules-engine/.../engine/scenarios/FooTest.kt`:

- Static board, exact setup: `scenario().withCardOnBattlefield(...).build()` then
  name-based `castSpell` / `declareAttackers` / decision sugar.
- Needs real turn/priority/mana flow: `GameTestDriver` + `initGame/initMirrorMatch`,
  `passPriorityUntil(...)`, `put*OnBattlefield`, submit actions.
- Assert via `StateProjector().project(state)` for static-ability/projection checks,
  or the harness's life/zone/stack queries.

**A new SDK type** → `mtg-sdk` round-trip/unit test (serializes, composes).

**Server-only behavior** (masking, DTO shape, idempotency, tournament) → `game-server`.

**A player-facing flow** (decision renders & is clickable, targeting UX) →
`e2e-scenarios/` Playwright.

**"Does this new set actually play?"** → gym HTTP self-play loop (exploratory).

---

## Decisions / open questions

- **Naming on relocation:** keep the engine class named `ScenarioTestBase` (so the
  game-server shim and migrated tests need no rename) vs. a fresh name like
  `EngineScenarioTest` (clearer, but forces import churn). _Leaning: keep the name._
- **Delete the game-server shim eventually?** Only once no genuine server-level
  scenario test remains. Likely a few masking/tournament tests justify keeping it.
- **`TestCards.all` cost:** registering the full catalog per spec is already what
  455 engine tests do; if registry build time ever shows up in profiles, cache a
  shared registry across the spec (it's immutable input).
- **Convergence (Phase 4):** worth it only if the two-style split generates real
  confusion. Revisit after Phases 1–3 settle.
