# Modal Spell Cast-Time Mode & Target Selection

## Problem

Per MTG rules 601.2b–c, **all** mode and target choices for modal spells happen at cast time, before the spell goes on
the stack. The opponent sees exactly which modes and targets were chosen, then decides whether to respond.

The engine currently handles this correctly for `chooseCount == 1` — the `CastSpellEnumerator` generates one
`LegalAction` per mode with `chosenMode = modeIndex` baked into the `CastSpell` action. But for `chooseCount > 1`
(Commands, Charms with "choose two", etc.), modes and targets are deferred to **resolution time** via the
`ModalContinuation` → `ModalTargetContinuation` chain.

### What's wrong

1. **Opponent is blind** — they pass priority without knowing which modes/targets were chosen
2. **Target legality timing** — targets should be validated at cast time (601.2c), not resolution
3. **"All targets illegal" fizzle** — Rule 608.2b says a spell with all targets illegal does nothing on resolution. If
   targets aren't chosen until resolution, this check can't work correctly
4. **Counterspell decisions** — in paper Magic, seeing "Brigid's Command targeting your Bears" on the stack informs
   whether you want to counter it

### What works correctly today

- `chooseCount == 1`: mode baked into `CastSpell.chosenMode`, targets in `CastSpell.targets` — fully correct
- `chooseCount > 1` at resolution: the `ModalContinuation` / `processChosenModeQueue` / `ModalTargetContinuation` chain
  works well mechanically — just fires at the wrong time

## Relevant Rules

### Casting sequence (601.2)

- **601.2a** — Move card to the stack
- **601.2b** — "If the spell is modal, the player announces the mode choice" (at cast time)
- **601.2c** — "The player announces their choice of an appropriate object or player for each target the spell requires.
  A spell may require some targets only if a particular mode was chosen for it"
- **601.2d** — "If the spell requires the player to divide or distribute an effect among targets, the player announces
  the division"
- **601.2f–h** — Determine and pay costs
- **601.2i** — Spell is now "cast", triggers fire

All of 601.2a–d happen before costs are paid (601.2f–h) and before the spell is considered "cast" (601.2i).

### Modal rules (700.2)

- **700.2a** — "The controller of a modal spell or activated ability chooses the mode(s) as part of casting that spell or
  activating that ability. If one of the modes would be illegal (due to an inability to choose legal targets, for
  example), that mode can't be chosen." — **Engine gap: the choose-N path doesn't filter out modes with no legal
  targets before presenting them to the player.**
- **700.2b** — "The controller of a modal triggered ability chooses the mode(s) as part of putting that ability on the
  stack." — Triggered abilities are different: modes chosen when put on stack, not at cast time. The engine's
  resolution-time `ModalContinuation` path is actually correct for triggered abilities.
- **700.2c** — "If a spell or ability targets one or more targets only if a particular mode is chosen for it, its
  controller will need to choose those targets only if they chose that mode." — Targets are per-mode, not global.
- **700.2d** — "If a player is allowed to choose more than one mode, that player normally can't choose the same mode more
  than once. However, some modal spells include the instruction 'You may choose the same mode more than once.'" —
  **Engine handles the no-repeat case** (line 52 in `resumeModal` removes chosen index from available), **but has no
  support for the repeat-allowed variant** (e.g., Escalate cards).
- **700.2e** — "Some spells and abilities specify that a player other than their controller chooses a mode for it." —
  Not yet relevant but worth noting for future cards.
- **700.2f** — "Modal spells and abilities may have different targeting requirements for each mode. Changing a spell or
  ability's target can't change its mode." — Already handled: each `Mode` carries its own `targetRequirements`.
- **700.2g** — "A copy of a modal spell or ability copies the mode(s) chosen for it. The controller of the copy can't
  choose a different mode." — **Engine gap: verify that `ChainCopyExecutor` / spell copy logic reads `chosenModes`
  from `SpellOnStackComponent` and propagates them to the copy.**
