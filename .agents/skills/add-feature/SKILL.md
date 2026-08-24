---
name: add-feature
description: Adds a new feature or mechanic to the Argentum Engine (SDK primitive, effect, trigger, condition, static/replacement ability, server/client capability) following the project's architecture and SDK-elegance principles. Use when implementing engine/SDK/server/client functionality that isn't a single card — e.g. "add an effect type", "support a new keyword", "add a decision UI", "make the engine handle X".
argument-hint: <feature description>
---

# Add Engine Feature

For work that extends the **engine, SDK, server, or client** — a new primitive, mechanic, decision flow,
or capability. A single card goes to `add-card` instead (including the small SDK additions one card
needs).

A "feature" here is anything that adds vocabulary the rest of the system builds on: a new
`Effect` / `Trigger` / `Condition` / `DynamicAmount` / `Filter` / `StaticAbility` / `ReplacementEffect`, a
component or tracker, a decision type + UI, a server message, a turn-structure or priority change, a
projection or layer change. **Because these are load-bearing, the bar for elegance and reuse is higher
than for a card.**

That bar lives in [`docs/sdk-design-principles.md`](../../../docs/sdk-design-principles.md) —
composition over monoliths, the four reusability rules, designing for the *next* card, pure-data SDK
types, and why refactoring is cheap here. **Read it before touching code**; it's the lens for every
decision below.

## Step 1: Understand the feature and find the seam

1. **Read the architecture.**
   [`docs/architecture-principles.md`](../../../docs/architecture-principles.md) — §1 (SDK data contract:
   AST values, composable filters, late-binding targets, atomic pipelines, DSL) plus the §2 subsection
   for the layer you're touching: projection §2.3, continuations §2.4, events §2.5, registries §2.6,
   replacements §2.7, SBAs §2.8, turn/priority §2.9, mana §2.10, copies §2.11.

2. **Read the SDK catalog.**
   [`docs/card-sdk-language-reference.md`](../../../docs/card-sdk-language-reference.md) — **most features
   are 80% already built.** Find the closest existing primitive before designing a new one.

3. **Research the real MTG rules — don't work from memory.** Look up how the mechanic actually works in
   the Comprehensive Rules (<https://magic.wizards.com/en/rules>; the plain-text `.txt` is too large to
   fetch into context — download it and `grep` locally) and check oracle rulings on Gatherer / Scryfall
   for the cards that use it.

   Mechanics are full of edge cases that are easy to misremember: replacement-vs-trigger ordering, what
   counts as last-known information, layer interactions, timing and priority windows, what happens with
   zero or multiple instances. **The rule numbers and edge cases you capture here become both your design
   constraints and your test matrix in Step 7.** If a rule contradicts your mental model, the rules win.

4. **Search for prior art.** `grep -r "<mechanic-or-effect>" mtg-sdk/ rules-engine/` and read how the
   nearest analogous feature is wired end to end. Mirror its structure; don't invent a new one.

5. **State the boundary in one sentence** — what the new vocabulary *is* and what it deliberately is
   *not*. That's the contract everything else depends on. If you can't state it crisply, the design isn't
   ready.

## Step 2: Composition-first — try hard not to add a type

