# Storm (702.40) — implementation correctness audit & fix plan

**Files involved:**

- `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/actions/spell/CastSpellHandler.kt` (lines 1329–1362) — where Storm is detected and pushed on the stack.
- `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/stack/StormCopyEffectExecutor.kt` — creates copies on the stack.
- `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/continuations/MiscContinuationResumer.kt` (`resumeStormCopyTarget`) — per-copy target retargeting.
- `rules-engine/src/main/kotlin/com/wingedsheep/engine/core/CardSpecificContinuations.kt` (`StormCopyTargetContinuation`) — continuation frame.
- `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/effects/StackEffects.kt` (`StormCopyEffect`) — SDK definition.
- `rules-engine/src/main/kotlin/com/wingedsheep/engine/state/components/stack/StackComponents.kt` (`TriggeredAbilityOnStackComponent`, `SpellOnStackComponent`) — how stack objects are modeled.

**Scourge Storm cards currently relying on this:** Tendrils of Agony, Dragonstorm, Wing Shards, Sprouting Vines, Mind's Desire, Astral Steel, Hindering Touch, Brain Freeze, Scattershot, Hunting Pack, Reaping the Graves, Temporal Fissure. Also: Ral, Crackling Wit (grants Storm via emblem).

**Relevant comprehensive rules:** 702.40, 707.7, 707.10, 707.12, 608.2b, 603.3b.

---

## How MTG says Storm works

From CR 702.40 (verbatim, 2026-02-27):

> **702.40a** Storm is a triggered ability that functions on the stack. "Storm" means "When you cast this spell, copy it for each other spell that was cast before it this turn. If the spell has any targets, you may choose new targets for any of the copies."
>
> **702.40b** If a spell has multiple instances of storm, each triggers separately.

Cross-referenced rules that matter for copies:

- **707.7c** — a copy of a spell copies the characteristics *and all decisions made* for it, including modes, targets, the value of X, divided damage, and additional/alternative costs paid. The copy's controller may choose new targets (Storm explicitly allows this).
- **707.7b** — if, when the copy would be put on the stack, the spell it's copying has illegal targets that the copier's controller can't replace, the copy is still put on the stack but will fizzle on resolution.
- **707.10** — "The act of copying a spell or ability is not the same as casting or activating." Copies aren't cast — they don't trigger "whenever you cast" abilities, and they don't count toward Storm for later spells.
- **707.12** — the copy is a spell (for instants/sorceries) with the same name, types, subtypes, colors, mana value — meaning it can be countered, can be the target of "target spell" effects, etc.
- **603.3b** — when the Storm spell is cast, Storm triggers and goes on the stack *on top of* the spell itself. Storm resolves first (creating copies), then the spell resolves.

---

## What our implementation does today

The flow in `CastSpellHandler.execute(...)`:

1. Capture `stormCount = spellsCastThisTurn` **before** incrementing for this spell (line 1253).
2. Increment `spellsCastThisTurn` and friends.
3. Call `stackResolver.castSpell(...)` — the spell goes on the stack as a proper `SpellOnStackComponent`.
4. If `!castFaceDown && stormCount > 0 && (cardDef.hasKeyword(STORM) || hasStormFromGrant)` and the spell has a `cardDef.script.spellEffect`, push a `TriggeredAbilityOnStackComponent` on top carrying a `StormCopyEffect(copyCount, spellEffect, spellTargetRequirements, spellName)`.

When the Storm trigger resolves, `StormCopyEffectExecutor.execute(...)`:

- For non-modal, untargeted spells → creates `copyCount` copies as `TriggeredAbilityOnStackComponent`s carrying the same `spellEffect` and inheriting the *original's* chosen modes (if any) and per-mode targets.
- For non-modal, targeted spells → prompts the controller to pick fresh targets for each copy in sequence via `StormCopyTargetContinuation` / `ChooseTargetsDecision`.
- For modal spells → bypasses retargeting entirely and inherits per-mode targets verbatim (see the comment at line 47–49 of `StormCopyEffectExecutor.kt`).
- If any target requirement has no legal targets, **skips the remaining copies** (line 115–118) rather than putting a fizzling copy on the stack.

Each copy is pushed via `stackResolver.putTriggeredAbility(...)` — so the copy lives on the stack as a triggered ability, never as a spell.

---

## Bugs / divergences from the rules

The order below is roughly by severity.

### 1. Copies are triggered abilities, not spell copies (**highest impact**)  [DONE]

