# LTR "Implement all remaining cards" — Decisions Log

Branch: `ltr-all-cards`. Goal: implement every unchecked card in `cards.md`, one commit per
card, faithful to oracle text (`ltr_set.json`) and the Comprehensive Rules
(`MagicCompRules_20260417.pdf`). Engine gaps are listed in `TODO.md`; this log records the
concrete decisions made per card as they are implemented.

Workflow note: per the user, this is a single sweeping effort on one branch (`ltr-all-cards`),
**one commit per card** (not one-PR-per-gap as the original TODO suggested). When a card needs a
new engine primitive, the primitive + card + tests + SDK-reference update land together in that
card's commit (or a small group commit when several cards share one freshly built primitive).

## Order of attack

Roughly grouped so that cards sharing a freshly built engine gap land together. See per-card
entries below for the actual decisions.

---

## Per-card decisions

### Bill the Pony (White) — Gap 27, assign combat damage by toughness

- **Oracle:** ETB create two Food; "Sacrifice a Food: Until end of turn, target creature you control
  assigns combat damage equal to its toughness rather than its power."
- **Engine gap:** the existing `AssignDamageEqualToToughness` is a *static* ability read off a card's
  printed statics by `CombatDamageUtils`; there was no turn-scoped, granted form.
- **Decision:** add a new `AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS` and grant it via the
  existing `Effects.GrantKeyword(AbilityFlag, target, Duration.EndOfTurn)` floating-effect path
  (Layer.ABILITY → projected keywords). `CombatDamageUtils.assignsDamageAsToughness` now short-circuits
  to `true` when the creature carries that projected flag — **unconditional** (no `toughness > power`
  gate), matching Bill's wording. This composes with the whole existing GrantKeyword machinery rather
  than adding a bespoke effect/executor; mirrors the Gap-39-style "grant a combat flag" pattern.
- **Composition:** ETB = `Effects.CreateFood(2)`; cost = `Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food"))`;
  target = `Targets.CreatureYouControl`.
- **Touched:** `AbilityFlag.kt` (+flag), `CombatDamageUtils.kt` (+projected-flag check), client
  `enums.ts` (AbilityFlag mirror + display name), card + `BillThePonyScenarioTest`, SDK reference.

### Slip On the Ring (White) — Gap 14, flicker (NO engine change)

- **Oracle:** "Exile target creature you own, then return it to the battlefield under your control.
  The Ring tempts you."
- **Decision:** Gap 14 was already obsolete — the flicker composes from existing primitives. The
  in-set **Meneldor, Swift Savior** does the identical "exile creature you own, return under your
  control" via `Effects.Move(EXILE).then(Move(BATTLEFIELD))`. Because the target is restricted to a
  creature *you own*, returning it under its owner's control = under your control. Add the
  `Effects.TheRingTemptsYou()` rider. No new effect/executor; no dedicated scenario test (snapshot
  net + the proven primitive cover it).
- **Composition:** `TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.ownedByYou()))`
  + `Effects.Composite(Move EXILE, Move BATTLEFIELD, TheRingTemptsYou)`.

### Dreadful as the Storm (Blue) — Gap 13, set base P/T (facade only)

- **Oracle:** "Target creature has base power and toughness 5/5 until end of turn. The Ring tempts you."
- **Decision:** Gap 13 was engine-landed already — `SetBasePowerToughnessEffect` and its registered
  `SetBasePowerToughnessExecutor` exist; only the DSL facade was missing. Added
  `Effects.SetBasePowerAndToughness(power, toughness, target, duration)` (mirrors `SetBasePower`).
  Card composes it (5/5, EndOfTurn) with `Effects.TheRingTemptsYou()`. This also partially unblocks
  Frodo, Sauron's Bane. Scenario test asserts base P/T 5/5.
- **Triage note:** the parallel Explore triage over-claimed composability — Elrond (needs an
  "Nth ability resolution this turn" condition), Dúnedain Rangers (needs a player-level "you control a
  Ring-bearer" condition), Frodo Baggins (needs a conditional "must be blocked" static), Ringwraiths
  (graveyard-functional trigger) all genuinely need new primitives. The TODO gap list is largely
  ACCURATE; only Gap 14 (flicker) and Gap 13 (set base P/T, facade) were obsolete. **Do not trust a
  COMPOSABLE verdict without grepping the named primitive.**

### Battle-Scarred Goblin (Red) — Gap 15 (deal damage to each blocker)

- **Oracle:** "Whenever this creature becomes blocked, it deals 1 damage to each creature blocking it."
- **Decision:** the group-damage primitive `Patterns.Group.dealDamageToAll(n, GroupFilter)` and the
  `Triggers.BecomesBlocked` trigger already existed; the only gap was a *source-relative* "blocking
  THIS creature" filter (the existing `IsBlocking` matches any blocker in combat, which would wrongly
  hit blockers of other attackers). Added `StatePredicate.IsBlockingSource` (mirrors the existing
  source-relative `InSameBandAsSource`): a blocker whose `BlockingComponent.blockedAttackerIds`
  contains `PredicateContext.sourceId`. Plus filter builder `GameObjectFilter.Creature.blockingSource()`.
  `ForEachInGroup`→`findMatchingOnBattlefield` already threads the effect's sourceId into the
  predicate context, so it resolves correctly. Card = `Triggers.BecomesBlocked` +
  `Patterns.Group.dealDamageToAll(1, GroupFilter(Creature.blockingSource()))`.
- **Touched (exhaustive-when fan-out):** `StatePredicate.kt` (+predicate), `ObjectFilter.kt`
  (+`blockingSource()`), `PredicateEvaluator.kt` (real eval), `AffectsFilterResolver.kt` (inert/false
  in projection), `BeginningPhaseManager.kt` + `TriggerMatcher.kt` (no-constraint lists), card +
  `BattleScarredGoblinScenarioTest` (proves only its blockers die), SDK reference.
- **Reusable for:** any "becomes blocked → affect each blocker" card.

