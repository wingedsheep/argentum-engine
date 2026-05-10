---
name: review-changes
description: Review pending changes (a branch, PR, or working tree) for the Argentum Engine. Optimizes for an elegant, reusable SDK — flags one-off effects/abilities that should compose existing primitives — and checks correctness, projection use, tests, and architectural fit. Use when the user says "review this PR", "review this branch", "review my changes", or asks for a code review of pending work.
argument-hint: [<PR# | branch | path>]
---

# Review Changes

Primary lens: **SDK elegance**. The SDK must stay small and reusable so new cards compose
existing primitives instead of growing a card-specific type per Magic card.

`$ARGUMENTS` may be a PR number/URL, a branch name, or empty (review the working tree's
diff vs `main`).

## 1. Establish the diff

- **PR** — `gh pr view <n> --json …`, then `git fetch <fork> <head>:pr-<n>-review &&
  git checkout pr-<n>-review`. Diff vs `main` (or `baseRefName`).
- **Branch** — `git diff main...<branch>` (three-dot).
- **Empty** — `git diff main...HEAD` plus `git status`.

Read every changed file in full, not just the hunk. For large diffs, spawn `Explore` for
unfamiliar areas.

## 2. SDK elegance — the central question

For every new SDK type the diff introduces (`Effect`, `StaticAbility`, `Trigger`,
`Condition`, `TargetRequirement`, `EntityNumericProperty`, `DynamicAmount` variant,
`Modification`, `ReplacementEffect`, …), ask:

1. **Could the card compose existing primitives?** Tell-tale: the engine handler converts
   the new type 1:1 into an existing `Modification` / effect with a literal formula
   (`is NewThing -> ContinuousEffectData(Modification.X(literal), filter)`) where
   `Modification.X` already takes a `DynamicAmount`. Build the formula via
   `DynamicAmount.{Min,Max,Multiply,Add,EntityProperty,…}` and use the existing static
   ability (e.g. `GrantDynamicStatsEffect`).
2. **Is it genuinely novel?** Acceptable: reads state no primitive can read (new
   component, tracker, counter filter); player-interaction shape no executor produces;
   layer/timing semantics not expressible in the AST.
3. **Parameterized for the next card, not this one?** Constants baked in
   (`bonusPerType=1`, `maxBonus=10`, hardcoded subtype) → the next similar card forces
   another type. Prefer a small generic primitive + DSL recipe in `EffectPatterns` /
   `Conditions` / `Filters`.
4. **Name matches semantics?** `CreatureTypeCount` that counts all subtypes is a name
   lie — rename and document the gap.

When you find one, **show the rewrite**. A concrete card-using-existing-primitives is
more useful than abstract objection.

Reference: `docs/architecture-principles.md` §1.5 (atomic pipelines), §1.2 (AST for
dynamic values), §1.3 (composable filtering), §1.6 (DSL as abstraction).

## 3. Correctness — recurring bug classes in this engine

- **Projected vs base state** (`docs/architecture-principles.md` §2.3). Battlefield reads
  of type/subtype/color/keywords/P/T/controller MUST go through projection
  (`predicateEvaluator.matchesWithProjection`, `projected.getSubtypes`,
  `projected.isCreature`, `state.projectedState.getController`). Flag base
  `ControllerComponent` or `cardComponent.typeLine.isCreature` on battlefield permanents.
- **Layer 613.8.** New continuous-effect families: dependency-by-trial-application must
  hold; never `toMutableSet()` `ContinuousEffect` lists (dedupes equal lord effects).
- **Events, not silent mutations.** Every state change emits a `GameEvent`. Flag bypasses.
- **Trigger detection paths.** Battlefield → `detectTriggers`; phase/step →
  `detectPhaseStepTriggers` (called by `PassPriorityHandler`, NOT `matchesTrigger`);
  leaves-the-battlefield → `detectLeavesBattlefieldTriggers`.
- **Last-known information.** Dies/leaves triggers must read `triggerLastKnownPower`,
  `lastKnownCardDefinitionId`, `lastKnownCounters` from `ZoneChangeEvent` (tokens
  disappear in the same SBA pass).
- **Continuations carry targets.** Frames wrapping `EffectTarget.ContextTarget(n)` must
  propagate `targets` / `namedTargets` / `outerTargets` into `EffectContext`.
- **Modal spells.** Check `modeTargetsOrdered` is built from flat `targets`, and
  no-target modes inherit outer targets.
- **Mana / costs.** New `ManaSource` shapes must reserve mana for self-activation costs.
- **Anti-corruption layer.** New `GameEvent` → branch in `ClientEvent.kt` exhaustive
  `when`. New client-visible state → `ClientStateTransformer`.
- **Dumb-terminal client.** No game logic in `web-client`; server sends legal actions.

Consult the relevant doc when the change touches an area:
`continuous-effect-dependency-system.md`, `engine-server-interface.md`,
`data-contracts.md`, `card-definition-guide.md`, `api-guide.md`.

## 4. MTG rules accuracy — verify every cited rule

Whenever the diff (code, comments, commit message, or PR body) cites a CR rule number,
**verify it online** before accepting it:

```
WebFetch https://yawgatog.com/resources/magic-rules/   # current Comprehensive Rules
WebFetch https://api.scryfall.com/cards/named?exact=<card>&set=<code>   # Oracle text
```

CR numbers drift between editions and are easy to misremember (613.7 vs 613.8, 704.5 vs
704.6, 608.2b vs 608.2c). If the cited number doesn't match the rule's text, fix it in
the review (and request the author fix it in code/commit). When in doubt, describe the
rule by name rather than guessing a number.

For card text, cross-check Scryfall Oracle — the PR may be implementing pre-errata
wording. Spell out the rules path you expect for corner cases (Changeling, copyable
values, layer interactions, "as ~ enters", protection / hexproof / ward).

## 5. Tests — every cited rule must be exercised

- **Coverage of cited rules.** Every rule the implementation references in code or
  comments must have a test that exercises it. If the change cites "CR 702.19c trample
  through dead blockers", there must be a test where the attacker has trample and a
  blocker dies; otherwise the citation is decorative. Flag rule-citations without a
  paired test case.
- **Interesting axes.** Typical case + the rule-corner that drove the change (Changeling
  for type-counting; regeneration for destroy-vs-exile; last-known-info for dies
  triggers; first-strike + trample interaction; etc.).
- **Module placement.** Card-interaction scenarios → `game-server` `scenarios/` (full
  stack). Pure rules → `rules-engine` `scenarios/` with `ScenarioTestBase`. SDK round-
  trips → `mtg-sdk`. JSON round-trip fixtures are NOT required per card.
- **`ScenarioTestBase` set scope.** Only registered sets are loaded; cards from other
  sets must be defined inline via `CardDefinition.creature(...)` and registered via
  `cardRegistry.register(card)` in `init { }`.
- **Run the tests yourself.** `./gradlew :game-server:test --tests "<Name>"`,
  `:rules-engine:test`, `:mtg-sdk:test` after engine/SDK changes. Confirm green; don't
  trust the PR description. Run the broader module suite if a registry/executor/evaluator
  signature changed.

## 6. Style & scope

- Comments only when *why* is non-obvious (project CLAUDE.md). Flag restated-code
  comments and "added for X" notes.
- No backwards-compat hacks (unused fields, `// removed` markers).

## 7. Output

1. **Verdict** (1–2 sentences) — is the behavior right? Is the SDK shape right?
2. **What's good** — genuine positives worth keeping if the author rewrites: clean tests,
   right plumbing, good naming, well-chosen primitives. Skip filler; if there's nothing
   real to praise, say so briefly. Do this *before* the issues so the author knows what
   not to throw away.
3. **Issues, by severity:**
   - **Blocking** — wrong behavior, broken rules, missing wiring (new event without
     `ClientEvent.kt` branch), tests that don't exercise the change, base-vs-projected
     state bugs. Must be fixed before merge.
   - **Important** — over-specialized SDK types (show the rewrite), CR-number mismatches,
     missing rule-corner test, projection fallback gaps, naming that lies about
     semantics. Should be fixed before merge.
   - **Minor** — comment hygiene, descriptions, drive-by formatting, dead code, doc
     inconsistencies. Author's discretion.

   Each issue: one short paragraph with `file:line` and the concrete fix.
4. **Recommendation** — concrete next action ("drop type X, define card via Y, add a
   test for Z"). If the diff is fine as-is, say so.

This skill writes a review into the conversation. It does not push, post via `gh`, run
`/ultrareview`, auto-fix, or touch other agents' work.
