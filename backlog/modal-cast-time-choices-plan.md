# Modal Spell Cast-Time Mode & Target Selection — Implementation & Test Plan

Companion to [`modal-cast-time-choices.md`](modal-cast-time-choices.md).

## Summary

Fix MTG rules 601.2b–c / 700.2 compliance for modal spells with `chooseCount > 1`. Today those modes (Brigid's Command, Sygg's Command, future Commands/Charms/Escalate/Spree) are resolved at resolution time via `ModalContinuation`/`ModalTargetContinuation`, which means opponents pass priority blind. Move the decision to cast time while keeping the resolution-time path intact for modal triggered abilities (603.3c). Also ship the four backlog fixes: choose-1 illegal-mode filtering (700.2a), chosen-mode propagation for spell copies (700.2g), `allowRepeat` (700.2d), and `minChooseCount` (Austere-Command). Add opponent visibility for chosen modes and per-mode targets on the stack.

---

## Current-state verification

All the backlog's claims check out against head, with refinements:

1. **`CastSpell.chosenMode` really is a single `Int?`.** `rules-engine/.../core/GameAction.kt:65` — one field, no `chosenModes` list, no `modeTargets` map.

2. **`CastSpellEnumerator` choose-N is a fall-through.** `rules-engine/.../legalactions/enumerators/CastSpellEnumerator.kt:368–525` gates the modal block on `modalEffect.chooseCount == 1`. Any `chooseCount != 1` falls through to the normal `targetReqs.isNotEmpty()`/no-targets branches (lines 527+), which read `cardDef.script.targetRequirements` — that's empty for a Command because the targets live per-mode on `Mode.targetRequirements`. Result: choose-N commands enumerate as a plain `CastSpell` action with no targets and no mode info.

3. **`CastSpellHandler.execute` on line 1237** forces `chosenModes = if (action.chosenMode != null) listOf(action.chosenMode) else emptyList()` → choose-N always reaches the stack with `chosenModes = emptyList()`.

4. **`ModalEffectExecutor` (lines 44–53)** only uses the pre-chosen single mode — `spellOnStack.chosenModes.first()` — then falls back to the resolution-time `ModalContinuation` path for everything else, including triggered abilities and choose-N. That is the wrong behavior for choose-N spells (our target here) but the correct behavior for modal triggered abilities (which must keep using it — 603.3c).

5. **`SpellOnStackComponent` has `chosenModes: List<Int>` already** (`StackComponents.kt:20`), **but no `modeTargets` and no per-mode damage distribution** — so even if we start emitting multiple entries in `chosenModes`, there is nowhere to park the per-mode `ChosenTarget`s.

6. **`TargetsComponent` on the stack is a flat list** (`StackComponents.kt:100`) with a flat `targetRequirements` list. There's no mapping from `List<ChosenTarget>` to "mode index N used ChosenTarget at position K". The `processChosenModeQueue` helper (line 124 of `ModalAndCloneContinuationResumer.kt`) re-finds legal targets from scratch at resolution, which works today because targets are chosen at resolution — once cast-time, we need those bindings to survive onto the stack.

7. **`ModalEffect` / `Mode` SDK** (`mtg-sdk/.../scripting/effects/CompositeEffects.kt:88`, 136). `ModalEffect` has only `modes` and `chooseCount`. `Mode.additionalManaCost`/`additionalCosts` exist but have no test coverage for choose-N cast-time flow. There's no `allowRepeat` and no `minChooseCount`.

8. **Copy paths.** `ChainCopyExecutor`, `StormCopyEffectExecutor`, `CopyTargetSpellExecutor` create `TriggeredAbilityOnStackComponent` copies that do not carry `chosenModes` at all. This is the 700.2g gap.

9. **Client.** `ClientStateTransformer.kt` renders `chosenModes.first()`'s description for choose-1 (line 890) — that's the entire mode-awareness in the DTO. `ClientDTO.kt:167` carries `targets: List<ClientChosenTarget>` on a stack item but no mode-index breakdown. `ClientDTO.kt:209` has `stackText: String?` — one line of runtime text.

10. **Existing tests.** There are **no** Kotlin tests for BrigidsCommand or SyggsCommand. Risk is only on resolution-time mode picking for other effects.

11. **`CastWithCreatureTypeContinuation` is the cast-time pattern to mirror.** It pauses mid-execute, asks a `ChooseOptionDecision` (phase `CASTING`), resumes by calling `stackResolver.castSpell(…)`, runs triggers, and restores priority (`ColorAndTypeContinuations.kt:171` + resumer `ModalAndCloneContinuationResumer.kt:776-831`). New modal cast-time continuation must follow the same shape.

