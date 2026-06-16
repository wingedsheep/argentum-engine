---
name: add-feature
description: Adds a new feature or mechanic to the Argentum Engine (SDK primitive, effect, trigger, condition, static/replacement ability, server/client capability) following the project's architecture and SDK-elegance principles. Use when implementing engine/SDK/server/client functionality that isn't a single card — e.g. "add an effect type", "support a new keyword", "add a decision UI", "make the engine handle X".
argument-hint: <feature description>
---

# Add Engine Feature

Implement the feature described in `$ARGUMENTS`. This skill is for work that extends the
**engine, SDK, server, or client** — a new primitive, mechanic, decision flow, or capability —
as opposed to a single card (use `add-card` for that, and `add-card` Step 4 for the small SDK
additions a card needs).

A "feature" here is anything that adds vocabulary or behavior the rest of the system builds on:
a new `Effect`/`Trigger`/`Condition`/`DynamicAmount`/`Filter`/`StaticAbility`/`ReplacementEffect`,
a new component or tracker, a new decision type + UI, a new server message, a turn-structure or
priority change, a projection/layer change. Because these are *load-bearing*, the bar for
elegance and reuse is higher than for a card.

## Guiding principle: elegance and a clean, reusable SDK

Read this before touching code. It is the lens for every decision below.

- **Composition over monoliths.** The strongest version of a feature is usually *not a new type
  at all* — it's a composition of existing atoms. Before adding an `Effect`, ask whether
  `CompositeEffect` of existing effects, a `Patterns.*` recipe, or a `DynamicAmount` /
  `GameObjectFilter` parameterization already expresses it. The Gather → Select → Move pipeline
  (`docs/architecture-principles.md` §1.5) covers almost all zone/library manipulation with zero
  new executors. Reach for a new type only when no composition reads the state, produces the
  player interaction, or expresses the timing you need.
- **Think about the SDK as a whole — and the *next* card.** Don't design for the one card or
  situation in front of you. Ask: "what are the slight variations of this that will show up
  later?" A feature built for "+1/+1 for each Goblin" should be the same primitive that later
  serves "−1/−1 for each artifact" or "power equal to your life total". If a small change in
  requirements would force a *second* new type, you've drawn the boundary wrong — generalize now.
- **Name the mechanic, not the card.** `ReduceSpellCostEffect(filter, amount)`, not
  `ReduceGoblinCostEffect`. A name that lies about what the type does (`CreatureTypeCount` that
  actually counts all subtypes) is a bug — rename and document the gap.
- **Parameterize over filter / amount / duration / target / player.** Use `GameObjectFilter`,
  `DynamicAmount`, `Duration`, `EffectTarget`, and `Player`-parametric forms instead of baking in
  constants, a single entity type, or a hardcoded "until end of turn". One type, many cards. See
  `add-card` Step 4 reusability rules and `review-changes` §2 — this skill is held to the same bar.
- **Keep effects pure data; logic lives in executors.** SDK types are serializable data bags with
  no behavior (`docs/architecture-principles.md` §1.1). This is what makes state projection safe,
  networking trivial, and replay deterministic. Never put a lambda or engine reference in an SDK
  type.
- **When the elegant version is more work, do the work.** A one-off type that "works for this
  case" is technical debt that every future card pays interest on. If you genuinely can't find a
  reusable shape, say so explicitly and explain why — don't silently ship the monolith.
- **Don't be afraid to refactor or break backwards compatibility.** This is a young, single-owner
  engine with no external API consumers — there is no compatibility contract to preserve. If
  reshaping an existing type, renaming a primitive, collapsing redundant variants, or changing a
  signature makes the engine or SDK cleaner and simpler, that is *usually the correct call*, not a
  risk to avoid. Don't bolt a new variant alongside a now-obsolete one to "stay safe" — replace the
  old shape and update every call site. (Still respect `CLAUDE.md` collaboration rules: refactor
  *your own* surface freely, but don't revert or stash another agent's in-flight work; if a refactor
  collides with theirs, pause and report.) The condition-hierarchy unification and the `EffectTarget`
  refactor are the model here: sweeping, compat-breaking changes that left the SDK markedly simpler.