Per rule 707.12 a copy of an instant/sorcery spell is itself a spell on the stack with the original's name, types, subtypes, colors, mana value, and ability text. Our copies are `TriggeredAbilityOnStackComponent` instances, so:

- **Counter target spell fails to find them.** `TargetEnumerationUtils.findValidSpellTargets(...)` filters `state.stack` by `SpellOnStackComponent`-shaped predicates. Our copies don't have that component, so Counterspell, Hindering Touch, and Mana Drain can't target them. This is a visible correctness bug against Storm decks (Tendrils copies uncounterable by conventional counters in our engine).
- **"Whenever a player casts a red spell" and similar spell-type-matching effects won't see the copies as matching spells.** (Although those are "cast" triggers — which copies shouldn't fire anyway per 707.10 — filter-on-stack continuous effects still care.)
- **Cards that check the stack for a specific spell type** (e.g., "you may cast an instant spell this turn only if there's a creature spell on the stack") can't see copies as spells.

### 2. Copies read `cardDef.script.spellEffect`, bypassing runtime text replacement  [DONE]

`CastSpellHandler.kt:1342` uses `cardDef.script.spellEffect` to build the `StormCopyEffect`, not the effect stored on the cast spell's `CardComponent.spellEffect` or its `SpellOnStackComponent`. That means any text-replacement effect that was applied to the spell *as it was cast* (Artificial Evolution, Mind Bend, Glamer Spinners-style rewrites) is not reflected on the copies.

Since `applyTextReplacement(...)` is already wired into the effect hierarchy and used elsewhere, this is a direct correctness hole for any Storm spell whose text was altered.

### 3. Cast-time decisions other than chosen modes are not propagated to copies

Per 707.7c, a copy inherits *all* decisions made for the original. Today we only copy `chosenModes` / `modeTargetsOrdered` / `modeTargetRequirements`. We drop everything else that's stored on `SpellOnStackComponent`:

| Field dropped | Affected scenario |
|---|---|
| `xValue` | Any hypothetical X + Storm spell (no printed example today, but the model is wrong) |
| `wasKicked` / `wasWarped` / `wasEvoked` | Kicker + Storm combo, warp combos |
| `sacrificedPermanents` / `sacrificedPermanentSubtypes` | Additional sacrifice costs (copies shouldn't repay the cost but must see the sacrificed entity) |
| `damageDistribution` | Divided-damage spells with Storm |
| `chosenCreatureType` | Spells like Aphetto Dredging if ever granted Storm |
| `exiledCardCount` | Chill Haunting-style variable exile costs + granted Storm |
| `beheldCards` | Behold-selected cards |
| `manaSpentWhite/Blue/...` | Mana-spent-gated triggers propagated through copies |

We should build copies by cloning the `SpellOnStackComponent` of the original (minus payment/cast-only flags), not by constructing a fresh effect from the card definition.

### 4. Modal spells with targets — copies can't re-choose per-mode targets

`StormCopyEffectExecutor.kt` lines 47–49 explicitly short-circuit: if `chosenModes.isNotEmpty()`, all copies inherit targets verbatim and the controller never gets to pick new ones. Storm's rules text ("you may choose new targets for any of the copies") makes no modal exception — for modal targeted spells (rare in Scourge, but plausible for future sets and directly reachable via Ral's granted Storm on any modal instant/sorcery), we need per-mode retargeting decisions.

### 5. No-legal-targets copies are skipped instead of fizzling on the stack

Per 707.7b, a copy that has no legal replacement for an illegal target is *still put on the stack* and fails to resolve. `StormCopyEffectExecutor.kt` lines 114–118 just return success with no state change when any requirement has zero legal targets, so:

- Downstream "whenever a copy is put onto the stack" style triggers don't fire.
- Players/opponents don't see the copy appear briefly (stack-based log).
- A spell with multiple targets where only one is illegal still gets created today via the continuation flow, but if *all* legal targets are wiped out mid-cascade of copies, the remaining copies silently vanish instead of being created-then-fizzling.

### 6. Multiple instances of Storm on the same spell trigger only once

Per 702.40b, each instance of Storm triggers separately. `CastSpellHandler.kt:1340-1341` uses `cardDef.hasKeyword(Keyword.STORM)` (boolean) and emits exactly one `StormCopyEffect`. This is edge case (no printed card has two storm instances), but it's a real gap if a future "Storm + granted Storm via emblem" stack-up happens with Ral's emblem.

### 7. Storm count 0 is short-circuited rather than triggering with a no-op copy count