12. **Re-validation at resolution.** `TargetsComponent.targetRequirements` is consulted on resolution for 608.2b fizzle. Currently flat and modeless; we must carry per-mode requirements through so a choose-N spell can fizzle per-mode (it already fizzles per-mode inside `processChosenModeQueue` line 194, but only for resolution-time picks).

---

## Implementation phases

### Phase 1 — SDK: extend `ModalEffect` / `Mode` [DONE]

File: `mtg-sdk/.../scripting/effects/CompositeEffects.kt`

- Add to `ModalEffect` (line 136):
  - `val minChooseCount: Int = chooseCount` — for "choose one or both" (Austere Command). Default keeps current behavior (exactly `chooseCount`).
  - `val allowRepeat: Boolean = false` — for 700.2d Escalate-style reuse.
- Add DSL hooks: update `modal { chooseCount = N; minChooseCount = 1; allowRepeat = true; mode("…") { … } }` builder.
- Update `ModalEffect.description` to render "Choose one or both —" / "Choose one or more —" when `minChooseCount < chooseCount`, and "You may choose the same mode more than once." when `allowRepeat`.
- Update `applyTextReplacement` to preserve new fields on copy.

Why: prereq for both `minChooseCount` tests (Austere Command) and `allowRepeat` tests (Escalate). The fields must exist before the enumerator/handler can honor them.

Risks: Serialization round-trip — `ModalEffect` is already `@Serializable` and all fields have defaults, so existing card JSON still parses. Add a SDK serialization round-trip test covering both new fields.

### Phase 2 — Engine core: extend `CastSpell` and `SpellOnStackComponent` [DONE]

File: `rules-engine/.../core/GameAction.kt` (lines 51–67).

- Deprecate `chosenMode: Int?` (keep for one release for JSON backwards compat) and add:
  - `val chosenModes: List<Int> = emptyList()`
  - `val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList()` — aligned 1:1 with `chosenModes`. This is the only shape that cleanly handles `allowRepeat` (same mode index appearing twice with different targets).
  - `val modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap()` — future-proof for per-mode `DividedDamageEffect`.
- Normalization rule: any place that reads `chosenMode` must delegate to a helper `action.resolvedChosenModes(): List<Int>` that returns `chosenModes.ifEmpty { chosenMode?.let { listOf(it) } ?: emptyList() }`.

File: `rules-engine/.../state/components/stack/StackComponents.kt`

- `SpellOnStackComponent` already has `chosenModes: List<Int>`. Add:
  - `val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList()`
  - `val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap()` — needed at resolution for 608.2b per-mode re-validation.
  - `val modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap()`

**Canonical choice**: `modeTargetsOrdered: List<List<ChosenTarget>>` aligned with `chosenModes`. Flush `chosenMode: Int?` and any `modeTargets: Map<...>` from the long-term design. Helper still parses old payloads.

Risks: `SpellOnStackComponent` is serialized over the wire. Existing saved games / in-flight serializations will deserialize fine because `modeTargetsOrdered` defaults to empty. Add a dedicated serialization round-trip test with `chosenModes = [0, 0], modeTargetsOrdered = [[Permanent(a)], [Permanent(b)]]`.

### Phase 3 — `CastSpellEnumerator` rewrite for choose-N [DONE]

File: `rules-engine/.../legalactions/enumerators/CastSpellEnumerator.kt`

1. **Extract the single-mode legality check into a helper** `computeModeCostAndTargets(state, playerId, modeIndex, mode, baseEffectiveCost, cachedSources, …): ModeEnumeration?` that returns per-mode cost/affordability/targets or null. Preserve 100% of the current logic. Prep, no behavior change.

2. **For `chooseCount > 1`**: emit a **single** `LegalAction` of type `CastSpellModal` whose `action` is a `CastSpell` with no `chosenModes` yet, and an attached `modalEnumeration` payload describing:
   - per-mode cost deltas (`additionalManaCost`, `additionalCosts`)
   - per-mode legal targets (pre-filtered per 700.2a)
   - `chooseCount`, `minChooseCount`, `allowRepeat`
   - which modes are currently *unavailable* (no legal targets, insufficient mana for `additionalManaCost`)

   Modes flagged unavailable must NOT be pickable. This is the 700.2a enforcement for choose-N.

   Add a new field to `LegalAction` — `modalEnumeration: ModalLegalEnumeration? = null`. The client uses it to drive the cast-time decision flow. Without it, this is Option B (cartesian explosion) and will blow up for Escalate's `allowRepeat` (unbounded).