- **700.2h** — "Some modal spells have one or more modes with a cost listed before the effect of that mode." — This
  covers the Spree mechanic (Outlaws of Thunder Junction). The `Mode.additionalManaCost` and `Mode.additionalCosts`
  fields already support this for choose-1, but the cast-time flow for choose-N must also handle per-mode additional
  costs.

### Resolution (608.2b)

- **608.2b** — At resolution, re-check target legality. If **all** targets for **all** instances of "target" are illegal,
  the spell does nothing (fizzles). If some targets are still legal, the spell resolves and does as much as it can —
  effects referencing illegal targets simply don't happen. For modal spells: a mode whose targets all became illegal
  is skipped, but other modes with legal targets still execute. The engine's `processChosenModeQueue` already handles
  per-mode fizzle (line 194), but this only works correctly if targets were chosen at cast time.

### Triggered abilities distinction (603.3c)

- **603.3c** — Modal triggered abilities choose modes when put on the stack (not at cast time). The existing
  resolution-time `ModalContinuation` path is correct for these — it should NOT be moved to cast time.

## Affected Cards

### Primary: choose-N spells (cast-time mode selection broken)

Any modal spell with `chooseCount > 1`. Currently implemented:

- **Brigid's Command** — choose two, 4 modes with varying targets
- **Sygg's Command** — choose two

Future cards that would hit this:

- **Cryptic Command** — choose two
- **Incendiary Command**, **Austere Command**, **Primal Command**, etc. — all Lorwyn/Morningtide Commands
- **Collective Brutality**, **Collective Effort** — Escalate (choose one or more, pay extra per mode)

### Secondary: choose-1 mode legality filtering (700.2a gap)

The choose-1 path generates one `LegalAction` per mode but doesn't filter out modes with no legal targets. Example: if
Abzan Charm mode 1 is "Exile target creature with power 3 or greater" and no such creature exists, that mode's
`LegalAction` is still emitted (with `requiresTargets = true, validTargets = []`). The client may show it as uncastable,
but the engine should not offer it at all per 700.2a.

### Tertiary: unsupported modal variants

- **"Choose one or both"** — e.g., Austere Command. Needs `minChooseCount` on `ModalEffect` (choose 1 to N).
- **"You may choose the same mode more than once"** — e.g., Escalate cards. The current `resumeModal` removes chosen
  indices from `availableIndices`, preventing repeats (700.2d). Needs a `allowRepeat: Boolean` flag.
- **"Choose one that hasn't been chosen"** — e.g., Sagas, planeswalkers with modal loyalty abilities. Different
  mechanic but uses the same "mode" vocabulary.
- **Spree** — "Choose one or more additional costs" (700.2h). `Mode.additionalManaCost` / `Mode.additionalCosts` exist
  on the SDK side but haven't been tested with choose-N cast-time flow.

## Implementation Plan

### Phase 1: Extend `CastSpell` to carry multiple modes + per-mode targets

**File: `rules-engine/.../core/GameAction.kt`**

`CastSpell` already has `chosenMode: Int?` for single-mode. Extend to support multiple:

```kotlin
data class CastSpell(
    // ... existing fields ...
    val chosenMode: Int? = null,           // keep for backwards compat (choose-1)
    val chosenModes: List<Int> = emptyList(), // new: for choose-N
    val modeTargets: Map<Int, List<ChosenTarget>> = emptyMap() // new: targets per mode index
)
```

### Phase 2: Extend `CastSpellEnumerator` for choose-N

**File: `rules-engine/.../legalactions/enumerators/CastSpellEnumerator.kt`**

Currently, the `chooseCount == 1` branch (line 369) generates one `LegalAction` per mode. For `chooseCount > 1`, the
enumerator currently falls through to a single generic `LegalAction` with no mode info.

Option A — **single action with cast-time decision sequence**: Emit one `LegalAction` that triggers a multi-step
cast-time decision flow (mode 1 → mode 2 → targets for each mode → costs → on stack). This keeps the legal action
list clean but requires a new cast-time continuation path.