If you're unsure whether a new type is justified, apply the `review-changes` §2 test to your own
design: *Could a card compose existing primitives? Is it genuinely novel? Is it parameterized for
the next card? Does the name match the semantics?*

## Step 1: Understand the feature and find the seam

1. **Read the architecture.** [`docs/architecture-principles.md`](../../../docs/architecture-principles.md)
   — especially §1 (SDK data contract: AST values, composable filters, late-binding targets,
   atomic pipelines, DSL) and the §2 subsection for the layer you're touching (projection §2.3,
   continuations §2.4, events §2.5, registries §2.6, replacements §2.7, SBAs §2.8, turn/priority
   §2.9, mana §2.10, copies §2.11).
2. **Read the SDK catalog.** [`docs/card-sdk-language-reference.md`](../../../docs/card-sdk-language-reference.md)
   — the full inventory of existing effects, triggers, conditions, filters, costs, keywords,
   dynamic amounts, modal/choice shapes, and replacement effects. **Most features are 80% already
   built.** Find the closest existing primitive before designing a new one.
3. **Research the actual MTG rules online — don't work from memory.** Before designing anything,
   look up how the mechanic *actually* works in the Comprehensive Rules via the official WotC rules page
   <https://magic.wizards.com/en/rules> (the linked plain-text `.txt` is too large to fetch into context — download
   it and `grep` locally), and check Gatherer / Scryfall oracle rulings for
   the cards that use it (<https://gatherer.wizards.com/> · <https://scryfall.com/>). Use `WebSearch`
   / `WebFetch` to pull the relevant CR section and any official rulings. Mechanics are full of
   edge cases that are easy to misremember — replacement vs. trigger ordering, what counts as
   "last-known information", layer interactions, timing/priority windows, what happens with zero or
   multiple instances. Capture the exact rule numbers and the edge cases they imply; these become
   both your design constraints and your test matrix (Step 7). If a rule contradicts your mental
   model, the rules win — design to them.
4. **Search for prior art.** `grep -r "<mechanic-or-effect>" mtg-sdk/ rules-engine/` and look at how
   the nearest analogous feature is wired end-to-end. Mirror its structure; don't invent a new one.
5. **State the boundary in one sentence.** Write down what the new vocabulary *is* and what it is
   deliberately *not* — this is the contract the rest of the system depends on. If you can't state
   it crisply, the design isn't ready.

## Step 2: Composition-first — try hard not to add a type

Before adding any new SDK type, attempt the composition. Concretely:

- **Behavior** → `CompositeEffect(listOf(...))` of existing `Effects.*`, or a new recipe in
  a pattern object (e.g. `LibraryPatterns.kt`, reached via `Patterns.Library`) composing existing atoms (see [feedback: no single-use patterns] — only add
  a `Patterns.*` recipe when a *second* user appears, or it's a named MTG mechanic; inline
  with explicit flags otherwise).
- **A number that changes with game state** → a `DynamicAmount` composition
  (`Add`, `Subtract`, `Multiply`, `Min`, `Max`, `EntityProperty`, counts over a filter), not a new
  amount node. Add a new `DynamicAmount` variant only when it must *read state no existing node can
  read*.
- **"Which objects"** → a `GameObjectFilter` composition of `CardPredicate` / `StatePredicate` /
  `ControllerPredicate`, not a new filter type.
- **A continuous stat/keyword/type change** → an existing static ability
  (`GrantDynamicStatsEffect`, `ModifyStatsForCreatureGroup`, etc.) fed a `DynamicAmount` /
  `GameObjectFilter`, not a bespoke static ability.

If a composition expresses it: stop. Add the card/recipe and go to tests. **Don't extend an
existing effect with a new optional parameter to cover a variation** — compose instead
(see [feedback: atomic effects]). Document the composition in the SDK reference if it's a reusable
recipe.

## Step 3: If a new type is genuinely needed — design it for reuse

Apply the four reusability principles from `add-card` Step 4 (target generality, duration/removal
generality, parameterized filters/amounts, name-the-mechanic). Then add it in the right place:

| New vocabulary | SDK home | Engine wiring |
|---|---|---|
| Effect | `mtg-sdk/.../scripting/effect/{Category}Effects.kt` + facade in `dsl/Effects.kt` | Executor in `rules-engine/.../handlers/effects/{category}/` + register in `{Category}Executors.kt` |
| Trigger | `mtg-sdk/.../scripting/trigger/` + facade in `dsl/Triggers.kt` | `TriggerDetector` detection path + `TriggerIndex` registration |
| Condition | `mtg-sdk/.../scripting/condition/` + facade in `dsl/Conditions.kt` | `ConditionEvaluator` (must work in *both* resolution and projection via `ConditionEvaluationContext`) |

**Before adding a `Condition` subtype, answer three placement questions** (the hierarchy
re-bloats with one-offs otherwise — see `backlog/sdk-analysis-2026-06-revised.md` §2.3):
1. **"Does X match a filter?"** → it's `Conditions.EntityMatches(entity, filter)` (or one of its
   `SourceMatches` / `EnchantedPermanentMatches` / `TargetMatchesFilter` / `TriggeringSpellMatches`
   facades), *not* a new condition. Name the entity role via `EffectTarget`.