Work the substitution table in
[`sdk-design-principles.md`](../../../docs/sdk-design-principles.md#composition-over-monoliths):
behavior → `CompositeEffect` or a `Patterns.*` recipe; a state-dependent number → a `DynamicAmount`
composition; "which objects" → a `GameObjectFilter` composition; a continuous stat/keyword change → an
existing static ability fed a `DynamicAmount`.

If a composition expresses it: **stop.** Add the recipe, go to tests, and document it in the SDK
reference if it's reusable.

## Step 3: If a new type is genuinely needed

Apply the four reusability rules (target generality, duration/removal generality, parameterized
filters/amounts, name-the-mechanic), then put it in the right place —
[`add-card/new-sdk-types.md`](../add-card/new-sdk-types.md) has the SDK-home / engine-wiring table for
every kind of vocabulary, plus the five layers a counter type spans.

**Before adding a `Condition` subtype, answer three placement questions** — the hierarchy re-bloats with
one-offs otherwise (`backlog/sdk-analysis-2026-06-revised.md` §2.3):

1. **"Does X match a filter?"** → that's `Conditions.EntityMatches(entity, filter)` or one of its
   `SourceMatches` / `EnchantedPermanentMatches` / `TargetMatchesFilter` / `TriggeringSpellMatches`
   facades, *not* a new condition. Name the entity role via `EffectTarget`.
2. **Tracker-shaped** ("you did/gained/cast N this turn") → `Compare` over a tracked `DynamicAmount` such
   as `TurnTracking`. If the tracked amount doesn't exist, add the *tracker enum value* (data), not a
   condition class.
3. **Genuinely set-specific** → put it in that mechanic's own file, quarantined next to the rest of its
   SDK surface, never in the general `*Conditions.kt`.

Hard rules while writing it — these are the engine's recurring bug classes:

- **Immutability** — never mutate components or state in place; return new instances.
- **Projected state for battlefield reads** — type/subtype/color/keywords/P/T/controller on battlefield
  permanents go through `predicateEvaluator.matches(state, projected, …)` (pass `state.projectedState`;
  it's a required parameter) / `projected.isCreature(…)` / `state.projectedState.getController(…)`, never
  base `ControllerComponent` or `cardComponent.typeLine.isCreature`. Passing the projection is safe in
  every zone — non-battlefield entities have no projection entry and fall back to base data.
- **Events, not silent mutations** — every state change emits a `GameEvent`.
- **One condition, both contexts** — conditions must evaluate identically at resolution and during
  projection. No separate `*ProjectionCondition` types; use `ConditionEvaluationContext`.
- **Continuations carry targets** — any frame wrapping `EffectTarget.ContextTarget(n)` must propagate
  `targets` / `namedTargets` / `outerTargets` into the rebuilt `EffectContext`.
- **Last-known information** — dies/leaves triggers read `triggerLastKnownPower`,
  `lastKnownCardDefinitionId`, `lastKnownCounters` off the `ZoneChangeEvent`; the entity is gone by the
  time the trigger resolves.
- **Layer 613.8 dependency** — new continuous-effect families sort by trial application before timestamp.
  Never `toMutableSet()` a `ContinuousEffect` list; it dedupes equal lord effects.

## Step 4: Trace the feature through every layer

**Mandatory.** A feature is done only when every layer either handles it or is verified not to need
changes. Walk at least the happy path plus one edge case (fizzle, "may" declined, source leaves before
resolution, empty/zero input, simultaneous instances, replacement interaction):

| Layer | What to verify |
|---|---|
| **SDK (data)** | Pure, serializable, fully parameterized. Round-trips through serialization |
| **Engine handler/executor** | Right executor picks it up, emits the right `GameEvent`s, returns the right `GameState`, registered in the right registry |
| **TriggerDetector** | Detected from emitted events, registered in `TriggerIndex`, on the correct path — battlefield `detectTriggers` vs `detectPhaseStepTriggers` vs `detectLeavesBattlefieldTriggers` |
| **StateProjector** | Applied in the correct Rule 613 layer, reflected in projected state, dependency ordering holds |
| **Continuations** | Player-input features pause with a `PendingDecision` and resume carrying targets/collections |
| **Cleanup** | Duration-bounded state removed at the right time (end of turn/combat, source leaves) |
| **Server DTO / masking** | New `GameEvent` → branch in `ClientEvent.kt`'s exhaustive `when`; new client-visible state → `ClientStateTransformer`; private info masked by `StateMasker` |
| **Legal actions** | New player action enumerated by an `ActionEnumerator` — never computed client-side |
| **Frontend** | New decision/UX → component in `web-client/src/components/decisions/`; new keyword/icon → `enums.ts`, display names, icon index |

Write a short trace per scenario and fix every gap before proceeding.

## Step 5: Performance

The engine runs full state projection and legal-action enumeration constantly — every priority pass,
every AI/MCTS node. **Cheap-looking work in a primitive multiplies.**

- **Don't recompute projection.** It's cached per immutable `GameState`; read `state.projectedState`
  rather than re-running `StateProjector`. Inside an executor that already has one in context, reuse it.
- **Keep `DynamicAmount` and filter evaluation allocation-light** — they run inside projection and
  enumeration hot paths. Count over a filtered view instead of materializing lists per entity.
- **Respect immutability without copying the world.** Use the `with`/`without` component helpers and
  `copy(…)` of the changed slice; don't rebuild whole maps. Local `mutableListOf` accumulation inside a
  pure function is fine (the SBA loop does it); leaking mutation across calls is not.
- **Watch the client too.** New per-card fields on `ClientCard` or frequent `StateDeltaUpdate` churn cause
  battlefield re-renders. If the feature adds card-visible state, confirm it only changes when it
  actually changes.

## Step 6: UX / UI

The client is a dumb terminal (`architecture-principles.md` §4) — it renders what the server sends and
captures intent. **Server-side feature design *is* the UX.**

**If the feature surfaces anything to the player** — a decision, action, prompt, keyword, icon, badge,
label, or any client-visible state — **the UI is part of this feature, not a follow-up.** Build it in the
same change and trace the full player flow until a human can actually see and act on it in the running
client. A purely internal feature can skip this step, but say so explicitly rather than leaving the UI
silently unbuilt.

- **Server-authoritative interactivity.** The feature becomes clickable only because it appears in the
  server's legal actions or `PendingDecision`. Never add client-side rules to make something interactive.
- **Route to the right component** in `web-client/src/components/decisions/`. Prefer **on-battlefield
  selection** over a card-list overlay when choosing among permanents in play — overlays hide counters,
  effects, and board context.
- **Extend before creating.** A new decision component only when an existing one genuinely can't express
  the interaction.
- **Clear labels.** Any `description` on a mode or ability becomes button text; write it from the
  player's perspective.
- **Suppress stale UI at the handler**, not just at render time, when a sticky store field drives an element.

## Step 7: Tests

**The `rules-engine` tests are the most important part of this step and the bar is high: they must cover
the *complete* rules of the mechanic and its edge cases, not just the happy path.** The engine is the
source of truth — if it implements a rule wrong, no amount of SDK or client testing catches it.

Treat the rules and rulings from Step 1.3 as the spec. **Every rule you looked up must have a paired test
asserting the engine behaves as the CR and oracle rulings say** — including replacement-vs-trigger
ordering, last-known info, layer interactions, timing/priority windows, and zero/multiple instances. A
rule cited in a code comment without a test pinning it down is a gap. *A feature is under-tested until an
engine test would fail if any rule it implements were broken.*

Pick the layer that proves it (`architecture-principles.md` §5):

- **SDK round-trip** (`mtg-sdk`) — the data type serializes and composes as intended.
- **Engine unit/integration** (`rules-engine`) — construct `GameState` directly and assert on the
  executor/projector/detector in isolation.
- **Scenario** (`rules-engine`, package `com.wingedsheep.engine.scenarios`) — the feature working in a
  realistic board state through a card that uses it.

SDK and client tests are supporting layers; don't let them substitute for engine coverage. Harness
details (`ScenarioTestBase` vs `GameTestDriver`, inline test cards) and which command to run: the
**`verify`** skill.

## Step 8: Update the docs in the same change

- **[`docs/card-sdk-language-reference.md`](../../../docs/card-sdk-language-reference.md)** — every new
  effect, trigger, condition, filter, cost, keyword, dynamic amount, modal shape, or replacement effect,
  in the same change. Standing `AGENTS.md` rule.
- **[`docs/architecture-principles.md`](../../../docs/architecture-principles.md)** — only when the
  feature introduces or changes an *architectural* concept (a new engine layer, a new cross-cutting
  pattern), not for a routine primitive.
- Any other doc the feature contradicts: `engine-server-interface.md`, `data-contracts.md`,
  `player-input.md`, `web-client-architecture.md`, `continuous-effect-dependency-system.md`.

## Step 8a: Keep Argentum Assay compiling and honest

[Argentum Assay](../../../oracle-assay/README.md) — our first-party Oracle-text parser — reads printed
text into the very SDK types you just touched, so it is a compile-time consumer of `mtg-sdk` and the
closest thing the SDK has to an outside reader.

- **You changed or renamed an existing type** (new required parameter, moved constant, reshaped effect):
  fix the grammar rules that construct it **in this change**, and run `just assay-gate --limit 2000` so
  the touchstone still round-trips. Skipping this breaks the `:oracle-assay` build for everyone.
- **You added a new type**: nothing is owed here. An Assay rule is bidirectional and ships as a measured
  *band* — its own probe, ranking and PR (see [`docs/oracle-assay.md`](../../../docs/oracle-assay.md)) —
  so don't grow this PR into one. Name the new vocabulary in the PR body instead:
  `just assay-report --rank tail` is the ranked backlog of what the grammar still can't read, and new
  vocabulary is how a row on it becomes reachable.
- **Your feature changed how existing cards behave**: run `just assay-differential` over the affected
  set. A new `DIVERGENT` row means Assay and the corpus now disagree about a card you moved — classify it
  before committing rather than leaving it for the next set sweep.

## Step 8b: Teach the mtgish generator your new capability

A new SDK primitive should also become something the mtgish generator can *predict and draft*
corpus-wide — one bridge/emitter entry typically unlocks coverage and auto-draft for many cards sharing
the mechanic. Mechanics: [`add-card/new-sdk-types.md`](../add-card/new-sdk-types.md) → "Teach the mtgish
generator". Unlike an Assay band, this really is a one-line entry, which is why it stays in-PR.

**Gate this on the right axis.** It applies when the feature introduces a new SDK effect/primitive
mapping to an mtgish IR tag. It does **not** apply to pure composition of effects the emitter already
renders (e.g. a `jobSelect()` keyword shell chaining `CreateToken` + `AttachEquipment`) — there's no new
capability or IR tag to register, so skip it and say so.

Crucially, **do not gate on whether the motivating set is in the mtgish corpus.** The generator is
corpus-wide; "the set isn't in the corpus / there's no `coverage-verify --set X` path" is not a valid
reason to decline.

## Step 9: Build, verify, commit

1. Run the gates via the **`verify`** skill. Fix only failures your change caused; if a pre-existing or
   other-agent test fails, report it and stop.
2. **Verify every CR rule number** you cite in code, comments, or the commit message against the official
   rules text before committing. Describe the rule by name if you can't confirm the number.
3. Commit describing the capability (`Add <mechanic> support to the engine`), ending with the project's
   `Co-Authored-By` trailer. Current branch; don't push unless asked.

## Anti-patterns to reject in your own design

- A new `Effect`/`StaticAbility` whose executor converts it 1:1 into an existing `Modification` or effect
  with a literal formula → use the existing type fed a `DynamicAmount`.
- A new optional parameter bolted onto an existing effect to cover a variation → compose instead.
- Constants baked into a type (`bonusPerType = 1`, a hardcoded subtype, `count = 20` for "any number") →
  parameterize.
- A type named for the card that motivated it.
- Battlefield reads against base state instead of projected state.
- A new `GameEvent` with no `ClientEvent.kt` branch; client-visible state with no transformer.
- Game logic creeping into `web-client`.
- A single-use `Patterns.*` recipe with no second caller — inline it.