`CastSpellHandler.kt:1340` requires `stormCount > 0` to create the Storm trigger. Per 702.40a the ability triggers whenever you cast the spell; it just copies zero times. Our optimization is invisible in most cases but breaks "whenever a triggered ability is put onto the stack" / "whenever you trigger an ability" effects on the first spell of a turn.

### 8. Forced retargeting UX (cosmetic, not a correctness bug)

MTG rules default is "inherit the original targets; you *may* pick new ones." We always prompt. Selecting the same target is allowed, so this is correct but cumbersome — a "keep original targets" quick action would match real-table UX.

### 9. Storm is special-cased in `CastSpellHandler`, not emitted via `TriggerDetector`

Structurally, Storm is a triggered ability — it belongs alongside other "on cast" triggers. Today it's handwired in `CastSpellHandler` and bypasses `TriggerDetector` / `TriggerProcessor`. This works but means Storm doesn't benefit from APNAP ordering, trigger batching, or pending-triggers continuations. Lower priority — the fix plan keeps this as a refactor, not a correctness fix.

### 10. Rules-engine test coverage is zero

There are no rules-engine tests for the Storm keyword at all (`StormsplitterTest.kt` is unrelated — it's the Bloomburrow *creature*, not the Storm keyword). `MindsDesireScenarioTest` lives in `game-server` but per the project memory, game-server tests don't currently compile after the DTO move refactor, so Storm is effectively untested in routine CI.

---

## Fix plan

### Phase 1 — Put copies on the stack as spell copies, not triggered abilities [DONE]

This is the single biggest change. The goal: when `StormCopyEffect` resolves, each copy goes on the stack as a `SpellOnStackComponent` (with a fresh `EntityId`, same `CardComponent` characteristics as the original, a `CopyOfComponent` marker, and its own `TargetsComponent`).

1. Introduce a `StackResolver.putSpellCopy(state, source, overrides)` helper that clones the original's `CardComponent` + `SpellOnStackComponent` onto a new entity, strips payment-specific flags (mana spent colors stay, but payment events don't re-fire), and sets `CopyOfComponent(originalCardDefinitionId, copiedCardDefinitionId)`.
2. Change `StormCopyEffectExecutor.createAllCopiesNoTargets(...)` and the `StormCopyTargetContinuation` resumer to call `putSpellCopy` instead of `putTriggeredAbility`.
3. Update `SpellResolver` / `StackResolver.resolveTopOfStack(...)` so resolving a spell-copy entity runs the stored effect (already true for regular spells since effects live on `CardComponent.spellEffect`). Copies cease to exist on leaving the stack per 112.3b (handled in `resolveNonPermanentSpell` and `fizzleSpell`).
4. Make sure `CounterEffect` / `findValidSpellTargets` can target copies — they already filter by `SpellOnStackComponent`, so once step 1 is done this works automatically.
5. Emit a distinct `SpellCopiedEvent` (not `SpellCastEvent`) when a copy is put on the stack, so "whenever you cast" triggers do not match (707.10).

### Phase 2 — Clone the cast spell's runtime state, not its `cardDef`

1. In `CastSpellHandler`, change the line `val spellEffect = cardDef.script.spellEffect` to read the cast spell's runtime effect. Since step 3 of Phase 1 makes copies reuse the original's `CardComponent` directly, this reduces to "don't extract `spellEffect` at all — clone the component."
2. `StormCopyEffect` becomes thinner: instead of carrying a snapshot of `spellEffect` and `spellTargetRequirements`, carry only `originalSpellEntityId`, `copyCount`, and the controller who gets to retarget. The executor reads everything else from the stack.
3. Retire `spellEffect` / `spellTargetRequirements` fields on `StormCopyEffect` in a follow-up once nothing else reads them.

### Phase 3 — Propagate every cast-time decision [DONE]

1. When creating a copy via `putSpellCopy`, copy these fields from the original `SpellOnStackComponent`: `xValue`, `wasKicked`, `wasWarped`, `wasEvoked`, `sacrificedPermanents`, `sacrificedPermanentSubtypes`, `damageDistribution`, `chosenCreatureType`, `exiledCardCount`, `beheldCards`, `manaSpentWhite/Blue/Black/Red/Green/Colorless`, `castFromZone`.
2. `chosenModes` / `modeTargetsOrdered` / `modeTargetRequirements` carry over by default (already handled today; fold into the clone).
3. Skip fields that are payment-specific and shouldn't re-trigger (e.g., don't re-emit `ManaSpentEvent` for copies — mana is only spent once per 707.10).