2. **A tracker-shaped check** ("you did/gained/cast N this turn") → `Compare` over a tracked
   `DynamicAmount` (e.g. `TurnTracking`). If the tracked amount doesn't exist, add the *tracker
   enum value* (data), not a condition class.
3. **A genuinely set-specific condition** → put it in the mechanic's own file (quarantined next to
   that mechanic's other SDK surface), never in the general `*Conditions.kt` files.
| Static ability | `mtg-sdk/.../scripting/StaticAbility.kt` | `StateProjector` layer application (correct Rule 613 layer) |
| Replacement effect | `mtg-sdk/.../scripting/ReplacementEffect.kt` (declarative `appliesTo` pattern) | engine interception point |
| DynamicAmount variant | `mtg-sdk/.../scripting/DynamicAmount.kt` | `DynamicAmountEvaluator` |
| Filter predicate | `mtg-sdk/.../model/` predicate types | `PredicateEvaluator` |
| Keyword | `mtg-sdk/.../core/Keyword.kt` | `web-client/src/types/enums.ts` (enum + display name) + icon index |
| Counter type | `core/CounterType.kt` | spans **5 layers** — see `add-card` Step 4.5b (CounterType, StateProjector KEYWORD_COUNTER_MAP, enums.ts, icon index, 3 frontend badge files) |
| Component / tracker | `mtg-sdk/.../component/` or engine component | reader in projection/handlers; cleanup if it has a duration |
| Decision type | `mtg-sdk` decision model | `PendingDecision` + continuation frame + client UI (Step 6) |

Hard rules while writing it (from `CLAUDE.md` "Load-bearing rules" and the engine's recurring bug
classes — `review-changes` §3):

- **Immutability** — never mutate components/state in place; return new instances.
- **Projected state for battlefield reads** — type/subtype/color/keywords/P/T/controller on
  battlefield permanents MUST go through `predicateEvaluator.matchesWithProjection(...)` /
  `projected.isCreature(...)` / `state.projectedState.getController(...)`, never base
  `ControllerComponent` or `cardComponent.typeLine.isCreature`. Non-battlefield zones can use base
  state.
- **Events, not silent mutations** — every state change emits a `GameEvent` so triggers and
  animations can react.
- **One condition, both contexts** — conditions must evaluate identically at resolution and during
  projection (no separate `*ProjectionCondition` types; use `ConditionEvaluationContext`).
- **Continuations carry targets** — any frame wrapping `EffectTarget.ContextTarget(n)` must
  propagate `targets` / `namedTargets` / `outerTargets` into the rebuilt `EffectContext`.
- **Last-known information** — dies/leaves triggers read `triggerLastKnownPower`,
  `lastKnownCardDefinitionId`, `lastKnownCounters` off the `ZoneChangeEvent` (the entity is gone by
  the time the trigger resolves).
- **Layer 613.8 dependency** — new continuous-effect families sort by trial application before
  timestamp; never `toMutableSet()` a `ContinuousEffect` list (it dedupes equal lord effects).

## Step 4: Trace the feature through every layer it touches

This is the analogue of `add-card` Step 9, and it is mandatory for engine features. A feature is
done only when every layer below either handles it or is verified not to need changes. Walk **at
least the happy path plus one edge case** (fizzle, "may" declined, source leaves before resolution,
empty/zero input, simultaneous instances, replacement interaction):