Option B — **enumerate mode combinations**: Generate one `LegalAction` per valid mode combination (e.g., for "choose 2
from 4" that's C(4,2) = 6 combinations). Each combination would then need target selection. This avoids new
continuation types but explodes the action list for higher choose-counts.

**Recommended: Option A** — it mirrors how the engine already handles other cast-time decisions (X value, additional
costs, convoke creature selection). The `CastSpellHandler` already has infrastructure for pausing mid-cast.

### Phase 3: Cast-time mode selection continuation

**New continuation type or reuse of existing pattern:**

When a player casts a choose-N modal spell without pre-selected modes:

1. `CastSpellHandler` detects `chooseCount > 1` and modes not yet chosen
2. Pushes a `CastModalContinuation` (new) and pauses with `ChooseOptionDecision`
3. Player picks mode 1 → continuation accumulates, pauses again for mode 2
4. All modes chosen → handler proceeds to target selection for each mode's requirements
5. Targets collected → spell goes on stack with `SpellOnStackComponent(chosenModes=[...])` and targets bound

This is structurally similar to the existing `CastWithCreatureTypeContinuation` — a cast-time decision that must
complete before the spell reaches the stack.

### Phase 4: Extend `SpellOnStackComponent`

**File: `rules-engine/.../state/components/stack/StackComponents.kt`**

`SpellOnStackComponent.chosenModes` already exists as `List<Int>`. Add per-mode target storage:

```kotlin
data class SpellOnStackComponent(
    // ... existing fields ...
    val chosenModes: List<Int> = emptyList(),
    val modeTargets: Map<Int, List<ChosenTarget>> = emptyMap() // new: targets keyed by mode index
)
```

### Phase 5: Update `ModalEffectExecutor` to use pre-chosen modes

**File: `rules-engine/.../handlers/effects/composite/ModalEffectExecutor.kt`**

The executor already has the "mode pre-chosen at cast time" path (line 46). Extend it to handle multiple pre-chosen
modes: iterate `chosenModes`, execute each mode's effect with its corresponding pre-selected targets from
`modeTargets`. No `ChooseOptionDecision` or `ModalContinuation` needed — everything was decided at cast time.

### Phase 6: Update client for cast-time mode selection

**File: `web-client/src/components/decisions/`**

The client already has decision UI for `ChooseOptionDecision`. The cast-time mode selection will appear as the same
decision type, just during the casting flow rather than during resolution. No new UI components needed — the existing
decision overlay handles it.

### Phase 7: Server — expose mode info on stack items

**File: `game-server/.../dto/ClientStateTransformer.kt`**

Ensure stack items include the chosen modes and target names so the opponent can see "Brigid's Command — +3/+3 on
Elvish Warrior, fight Warrior vs Bears" on the stack.

## Additional Fixes (can be done independently)

### Fix 1: Filter illegal modes from choose-1 enumeration (700.2a)

In `CastSpellEnumerator`, the choose-1 loop (line 369) iterates all modes. Add a pre-check: for each mode with
`targetRequirements`, call `TargetEnumerationUtils.findValidTargets()` — if the result is empty and the requirement
is non-optional, skip that mode. Small, self-contained fix.

### Fix 2: Spell copy preserves modes (700.2g)

Verify that `ChainCopyExecutor` and any spell-copy logic copies `SpellOnStackComponent.chosenModes` (and future
`modeTargets`) to the copy's `SpellOnStackComponent`. If not, add propagation. The copy should resolve with the same
modes — no new decision for the copy controller.

### Fix 3: Add `allowRepeat` to `ModalEffect` (700.2d)

Add `val allowRepeat: Boolean = false` to `ModalEffect`. When true, `resumeModal` does not remove the chosen index
from `availableIndices`. Needed for Escalate and similar "choose the same mode more than once" cards.

### Fix 4: Add `minChooseCount` to `ModalEffect`

Add `val minChooseCount: Int = chooseCount` to `ModalEffect`. For "choose one or both", set
`minChooseCount = 1, chooseCount = 2`. The mode selection UI presents a "Done" option after `minChooseCount` modes are
picked.