### Phase 4 — Per-mode retargeting for modal spells

1. In `StormCopyEffectExecutor`, when the source has non-empty `chosenModes`, build a sequence of `ChooseTargetsDecision`s — one per mode that has targets — instead of inheriting verbatim.
2. Reuse `StormCopyTargetContinuation` but extend it with a mode cursor, or introduce `StormCopyModalTargetContinuation` if it keeps the control flow simpler.
3. After the final mode's targets are chosen for a copy, put that copy on the stack and move to the next copy.

### Phase 5 — Illegal targets → copy still created

1. When a target requirement has no legal targets, still call `putSpellCopy` with the chosen-targets list populated with whatever we can fill; leave the impossible slots empty. On resolution, Rule 608.2b will fizzle it.
2. Remove the early-return at `StormCopyEffectExecutor.kt:114-118`.

### Phase 6 — Support multiple instances of Storm

1. Replace the boolean `cardDef.hasKeyword(Keyword.STORM)` check with a count-based lookup (extend the keyword container or use `cardDef.keywordAbilities.count { it.keyword == Keyword.STORM }` plus `hasStormFromGrant` multiplicity).
2. Emit N independent `StormCopyEffect` trigger abilities instead of one, each carrying the same copy count. They stack in APNAP order per standard triggered-ability rules.

### Phase 7 — Storm always triggers, even at count 0

1. Remove `stormCount > 0` from the guard at `CastSpellHandler.kt:1340`. Always push the Storm trigger; let `StormCopyEffectExecutor` no-op when `copyCount == 0` (it already does at line 37).
2. This is a one-line fix; the reason to do it is to surface the trigger to "whenever an ability triggers" effects.

### Phase 8 — Route Storm through TriggerDetector (refactor, not correctness)

1. Add a real `Triggers.OnCastSelf` hook for the Storm trigger: let `TriggerDetector.detectSpellCastTriggers(...)` see Storm keyword on the casting card and queue a trigger with `TriggerProcessor`.
2. Remove the hand-wired push in `CastSpellHandler`.
3. Optional / lower priority — wait until Phases 1–7 land and are tested.

### Phase 9 — UX "keep original targets" affordance

1. In the `ChooseTargetsDecision` for Storm copies, include the previous copy's targets as a pre-selected default so the client can render a "keep these" one-click option. Strictly UX — no engine change beyond populating a field on the decision.

---

## Tests to add

All tests live under `rules-engine/src/test/kotlin/com/wingedsheep/engine/scenarios/` unless noted, and use `GameTestDriver` + `TestCards` (the rules-engine test harness — not `ScenarioTestBase`, which is a game-server fixture). Any cards needed that aren't in `TestCards` can be created inline via `CardDefinition.creature(...)` or `card(...) { ... }` and registered via `driver.registerCards(...)`, per the project memory note "ScenarioTestBase only has set-registered cards."

For Storm-keyword tests, register the specific Scourge cards being tested:

```kotlin
val driver = GameTestDriver()
driver.registerCards(TestCards.all + listOf(TendrilsOfAgony, BrainFreeze, TemporalFissure))
```

### `StormBasicCountTest.kt`

- Cast one non-Storm spell, then cast Tendrils of Agony → Storm trigger on stack → 1 copy created.
- Storm count resets at start of turn (cast two cantrips turn 1, pass, cast Storm spell turn 2 → 0 copies).
- Cast Storm spell with nothing else cast this turn → Storm trigger still placed on stack, resolves as no-op (assert `spellsCastThisTurn` includes exactly the Storm spell, and assert trigger fired by scanning events — this is the Phase 7 test).
- Opponents' spells count toward your Storm count (have P2 cast an instant during P1's turn via Flash or a priority pass, then P1 casts Storm spell).

### `StormCopiesAreSpellsTest.kt` (Phase 1)

- Cast two non-Storm spells + Brain Freeze with 2 copies produced. Opponent Counterspell on a copy → copy goes to graveyard, other copy resolves, original resolves.
- Cast Tendrils of Agony (1 copy); opponent casts Counterspell targeting the copy → assert target enumeration for Counterspell includes the copy's entity ID.

### `StormCopyRetargetingTest.kt`

- Cast Tendrils of Agony with 2 copies (3 spell-events total: original + 2 copies); choose a different player for each copy; verify each player's life totals shift by the right amount.
- Single-copy: pick the same target as the original → resolves with that target.

### `StormCopyInheritsAllDecisionsTest.kt` (Phase 3)