| Layer | What to verify |
|---|---|
| **SDK (data)** | Pure, serializable, fully parameterized? Round-trips through serialization? |
| **Engine (handler/executor)** | Right handler/executor picks it up; emits the right `GameEvent`s; returns correct new `GameState`. Registered in the right registry. |
| **TriggerDetector** | New trigger detected from emitted events; registered in `TriggerIndex`; correct detection path (battlefield `detectTriggers` vs phase/step `detectPhaseStepTriggers` vs `detectLeavesBattlefieldTriggers`). |
| **StateProjector** | New continuous effect/static ability applied in the correct Rule 613 layer; projected state reflects it; dependency ordering holds. |
| **Continuations** | Player-input features pause with a `PendingDecision` and resume correctly, carrying targets/collections. |
| **Cleanup** | Duration-bounded state removed at the right time (end of turn/combat, source leaves). |
| **Server DTO / masking** | New `GameEvent` → branch in `ClientEvent.kt` exhaustive `when`. New client-visible state → `ClientStateTransformer`. Private info masked by `StateMasker`. |
| **Legal actions** | New player action enumerated by an `ActionEnumerator`; never computed client-side. |
| **Frontend** | New decision/UX → component in `web-client/src/components/decisions/`; new keyword/icon → `enums.ts`, display names, icon index. |

Write a short trace per scenario (SDK → engine → triggers → continuations → DTO → frontend, each
✅ or a noted gap). Fix every gap before proceeding.

## Step 5: Performance

The engine runs full state projection and legal-action enumeration constantly (every priority
pass, every AI/MCTS node). Cheap-looking work in a primitive multiplies.

- **Don't recompute projection.** Projected state is cached per immutable `GameState`; read
  `state.projectedState`, don't re-run `StateProjector` yourself. Inside executors that already
  have a projected state in context, reuse it.
- **Keep `DynamicAmount` / filter evaluation allocation-light.** They run inside projection and
  enumeration hot paths. Avoid building large intermediate collections per entity; prefer counting
  over a filtered view to materializing lists.
- **Respect immutability without copying the world.** Use the `with`/`without` component helpers
  and `copy(...)` of the changed slice — don't rebuild whole maps. Local `mutableListOf`
  accumulation inside a pure function is fine (the SBA loop does this); leaking mutation across
  calls is not.
- **Watch the client too.** New per-card fields in `ClientCard` or frequent `StateDeltaUpdate`
  churn can cause battlefield re-renders. If the feature adds card-visible state, confirm it only
  changes when it actually changes (see the recent battlefield re-render perf work).
- **Benchmarks** — the AI `*Benchmark` classes are too slow for routine validation; run `*Test`
  classes during development (see [feedback: skip benchmarks]).

## Step 6: UX / UI

The client is a dumb terminal (`docs/architecture-principles.md` §4): it renders what the server
sends and captures intent. Server-side feature design *is* the UX.

**If the feature surfaces anything to the player — a new decision, action, choice prompt, keyword,
icon, badge, label, or any new client-visible state — the UX/UI side is part of this feature, not a
follow-up.** Design and implement it in the same change, trace the full player flow end to end (a
human must be able to actually see and act on the feature in the running client), and don't declare
the feature done until that flow works. A purely internal engine feature with no player-facing
surface can skip this step — but say so explicitly rather than silently leaving the UI unbuilt.

- **Server-authoritative interactivity.** The feature becomes clickable only because it appears in
  the server's legal actions / `PendingDecision`. Make sure the new action/decision is enumerated;
  never add client-side rules to make something interactive.
- **Route decisions to the right component.** Map each player choice to an existing component in
  `web-client/src/components/decisions/` (battlefield selection, graveyard/library/zone overlays,
  yes/no, choose color/number/option, modal/budget). Prefer **on-battlefield selection** over a
  card-list overlay when choosing among permanents in play — overlays hide counters/effects/board
  context (see [feedback: UX review] and `add-card` Step 6).
- **Extend before creating.** Prefer extending an existing decision component over a new one. Add a
  new decision type/component only when an existing one genuinely can't express the interaction.
- **Clear labels.** Any `description` on a mode/ability becomes button/label text — write it from
  the player's perspective.