3. **Fix choose-1 700.2a** (backlog Fix 1): The existing `continue` at line 458 already skips modes with no legal targets. Claim: already compliant for choose-1 — add a regression test.

Risks: `LegalAction` shape change is a client-visible DTO. Must update `ClientStateTransformer`'s legal-action mapping and `web-client`.

### Phase 4 — `CastSpellHandler` + new `CastModalContinuation` [DONE]

File: `rules-engine/.../handlers/actions/spell/CastSpellHandler.kt`

1. In `validate` (line 108) and `execute` (line 741):
   - Compute `resolvedModes = action.resolvedChosenModes()`.
   - If the card's `spellEffect is ModalEffect modal`:
     - If `resolvedModes.isEmpty() && modal.chooseCount > 1` → **pause for cast-time mode selection** via new `CastModalContinuation`. This must happen early in `execute`, before cost payment (per 601.2b before 601.2f).
     - If `resolvedModes` non-empty but fails `minChooseCount <= size <= chooseCount`, reject in validate.
     - If any mode index appears twice and `!modal.allowRepeat`, reject in validate.
     - Validate `modeTargetsOrdered.size == resolvedModes.size`.
     - Validate per-mode targets against `modes[i].targetRequirements`.
     - Aggregate `additionalManaCost` across chosen modes (sum strings → `ManaCost.parse`).
     - Aggregate `additionalCosts` across chosen modes — they **stack** (700.2h). Extend `resolveAdditionalCostsForMode` (line 507) to union lists from all chosen modes.
   - Replace `mode?.targetRequirements ?: emptyList()` at line 319 with the union of all chosen modes' target requirements for resolution-time re-validation.

2. **New continuations** — add to `rules-engine/.../core/ModalCastContinuations.kt`:

```kotlin
data class CastModalModeSelectionContinuation(
    override val decisionId: String,
    val cardId: EntityId,
    val casterId: EntityId,
    val modal: ModalEffect,
    val baseCastAction: CastSpell,
    val selectedModeIndices: List<Int>,
    val availableIndices: List<Int>?,           // null if allowRepeat=true
    val pendingModeTargetsOrdered: List<List<ChosenTarget>>
) : ContinuationFrame

data class CastModalTargetSelectionContinuation(
    override val decisionId: String,
    val cardId: EntityId,
    val casterId: EntityId,
    val modal: ModalEffect,
    val baseCastAction: CastSpell,
    val chosenModeIndices: List<Int>,
    val resolvedModeTargets: List<List<ChosenTarget>>,
    val currentModeOrdinal: Int
) : ContinuationFrame
```

3. **Resumer** — `rules-engine/.../handlers/continuations/CastModalContinuationResumer.kt`:
   - `resumeCastModalModeSelection`: appends to `selectedModeIndices`; pushes another mode-selection or transitions to target selection or completes directly.
   - `resumeCastModalTargetSelection`: appends to `resolvedModeTargets`, advances ordinal. Once all targets collected, constructs finalized `CastSpell` and calls **package-private** `CastSpellHandler.executeFinalizedCast(state, action)` (NOT `actionProcessor.process` — forbidden from inside a continuation). Same shape as `CastWithCreatureTypeContinuation` resumer at line 776.

   Ordering: mode/target selection must happen **after** target legality pre-check (Phase 3 filters unavailable modes) and **before** cost payment (601.2f). Pause first; on final resume flow into existing `execute` path.

4. **Cancellation.** Place pauses before cost processing. Cancellation from first mode decision returns to pre-cast state. Cancellation from subsequent picks rolls back one step.

Files to change:
- `GameAction.kt:51` — extend `CastSpell`.
- `CastSpellHandler.kt:741` — insert modal-pause before cost processing; extract `executeFinalizedCast`.
- `CastSpellHandler.kt:1152` — replace `chosenMode != null` with `chosenModes.isNotEmpty()` and union per-mode requirements.
- `CastSpellHandler.kt:316` — same fix in `validate`.
- New: `CastModalContinuationResumer.kt` wired via `ContinuationResumerRegistry`.
- `Serialization.kt:183` — register new continuation subclasses.
- `CastSpellEnumerator.kt:369` — if `chooseCount > 1` emit the single `CastSpellModal` legal action.