- Build a synthetic "X + Storm" test card via `card("X Bolt") { manaCost = "{X}{R}"; keywords(STORM); spell { effect = Effects.DealDamage(Values.X, target("", Targets.Any)) } }` cast with X=3 after one other spell. Assert the single copy deals 3 damage, not 0.
- Build a synthetic "Exile additional card + Storm" test card exiling `exiledCardCount` cards from hand; assert the copy's effect sees the same `exiledCardCount` as the original.
- Build a "mana-spent" Storm test: spell whose effect reads `manaSpentRed`, cast after paying RR. Assert the copy sees `manaSpentRed = 2`.

### `StormModalRetargetingTest.kt` (Phase 4)

- Build a modal Storm test card: `Effects.Modal(chooseOne, mode A "deal 2 to any target", mode B "you gain 3 life")`. Cast with mode A chosen targeting opponent, with 1 copy; on copy retarget prompt, pick yourself. Assert opponent lost 2 and caster also lost 2 (self-targeted).

### `StormIllegalTargetFizzleTest.kt` (Phase 5)

- Cast Temporal Fissure (return target permanent to hand) with 1 copy scheduled; before the copy is put on the stack, wipe the only legal target (e.g., opponent's lone permanent bounced by the original first). Assert the copy is still created on the stack and then fizzles, producing a `SpellCountered`/`SpellFizzled` event with reason `no legal targets`.

### `StormMultipleInstancesTest.kt` (Phase 6)

- Grant Storm via Ral's emblem to a card that already has Storm (e.g., Tendrils); cast it after 2 other spells. Assert two Storm triggers fire (total 4 copies), not one.

### `StormCopiesDoNotCountTest.kt` (rule 707.10)

- Cast spell A, then cast Tendrils of Agony (1 copy from A). Then cast Brain Freeze. Storm count for Brain Freeze should be 2 (A + Tendrils), **not** 3 — the Storm copy of Tendrils must not bump `spellsCastThisTurn`.
- Cast Brain Freeze first with 3 spells already in the log of spells cast this turn → 3 copies. None of the copies should bump `spellsCastThisTurn`.

### `StormWithTextReplacementTest.kt` (Phase 2)

- Cast Artificial Evolution naming "Elf" → "Goblin" on a Storm creature-search spell (use Dragonstorm as the test target, or build a synthetic creature-search + Storm card). Assert that the copy's search effect filters by the replaced subtype, not by the card definition's original subtype.

### `StormGrantedByEmblemTest.kt`

- With Ral, Crackling Wit's emblem active (grants Storm to instants/sorceries), cast a non-Storm instant after 2 other spells. Assert 2 copies created.
- Without the emblem, the same spell casts normally with no Storm trigger.

### `StormCountedSpellsInclusionTest.kt`

- Countered spell still counts (cast spell A, counter it with spell B, cast Tendrils → count = 2).
- Free-cast spells count (Mind's Desire's exile-and-cast-for-free: the free cast should count toward later Storm spells).
- Lands played do NOT count (play 2 lands, cast Tendrils → 0 copies).
- Activated abilities do NOT count (tap 2 mana abilities, cast Tendrils → 0 copies).

### `StormStackOrderTest.kt` (rule 603.3b)

- Cast a Storm spell with 1 copy. Assert stack order: top → Storm trigger (resolves first creating copy), next → Storm copy (resolves second), bottom → original spell (resolves last).

---

## Out of scope (non-goals for this plan)

- Dragonstorm optimizations. Dragonstorm's per-copy library search already works under the current implementation via `EffectPatterns.searchLibrary(...)`; once Phase 1 lands, each copy is a spell on the stack and the search pipeline runs per-copy.
- Game-server integration tests (`MindsDesireScenarioTest` and the like). Those live in a module whose tests don't currently compile per project memory; re-enabling them is tracked separately.
- Web client UI for showing copies on the stack. Once copies are `SpellOnStackComponent`-backed, `ClientStateTransformer` already handles spell stack items — at most a `CopyOfComponent` badge on the stack zone needs adding.

---

## Suggested ordering

1. Phase 10 (tests) written against current behavior first — most will fail, documenting the bugs.
2. Phase 1–3 (structural: copies are spells, clone runtime state) — biggest correctness win.
3. Phase 4–6 (modal retarget, fizzle-on-stack, multi-instance) — additional correctness.
4. Phase 7 (count-0 trigger) — one-liner.
5. Phase 8 (route through TriggerDetector) — refactor, optional.
6. Phase 9 (UX) — polish.