- **Suppress stale UI at the handler**, not just at render time, if a sticky store field drives a
  UI element (see [feedback: suppress stale UI state]).
- **Trace the player flow** end to end for the feature, exactly as `add-card` Step 6.1 prescribes.

## Step 7: Tests — match the pyramid

**The engine (`rules-engine`) tests are the most important part of this step and the bar is high:
they must cover the *complete* rules of the mechanic and its edge cases, not just the happy path.**
The engine is the source of truth — if it implements a rule wrong, no amount of SDK or client
testing catches it. Treat the rules and rulings you researched in Step 1.3 as the spec and make the
engine tests prove every clause of that spec. SDK round-trip and client tests are supporting layers;
do not let them substitute for thorough engine coverage. A feature is under-tested until an engine
test would fail if any rule it implements were broken.

Pick the layer that proves the feature, per `docs/architecture-principles.md` §5:

- **SDK round-trip / unit** (`mtg-sdk`) — new data type serializes and composes as intended.
- **Engine unit/integration** (`rules-engine`) — the executor/projector/detector behaves in
  isolation; construct `GameState` directly and assert on the result.
- **Scenario** (`com.wingedsheep.engine.support.ScenarioTestBase`) — the feature works in a realistic
  board state, exercised through a card that uses it. **Every rule you looked up in Step 1.3 must have
  a paired test that asserts the engine behaves as the CR / oracle rulings say** — including the tricky
  edge cases (replacement vs. trigger ordering, last-known info, layer interactions, timing/priority
  windows, zero/multiple instances). A rule cited in a code comment without a test that pins it down is
  a gap (`review-changes` §5). Cover the edge cases you traced in Step 4. **These belong in
  `rules-engine`** (package `com.wingedsheep.engine.scenarios`, under
  `rules-engine/src/test/kotlin/com/wingedsheep/engine/scenarios/`) — the engine is the source of truth,
  so a feature's rules are proven there, not in `game-server`. `game-server` tests only cover what is
  genuinely a game-server concern: the interaction between the frontend and the engine (state masking,
  DTO transformation, session/tournament orchestration). Do not write a `game-server` scenario test to
  prove engine behavior.
- **Harness & scope** — two setup styles, both in `rules-engine/src/testFixtures` and backed by the
  real `ActionProcessor`: `ScenarioTestBase` (fluent static board: `scenario().withCardOnBattlefield(...)
  .build()` + name-based actions/decision sugar) and `GameTestDriver` (live game: real turns, priority,
  mana). Both register the full `MtgSetCatalog` plus test-only cards (`TestCards.all`) and the predefined
  tokens — so any printed card is available by name. For a card that doesn't exist yet, define an inline
  test card via `CardDefinition.creature(...)` and `cardRegistry.register(...)` in `init { }`.

Run them yourself: `just test` / `just test-rules` / `just test-class <Name>` (or the direct
gradle module command). Don't trust a description — confirm green. If a registry/executor/evaluator
signature changed, run the broader module suite.

## Step 8: Update the docs in the same change

- **`docs/card-sdk-language-reference.md` is the canonical SDK catalog.** Any new effect, trigger,
  condition, filter, cost, keyword, dynamic amount, modal shape, or replacement effect MUST be
  documented in the appropriate section in this same change (this is a `CLAUDE.md` standing rule).
- **`docs/architecture-principles.md`** — update only if the feature introduces or changes an
  *architectural* concept (a new layer of the engine, a new cross-cutting pattern), not for a
  routine new primitive.
- Update any other doc the feature contradicts (`engine-server-interface.md`, `data-contracts.md`,
  `player-input.md`, `web-client-architecture.md`, `continuous-effect-dependency-system.md`).

## Step 8b: Teach the mtgish generator your new capability

Ideally, a new SDK primitive doesn't just serve the card in front of you — it also becomes something
the [`:mtgish-tooling`](../../../mtgish-tooling/README.md) generator can *predict and draft* for every
set. This has wider benefits than the one feature: the tooling maps the mtgish IR corpus across the
whole corpus, so one bridge/emitter entry typically unlocks coverage and auto-draft for many cards
that share the mechanic. When your feature corresponds to an mtgish IR tag, add it in two places:

- **Capability bridge** (`mtgish-tooling/.../coverage/bridge/`) — add a one-line mapping from the
  mtgish tag to your new Argentum capability in the closest themed bridge file, so the probe scores
  cards that use it as *coverable* instead of *blocked*.
- **Rendering emitter** (`mtgish-tooling/.../coverage/emitter/*Handlers.kt`) — add a `simple("Tag",
  "MyEffect()")` (argument-free) or `on("Tag") { node, args, tvar -> ... }` (needs amount/target/filter
  recovery) entry that renders the `cardDef` DSL for your new primitive. Handlers don't track imports
  (`Shells.importsFor` derives them); if the handler needs target/filter support, extend
  `TargetRecovery.kt` rather than widening filters. Return `null` for shapes you can't render whole.

Then prove it: `just coverage-verify --set <SET>` (a set that uses the mechanic) should now show the
relevant cards compile-verified with no capability mismatch. This is best-effort — if the mechanic is
genuinely too card-specific or X-carrying for the emitter to render exactly, leave the emitter
returning `null` (the `SCAFFOLD` tier) and note why, but still add the bridge entry so coverage scoring
is correct. See [`mtgish-tooling/README.md`](../../../mtgish-tooling/README.md) §"Adding a handler".

## Step 9: Build, verify, commit

1. `just build` (no new behavior to exercise) or `just test` (new effects/engine changes). Fix only
   failures your change caused; if a pre-existing/other-agent test fails, report it and stop
   (`CLAUDE.md` collaboration rules — never revert or stash others' work).
2. **Verify CR rule numbers** you cite in code/comments/commit via the official WotC rules page
   <https://magic.wizards.com/en/rules> (download the linked plain-text `.txt` and `grep` it locally — too large to
   fetch into context) before committing — they're easy to misremember.
   Describe the rule by name if you can't confirm the number.
3. Commit with a message describing the capability (e.g. `Add <mechanic> support to the engine`).
   End the message with the project's `Co-Authored-By` trailer. Commit to the current branch; don't
   push unless asked.

## Anti-patterns to reject in your own design

- A new `Effect`/`StaticAbility` whose executor converts it 1:1 into an existing `Modification` /
  effect with a literal formula → use the existing type fed a `DynamicAmount`.
- A new optional parameter bolted onto an existing effect to cover a variation → compose instead.
- Constants baked into a type (`bonusPerType = 1`, hardcoded subtype, `count = 20` for "any
  number") → parameterize (`DynamicAmount`, `GameObjectFilter`, `unlimited`/`dynamicMaxCount`
  flags).
- A type named for the card that motivated it.
- Battlefield reads against base state instead of projected state.
- A new `GameEvent` without a `ClientEvent.kt` branch; client-visible state without a transformer.
- Game logic creeping into `web-client`.
- A single-use `Patterns.*` recipe with no second caller (inline it).

## Important rules

1. **Compose first** — a new SDK type is the last resort, not the first move.
2. **Design for the next card, not this one** — parameterize over filter/amount/duration/target.
3. **Name the mechanic, not the card.**
4. **Keep SDK types pure serializable data**; logic lives in executors.
5. **Immutability + projected-state reads** are non-negotiable on battlefield code.
6. **Trace every layer** (Step 4) before declaring done.
7. **Mind performance** on projection/enumeration hot paths and client re-renders.
8. **Server-authoritative UX**; the client only renders and captures intent.
9. **Update `card-sdk-language-reference.md`** for every SDK addition, same change.
9b. **Teach the mtgish generator** (Step 8b) — add a bridge capability + emitter handler so the new
    primitive unlocks coverage and auto-draft across every set, not just this feature.
10. **Research the real MTG rules online up front** (CR + oracle rulings), design to them, **verify
    CR rule numbers, and write a paired test for every rule and edge case** you looked up.
11. **Be elegant — no monolithic one-off shortcuts.** If you can't find the reusable shape, say so
    and explain why rather than shipping the monolith.
12. **Refactor freely; backwards compat is not sacred.** There are no external API consumers — if
    reshaping/renaming/collapsing existing types makes the engine or SDK cleaner, do it and update
    all call sites, rather than adding a parallel variant. (Don't touch another agent's in-flight
    work — pause and report instead.)