### Phase 5 — `ModalEffectExecutor` update for choose-N [DONE]

File: `rules-engine/.../handlers/effects/composite/ModalEffectExecutor.kt` (line 44 onward)

Extend from "first mode only" to iterate all chosen modes:

- For each index `i` in `chosenModes`:
  - Resolve `Mode m = effect.modes[chosenModes[i]]`.
  - Resolve pre-chosen targets `modeTargetsOrdered[i]`.
  - Build an `EffectContext` where `context.targets = modeTargetsOrdered[i]` (so `EffectTarget.ContextTarget(k)` resolves inside that mode's scope).
  - **Per-mode 608.2b re-validation**: call `targetValidator.validateTargets(...)` against `modeTargetRequirements[chosenModes[i]]`. If all targets illegal, skip this mode (per-mode fizzle). If some illegal, remove them.
  - Execute the mode's effect.
  - If execution pauses (reflexive choice inside a mode), surface with a new `ModalPreChosenContinuation` that remembers remaining chosen modes to drain.

Critically: **when `chosenModes.isEmpty()` on a ModalEffect**, use the existing resolution-time `ModalContinuation` path. This preserves modal triggered abilities (603.3c).

### Phase 6 — Spell copy propagation (700.2g) [DONE]

Files:
- `rules-engine/.../handlers/effects/stack/StormCopyEffectExecutor.kt`
- `rules-engine/.../handlers/effects/stack/CopyTargetSpellExecutor.kt`
- `rules-engine/.../handlers/effects/chain/ChainCopyExecutor.kt`

**Recommended**: extend `TriggeredAbilityOnStackComponent` with `chosenModes`, `modeTargetsOrdered`, `modeTargetRequirements`; propagate from the source `SpellOnStackComponent` into the copy. Extend `ModalEffectExecutor` to read these off `TriggeredAbilityOnStackComponent` too.

```kotlin
state.getEntity(originalSpellId)?.get<SpellOnStackComponent>()?.let { orig ->
  copyAbility = copyAbility.copy(chosenModes = orig.chosenModes, modeTargetsOrdered = orig.modeTargetsOrdered)
}
```

For copies where the copy controller is allowed to re-choose targets: copy keeps `chosenModes`, resets `modeTargetsOrdered` to empty, runs `processChosenModeQueue` with a pre-fixed mode list — modes stay fixed (700.2g), targets re-chosen.

### Phase 7 — Priority / resolution-time validation plumbing [DONE]

File: `rules-engine/.../mechanics/stack/StackResolver.kt` (line 89)

- Extend `castSpell` signature with `modeTargetsOrdered: List<List<ChosenTarget>> = emptyList()` and `modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap()`.
- Store on `SpellOnStackComponent`.
- Populate `TargetsComponent` (line 152) with the **union** of all mode targets so existing "target arrow" rendering works. Per-mode breakdown lives in `SpellOnStackComponent`.
- Emit `BecomesTargetEvent` per targeted permanent in the union (line 190).

### Phase 8 — Server DTO + stack opponent visibility

Files:
- `rules-engine/.../view/ClientDTO.kt`
- `rules-engine/.../view/ClientStateTransformer.kt`

1. Add to `ClientCard` (around `ClientDTO.kt:209`):
   ```kotlin
   val chosenModeDescriptions: List<String> = emptyList()
   val perModeTargets: List<ClientPerModeTargetGroup> = emptyList()
   ```
   `ClientPerModeTargetGroup = { modeDescription: String, targets: List<ClientChosenTarget>, targetNames: List<String> }`.

2. In `ClientStateTransformer.kt` (line 877 `runtimeStackText`, line 889 modal branch):
   - Replace `chosenModes.first()` with `chosenModes.map { modes[it].description }` with dynamic-amount evaluation.
   - Build `perModeTargets` from `spellOnStack.modeTargetsOrdered` — resolve each `ChosenTarget` to `(entityId, targetName)`. Target names visible regardless of hidden-zone rules (they're on the stack).
   - `stackText` becomes a `\n`-joined concatenation for choose-N (so existing text renderer still works).

### Phase 9 — Web client

- **Stack viewer**: `web-client/src/components/game/board/StackZone.tsx` (line 319 where `stackText` is rendered). Render `chosenModeDescriptions` as a bulleted list under card name. For each mode, render target names below the mode description. Targeting arrows already render from `targets`; no change needed.
- **Cast-time mode selection UI**: existing `ChooseOptionDecisionUI.tsx` handles single-mode selection. For the sequence (pick mode 1 → mode 2 → targets for mode 1 → targets for mode 2 …), each pause is a normal `ChooseOption`/`ChooseTargets` decision — no new components needed. Verify the decision prompt shows `(X of N)` progress.
- **Modes already picked display**: show already-chosen modes in the prompt context.
- **Per-mode additional cost preview**: when picking a mode with `additionalManaCost` or `additionalCosts`, UI shows running cost via `modalEnumeration` from Phase 3.

### Phase 10 — `allowRepeat` + `minChooseCount` integration

- **`allowRepeat=true`**: in `CastModalModeSelectionContinuation`, `availableIndices` is `null` (re-initialized to `modes.indices` every step). Resumer does NOT remove chosen index. Validate rejects duplicates when `!allowRepeat`.
- **`minChooseCount < chooseCount`**: mode-selection decision offers a "Done" pseudo-option once `selectedModeIndices.size >= minChooseCount`. Reuses existing `ChooseOptionDecision` infrastructure.

### Phase 11 — Wire-up / registrations

- Register new continuations in `Serialization.kt:183`.
- Register resumer in `ContinuationHandler` alongside existing `ModalAndCloneContinuationResumer`.
- Update `LegalAction` to include `modalEnumeration: ModalLegalEnumeration?` and expose through server DTO layer.

---

## Test suite

All tests under `rules-engine/src/test/kotlin/com/wingedsheep/engine/scenarios/` using Kotest `FunSpec` + `GameTestDriver`, unless noted.

### A — Choose-N happy path

**A1: `BrigidsCommandChooseTwoHappyPath`**
- Setup: P1 hand = Brigid's Command; P1 battlefield: `Kithkin Warrior`, `Grizzly Bears`. P2 battlefield: `Goblin`. P1 has `{1}{G}{W}` mana up.
- Actions: P1 casts Brigid's Command, selects mode 2 ("+3/+3") targeting Bears, then mode 3 ("fight") targeting Bears vs Goblin.
- Asserts (before priority returns):
  - Stack has exactly one item, `SpellOnStackComponent` with `chosenModes == [2, 3]`, `modeTargetsOrdered[0] = [Bears]`, `modeTargetsOrdered[1] = [Bears, Goblin]`.
  - `TargetsComponent.targets` contains union of mode targets.
  - Priority is with **P2**.
- Actions: P2 passes, P1 passes, stack resolves.
- Asserts: Bears have +3/+3 until EOT; after fight, Goblin dies, Bears survives with 1 damage.

**A2: `SyggsCommandChooseTwoAutoSelectPlayer`**
- Setup: P1 casts Sygg's Command. Mode 3 ("target player draws") auto-selects the single opponent. Mode 4 ("tap + stun") targets P2's Angel.
- Asserts: stack item's `modeTargetsOrdered[0] = [Player(P2)]`, `[1] = [Permanent(angelId)]`.

### B — Opponent visibility / DTO tests

**B1: `OpponentSeesChosenModesAndTargetsOnStack`** (ClientStateTransformer unit test)
- Build a `GameState` with a fabricated `SpellOnStackComponent` (`chosenModes = [0, 2]`, populated `modeTargetsOrdered`).
- Transform via `ClientStateTransformer.transform(state, viewingPlayerId = opponent)`.
- Assert: resulting `ClientCard` on stack has non-empty `chosenModeDescriptions` (two items) and `perModeTargets` with correct resolved target names.

**B2: `OpponentStackViewDoesNotLeakHiddenHandInfo`** (regression)
- Card with mode targeting "card in your hand" — ensure mode description is visible but target card identity in hidden zone follows existing hidden-info rules.

### C — Counterspell integration

**C1: `OpponentCanCounterChooseTwoCommand`**
- Setup: P1 hand = Brigid's Command; P2 hand = Counterspell. Both have mana up.
- Actions: P1 casts Brigid's Command picking modes + targets at cast time. Priority to P2.
- Asserts:
  - P2's legal actions include `CastSpell` for Counterspell with valid targets including the Command's entity ID.
  - P2 casts Counterspell targeting Command.
  - Command is countered; no modes resolve.

### D — Target-legality timing

**D1: `AllTargetsBecomeIllegalBetweenCastAndResolution_Fizzles`** (608.2b modal)
- Setup: P1 casts Brigid's Command with mode 3 (+3/+3) + mode 4 (fight) targeting specific creatures.
- Action: P2 responds with Bolt killing both targets. Stack resolves.
- Asserts: Command resolves as no-op. `ResolvedEvent` fires. No `DamageEvent` or `ModifyStatsEvent`.

**D2: `SomeTargetsIllegal_LegalModesStillResolve`** (608.2b — modal partial fizzle)
- Setup: P1's Brigid's Command picks mode 3 (+3/+3 on Creature A) and mode 4 (fight B vs Goblin).
- Action: P2 kills only Creature A. Resolution.
- Asserts: mode 3 fizzles silently; mode 4 resolves — B fights Goblin.

**D3: `ModeIndependence_OneTargetDies_OtherResolves`**
- Same shape but one mode targets a player (who doesn't change); other mode's creature target gets bounced. Confirm independence.

### E — 700.2a legality filtering

**E1: `Choose1_ModeWithNoLegalTargetsNotOffered`** (Abzan-Charm style regression)
- Setup: synthetic test card `ExileOrDrawCharm` with mode 1 = "Exile target creature with power ≥ 5" and mode 2 = "Draw a card". Battlefield has only 1/1s.
- Assert: enumerator returns only the draw mode's `CastSpellMode` action.

**E2: `ChooseN_UnavailableModesNotPickable`**
- Setup: synthetic `TestTwoCommand` (choose two); mode 1 targets "creature with power ≥ 5" (none present); modes 2–4 fine.
- Assert: `modalEnumeration.unavailableIndices` contains index 0; cast-time mode-selection decision does not offer mode 1.
- Actions: P1 picks modes 2, 3. Cast proceeds.
- Asserts: state advances; mode 1 was never on decision's `options`.

**E3: `ChooseN_InsufficientManaForAdditionalCostMode`**
- Setup: test card with mode 2 having `additionalManaCost = "{5}{U}"`. P1 has only `{G}{W}` floating.
- Assert: mode 2 filtered out of cast-time decision; modes without additional costs remain.

### F — 700.2d (allowRepeat)

**F1: `AllowRepeatFalse_CannotPickSameModeTwice`**
- Setup: test card `choose 2, allowRepeat=false`.
- Assert: after picking mode 0, second-pick decision's options exclude mode 0.

**F2: `AllowRepeatTrue_CanPickSameModeTwice`**
- Setup: test card with `allowRepeat = true`.
- Actions: pick mode 0 twice with distinct targets.
- Asserts: `chosenModes == [0, 0]`, `modeTargetsOrdered.size == 2` with different targets. On resolution, mode's effect fires twice.

### G — 700.2g (copy preserves modes)

**G1: `CopySpellPreservesChosenModes`**
- Setup: P1 casts Brigid's Command picking modes 2, 3. P1 has copy effect (Twinflame-style).
- Actions: Copy the Command on stack. Resolve.
- Asserts:
  - Stack copy entity has `chosenModes = [2, 3]`.
  - No `ChooseOptionDecision` with mode options emitted during copy creation.
  - If copy given to opponent, they may re-select targets per 700.2g but modes remain fixed.
  - Both original and copy resolve, producing two instances of the mode effects.

**G2: `StormCopyPreservesChosenModes`**
- Setup: synthetic storm-modal card (or harness hack with `GrantedSpellKeywordsComponent`). Cast a spell first, then Brigid's Command with modes 2, 3 chosen.
- Assert: Storm copy's stack component carries `chosenModes = [2, 3]`, copy resolves without re-decision.

### H — 700.2h (per-mode additional costs)

**H1: `PerModeAdditionalManaCost_PickedMode_RequiresPayment`**
- Setup: synthetic modal card, mode 1 has `additionalManaCost = "{B}"`. P1 has only `{R}{G}` available.
- Assert: mode 1 unavailable in cast-time list; modes without extra cost remain.

**H2: `PerModeAdditionalCost_SacrificeRequired`**
- Setup: synthetic modal card, mode 1's `additionalCosts = [Forage]`.
- Actions: pick mode 1; on cast completion, player provides sacrifice/forage payment.
- Asserts: payment collected, spell resolves.

**H3: `SpreeStyle_MultipleAdditionalCostsStack`** (700.2h with choose-N)
- Setup: test card `Spree` choose 1-3, each mode has `additionalManaCost`. Pick modes 0, 1, 2.
- Asserts: final cost = base + sum of three additional mana costs; payment succeeds.

### I — minChooseCount

**I1: `ChooseOneOrBoth_AustereStyle_PickOne`**
- Setup: test card `modal(chooseCount = 2, minChooseCount = 1)` with two modes.
- Actions: player picks mode 0, selects "Done" before picking second.
- Asserts: `chosenModes == [0]`. Spell resolves with one mode.

**I2: `ChooseOneOrBoth_PickBoth`**
- Actions: pick both.
- Asserts: `chosenModes == [0, 1]`, both resolve.

**I3: `ChooseOneOrBoth_CannotPickZero`**
- Actions: attempt to finalize with zero modes.
- Asserts: decision rejects or "Done" not offered until `selectedModeIndices.size >= minChooseCount`.

### J — Modal triggered ability regression

**J1: `ModalTriggeredAbilityChoosesAtTriggerTime_NotCastTime`**
- Setup: Astral Slide-style trigger — "when a cycling trigger fires, choose one of N modes". Put a test trigger on battlefield and trigger it.
- Asserts:
  - Trigger goes on stack; `TriggeredAbilityOnStackComponent.chosenModes` is empty.
  - On resolution, `ModalContinuation` is pushed, `ChooseOptionDecision` (phase `RESOLUTION`) fires.
- (603.3c strict reading is a follow-up — see Open Questions.)

**J2: `ManifoldMouseStyleModalMayEffect_Unchanged`** (regression)
- Triggered `MayEffect` wrapping `ModalEffect` — ensure `outerTargets`/`outerNamedTargets` propagation into `processChosenModeQueue` still works.

### K — Cast-time decision resumption / rollback

**K1: `CancelMidModeSelection_RollsBack`**
- Setup: P1 starts casting choose-2 modal. Picks mode 0. Pauses for mode 2.
- Actions: P1 submits `CancelDecisionResponse`.
- Asserts:
  - `ContinuationStack` empty.
  - P1's hand contains the Command again.
  - No mana paid.
  - P1 has priority.

**K2: `CancelMidTargetSelection_RollsBackToModeSelection`**
- After picking both modes, during first mode's target selection, cancel.
- Assert: returns to mode 2 selection (or mode 1 — document intended UX).

### L — Serialization

**L1: `SpellOnStackComponentWithChosenModesRoundTrip`**
- Construct with `chosenModes = [0, 3, 3]`, `modeTargetsOrdered = [[a], [b, c], [d]]`, `modeTargetRequirements = {0 -> [...], 3 -> [...]}`. Serialize via kotlinx.serialization, deserialize, assert equality.

**L2: `CastSpellActionWithChosenModesRoundTrip`**
- Same for `CastSpell` action.

**L3: `CastModalContinuationsRoundTrip`**
- Round-trip both new continuation subclasses.

### M — E2E (Playwright)

Under `e2e-scenarios/tests/lorwyneclipsed/`:

**M1: `brigids-command-two-player-cast-time.spec.ts`**
- Two Playwright contexts via scenario API.
- P1 casts Brigid's Command. UI flow: click card → "Choose a mode (1 of 2)" → pick mode 2 → "Choose targets" → click target → "Choose a mode (2 of 2)" → pick mode 4 → "Choose targets" → click fighter + defender.
- Switch to P2's browser: stack overlay shows "Brigid's Command" with both mode descriptions and target names (Bears for mode 2, Bears + Goblin for mode 4).
- P2 casts Counterspell targeting the Command. Stack resolves. Command countered.

**M2: `brigids-command-partial-fizzle.spec.ts`**
- P1 casts Brigid's Command with two targeted modes. P2 Bolts one target. P1 passes, Bolt resolves, Command resolves. UI shows surviving mode's effect, fizzled mode's target unchanged (dead).

### N — Migration regression sweep

**N1: `ChooseOneCommands_StillWorkChoose1Path`** — enumerate all `ModalEffect(chooseCount = 1)` card definitions in `mtg-sets`; pick 2–3 for smoke tests.

**N2: `TriggeredModalAbility_ManifoldMouse_EndToEnd`** — keep existing test (referenced in `ModalEffectExecutor` commentary) passing.

**N3: `AbzanCharmOrEquivalent_EnumerationFiltersModes`** — find existing choose-1 card, pin current behavior.

---

## Migration / rollout notes

1. **`CastSpell.chosenMode` vs `chosenModes`.** Keep `chosenMode: Int?` for one release as JSON-only alias. Internal code reads `resolvedChosenModes()`. Remove after clients upgrade. `SpellOnStackComponent.chosenModes` is already plural — no rename there.

2. **Choose-N cards in `mtg-sets` today**: only Brigid's Command and Sygg's Command. No pre-existing Kotlin unit tests reference them. No test rewrite burden.

3. **Resolution-time mode picking** continues to work for modal **triggered** abilities. Audit other `ModalEffect` usage sites in `EffectExecutorRegistry`. The existing `processChosenModeQueue` stays as-is for triggered/activated modal paths.

4. **Storm/ChainCopy/CopyTargetSpell** now carry `chosenModes`. No existing tests assert the opposite (verified via grep).

5. **Client contract bump**: `ClientCard.chosenModeDescriptions` + `perModeTargets` are new; old clients ignore them and still render `stackText`. Keep `stackText` populated for backwards compat.

6. **Continuation stack migration**: new continuation subclasses in `Serialization.kt` are backwards-compatible (additions only). No subclass removals.

7. **ActionHandlerRegistry re-entrance**: resumer creates finalized `CastSpell` and calls **package-private** `CastSpellHandler.executeFinalizedCast` — not `ActionProcessor.process`.

8. **Additional-cost payment timing**: modes & targets (601.2b–c) must come before cost calculation (601.2f). Insert modal pause near top of `execute`.

9. **X costs + modal**: order is modes → targets → X → costs. No current card combines both; flag for future.

10. **Divided damage across modes** (`modeDamageDistribution`): field in place; no current card uses it. Leave unimplemented in execute; add a skipped feature test.

---

## Design decisions (previously open questions)

1. **603.3c for modal triggered abilities — deferred follow-up ticket.** The whole point of this work is opponent visibility, so leaving modal triggered abilities opaque eventually defeats it (Astral Slide / Brigid-on-a-trigger cases). But the fix is scoped to a separate ticket because it lives in a different code path (`TriggerProcessor` putting abilities on the stack, not `CastSpellHandler`). The `ModalEffectExecutor` refactor in Phase 5 makes the follow-up trivial once `chosenModes` is populated upstream: have `TriggerProcessor` run the same cast-time-style mode/target selection before attaching the `TriggeredAbilityOnStackComponent`. Test J1 pins the current behavior so the follow-up can flip it with minimal churn.

2. **Cancel semantics — defer all costs until after modes/targets are chosen.** The plan already routes this way: the cast-time modal pause is placed *before* cost processing in `CastSpellHandler.execute`. No partial sacrifices, no partial mana drain, no rollback machinery. Cancel = pop continuation, restore priority, done. Test K1 asserts exactly this.

3. **`LegalAction.modalEnumeration` — lazy, server-driven.** Eager cartesian enumeration explodes for Brigid's Command (C(4,2) × targets per mode = hundreds of actions) and is unbounded for `allowRepeat`. Lazy matches existing convoke / X-value / creature-type flows: one `LegalAction`, server drives the decision loop. If MCTS / AI search wants enumeration, expose a helper (`ModalLegalEnumeration.expandToConcreteActions(state): List<CastSpell>`) that expands on demand — do NOT pollute the live `LegalAction` list returned to priority-holders.

4. **Copy targeting (700.2g tail) — `processChosenModeQueue` with pre-fixed mode list.** When the copy controller is allowed to re-choose targets, they get per-mode target selection decisions in the order of the original's `chosenModes`. Modes are locked; targets are fresh. This reuses all existing resolution-time target-selection infrastructure and matches paper Magic's treatment of copies.

5. **Opponent visibility of hidden-zone targets — show mode + zone, hide card identity.** Matches paper MTG: opponents know *that* you're targeting a card in P1's hand, not *which*. In `ClientStateTransformer`, render as `"Mode 2: targets a card in P1's hand"`. `ClientPerModeTargetGroup.targetNames` is null (or `"a card in P1's hand"`) for hidden-zone targets; `modeDescription` stays public. Most real cards (discard, reveal) expose the card anyway via a separate reveal event, so this path is rare but must not leak.

---

### Critical Files for Implementation

- `rules-engine/.../handlers/actions/spell/CastSpellHandler.kt`
- `rules-engine/.../legalactions/enumerators/CastSpellEnumerator.kt`
- `rules-engine/.../handlers/effects/composite/ModalEffectExecutor.kt`
- `rules-engine/.../state/components/stack/StackComponents.kt`
- `rules-engine/.../view/ClientStateTransformer.kt`
