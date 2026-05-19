# Combat Resolution Board — UX Design

Design for replacing the current sequential per-attacker combat damage modals with a single
**bipartite "combat resolution board"** that pre-computes a sensible default assignment and lets
the player confirm with one click (or drag-edit edges before confirming). Covers plain
multi-block, trample, deathtouch, multi-blocker bipartite crossover, first strike / double
strike, trample over planeswalkers, and banding (CR 702.22j/k) including the two-actor case.

## 1. Motivation

Today the combat damage flow is a chain of separate decisions:

1. `OrderObjectsDecision` for each attacker with multiple blockers (and each blocker with
   multiple attackers).
2. A per-chooser `CombatDamagePlanDecision` (recent commit `61a92bf57` bundles all of one
   chooser's attackers into one decision, but banding still emits a separate plan per chooser).
3. Repeats for first-strike step and regular step.

This is correct but UX-unfriendly:

- The player resolves combat by clicking through three or more modals for a single attack.
- The bipartite case (one blocker blocking two attackers) is invisible in a row-per-attacker
  view — the player can't see the global cost of redirecting a blocker.
- Banding (CR 702.22j/k) flips control of damage assignment to the *other* player. Today the
  engine splits this into separate decisions per chooser; nothing renders both sides on one
  board.
- Lethal-first and trample-overflow rules are enforced only at submit time, so illegal
  layouts are rejected after the fact rather than prevented during edit.

**Goal:** one screen, one logical decision, one click in the happy path.

## 2. Design

The combat damage step renders as a **bipartite graph**:

- **Attackers** on one side, with their declared targets (defender, planeswalker, battle) as
  edge endpoints.
- **Blockers** on the other side, possibly bound to multiple attackers.
- **Defender / planeswalkers / battles** as drain nodes for overflow (trample, trample over
  planeswalkers).
- Each **edge** carries a damage amount, pre-filled with the engine-computed default.
- Each edge is **editable by exactly one player** based on the rules (CR 702.22j/k flips edge
  ownership for banding).

### Visual primitives

| Element | Meaning |
|---|---|
| Edge `─N→` | Damage amount on this edge. Drag the chip to change. |
| Lethal chip `[lethal: X / Y]` | Y is current pending damage on the target; X is the threshold (toughness for creatures, loyalty for planeswalkers). Deathtouch sources show `1 = lethal` on outgoing edges. |
| Drain edge | Dimmed until preceding edges hit lethal; lights when constraint satisfied. |
| Step chip | "First Strike Damage" / "Regular Damage" at the top of the board. |
| Banding header chip | Cites CR 702.22j/k and names which edges have inverted control. |
| Marked damage chip | Shown on blockers that survived first-strike with leftover marked damage going into regular step. |

### Default assignment algorithm

Already implemented in `DamageCalculator.calculateAutoDamageDistribution`
(`rules-engine/.../DamageCalculator.kt:114`) and
`calculateBlockerDamageDistribution` (`:342`). Reuse as-is:

1. Walk blockers in declared order (`DamageAssignmentOrderComponent`).
2. Assign lethal damage to each blocker (1 for deathtouch sources, else
   `toughness − pending`), in order, until source power is exhausted.
3. For trample, overflow goes to attacked defender / planeswalker once all preceding blockers
   are at lethal. PW-trample (CR 702.19c) extends this to a second drain tier (player face).
4. Without trample (or without trample-over-PW for attacks against a planeswalker), no drain
   edge is rendered at all — the absence of the edge communicates the rule.

### Implicit assignment order

Today blocker order is set in a separate pre-step (`OrderObjectsDecision`). On the board,
**order is the row order**: drag a blocker row up or down to reorder. The response payload
carries `orderedBlockers` / `orderedAttackers` maps; the engine writes them to
`DamageAssignmentOrderComponent` / `AttackerOrderComponent` before applying damage. This
collapses two round-trips into one.

## 3. Scenarios

Each scenario shows the board state and the literal player actions. Reading convention:
`Source ─N→ Target` means "Source assigns N damage to Target".

### A — Plain multi-block

- **Active** attacks with Hill Giant (3/3).
- **Defender** blocks with Grizzly Bears (2/2) and Llanowar Elf (1/1). Order: Bears, Elf.

```
Hill Giant ─2→ Grizzly Bears   [lethal: 2 / 2]
           ─1→ Llanowar Elf    [lethal: 1 / 1]
Grizzly Bears ─2→ Hill Giant
Llanowar Elf  ─1→ Hill Giant
```

Active: **Confirm**. Defender: **Confirm**. (Total: 2 clicks.)

### B — Multi-block with trample

- **Active** attacks with Krosan Tusker (6/5, trample).
- **Defender** blocks with Grizzly Bears (2/2) and Llanowar Elf (1/1). Order: Bears, Elf.
  Defender at 20 life.

```
Krosan Tusker ─2→ Grizzly Bears   [lethal: 2 / 2]
              ─1→ Llanowar Elf    [lethal: 1 / 1]
              ─3→ Defender         [DRAIN — lit, prereqs met]
Grizzly Bears ─2→ Krosan Tusker
Llanowar Elf  ─1→ Krosan Tusker
```

Active: **Confirm**. Defender: **Confirm**.

**Variation B′ — Active wants to NOT trample.** Drag the `3` chip off the Defender drain onto
Bears. Edge becomes `─5→ Bears, ─1→ Elf, ─0→ Defender`. The drain dims. **Confirm**.

### C — Multi-block with deathtouch (no trample)

- **Active** attacks with Acidic Slime (2/2, deathtouch).
- **Defender** blocks with Spined Wurm (5/4) and Runeclaw Bear (2/2). Order: Wurm, Bear.

```
Acidic Slime ─1→ Spined Wurm     [DT-lethal: 1 ✓ / 4]
             ─1→ Runeclaw Bear   [DT-lethal: 1 ✓ / 2]
                                   ← "1 = lethal" chip on Slime's outgoing edges
Spined Wurm   ─5→ Acidic Slime
Runeclaw Bear ─2→ Acidic Slime
```

Active: **Confirm**. Defender: **Confirm**.

### D — Trample + deathtouch

- **Active** attacks with Yavimaya Wurm + Gorgon Flail (7/5, trample, deathtouch).
- **Defender** blocks with Siege Mastodon (3/5) and Runeclaw Bear (2/2). Order: Mastodon,
  Bear.

```
Wurm ─1→ Siege Mastodon  [DT-lethal: 1 ✓ / 5]
     ─1→ Runeclaw Bear   [DT-lethal: 1 ✓ / 2]
     ─5→ Defender         [DRAIN — lit, both blockers at DT-lethal]
                            ← "1 = lethal" chip on Wurm's outgoing edges
Siege Mastodon ─3→ Wurm
Runeclaw Bear  ─2→ Wurm
```

Active: **Confirm**. Defender: **Confirm**. This is the headline case where the new UX
clearly beats the old per-attacker numpad — the lethal-chip + lit drain make the "weird but
optimal" `1+1+5` split look obvious.

### E — Ironfist crossover (bipartite)

- **Active** attacks with A1 (2/2), A2 (3/3), A3 (4/4). None have trample.
- **Defender** blocks with IC1 (4/4, can block additional creature) on A1+A2, and IC2 (4/4,
  can block additional creature) on A2+A3.
- Orders: A2's blockers `[IC1, IC2]`; IC1's attackers `[A1, A2]`; IC2's attackers `[A2, A3]`.

```
            ┌─ 2 → IC1 (pending 2)
A1 (2/2) ───┘
            ┌─ 2 → IC1 (lethal-fills: 4 ✓)
A2 (3/3) ───┤
            └─ 1 → IC2 (pending 1)
A3 (4/4) ─── 4 → IC2 (lethal-fills: 5 — overkill 1, no trample)

IC1 (4/4) ──┬─ 2 → A1   (lethal ✓)
            └─ 2 → A2   (pending 2)
IC2 (4/4) ──┬─ 1 → A2   (lethal-fills: 3 ✓)
            └─ 3 → A3   (pending 3, A3 survives at 4t)
```

Active: **Confirm**. Defender: **Confirm**. Default kills A1, A2, IC1, IC2; A3 survives at
4t. This is the scenario the old per-attacker modal makes nearly impossible to reason about
— the new board surfaces the whole graph at once.

### F — Banding on the attacker (CR 702.22k)

- **Active** attacks with Benalish Cavalry (2/2, banding).
- **Defender** blocks with Hill Giant (3/3) and Grizzly Bears (2/2). Order: Hill Giant, Bears.

702.22k applies: **Active** controls blocker damage division too. 702.22j does not apply (no
banding blocker).

```
[Header: "CR 702.22k — you choose how Hill Giant and Bears divide their damage."]

Benalish Cavalry ─2→ Hill Giant       [editable by: Active]
Hill Giant       ─3→ Benalish Cavalry [editable by: Active]
Grizzly Bears    ─2→ Benalish Cavalry [editable by: Active]
```

Defender's view: same board, all edges read-only, "Waiting for opponent" indicator.

Active: **Confirm**. Defender: auto-confirmed (no editable edges).

### G — Banding on the blocker (CR 702.22j)

- **Active** attacks with Hill Giant (3/3).
- **Defender** blocks with Mesa Pegasus (1/1, banding) and Grizzly Bears (2/2). Order:
  Pegasus, Bears.

702.22j applies: **Defender** controls Hill Giant's damage division. 702.22k does not apply.

```
[Header (Defender's view): "CR 702.22j — you choose Hill Giant's damage division (banding blocker)."]

Hill Giant ─1→ Mesa Pegasus  [editable by: Defender]   ← inverted!
           ─2→ Grizzly Bears [editable by: Defender]
Mesa Pegasus  ─1→ Hill Giant [editable by: Defender]
Grizzly Bears ─2→ Hill Giant [editable by: Defender]
```

Active's view: same board, all edges read-only, "Defender is assigning…" indicator.

Defender drags `Hill Giant → Pegasus` from `1` to `0`, then `Hill Giant → Bears` from `2` to
`3` (sparing the Pegasus). **Confirm**. Active: **Confirm**.

### H — Both sides have banding (CR 702.22j + 702.22k together)

- **Active** attacks with Benalish Cavalry (2/2, banding).
- **Defender** blocks with Mesa Pegasus (1/1, banding) and Grizzly Bears (2/2). Order:
  Pegasus, Bears.

Both 702.22j and 702.22k apply. Edge ownership is fully inverted:

```
[Header (both views): "CR 702.22j + 702.22k — damage control is fully inverted."]

Cavalry  ─1→ Pegasus   [editable by: DEFENDER]
         ─1→ Bears      [editable by: DEFENDER]
Pegasus  ─1→ Cavalry   [editable by: ACTIVE]
Bears    ─2→ Cavalry   [editable by: ACTIVE]
```

Both players act simultaneously on their own halves; the board waits until both confirm.
This is the only place in the engine where two players hold open the same decision at the
same time.

### I — First strike attacker prunes a blocker

- **Active** attacks with White Knight (2/2, first strike).
- **Defender** blocks with Grizzly Bears (2/2) and Llanowar Elf (1/1). Order: Bears, Elf.

**Board 1 — First Strike Damage Step.** Only sources with FS or double strike are active;
others appear greyed.

```
[Step chip: "First Strike Damage — only first-strike creatures deal damage now"]

White Knight (FS) ─2→ Grizzly Bears   [lethal: 2 ✓]
                  ─0→ Llanowar Elf
Grizzly Bears   ─0→ White Knight   (greyed: no FS this step)
Llanowar Elf    ─0→ White Knight   (greyed: no FS this step)
```

Active: **Confirm**. Bears dies to SBAs before the regular step.

**Board 2 — Regular Damage Step.** White Knight already dealt its damage; Llanowar Elf strikes
back. The board is fully default and trivial — **auto-confirms with a brief animation**, no
click.

### J — Double strike accumulating damage across both steps

- **Active** attacks with Boros Swiftblade (1/2, double strike).
- **Defender** blocks with Grizzly Bears (2/2) and Llanowar Elf (1/1). Order: Bears, Elf.

**Board 1 — First Strike Damage Step.**

```
Swiftblade (DS) ─1→ Grizzly Bears   [pending → 1 / 2 toughness]
                ─0→ Llanowar Elf
Grizzly Bears  ─0→ Swiftblade   (greyed)
Llanowar Elf   ─0→ Swiftblade   (greyed)
```

Active: **Confirm**. Bears now has 1 marked damage going into the regular step.

**Board 2 — Regular Damage Step.**

```
[Marked-damage chip on Bears: "1 marked damage from first strike"]

Swiftblade (DS)  ─1→ Grizzly Bears   [lethal: 1 more ✓ (1 marked + 1 = 2)]
                 ─0→ Llanowar Elf
Grizzly Bears    ─2→ Swiftblade
Llanowar Elf     ─1→ Swiftblade
```

Active: **Confirm**. Defender: **Confirm**. Swiftblade and Bears die; Elf survives.

**Variation J′ — Active tries to redirect damage off Bears.** Dragging
`Swiftblade → Bears` from `1` to `0` would drop Bears below lethal (1 marked < 2 toughness).
The lethal chip flashes red; the drag snaps back. **The lethal-first rule is enforced as a
drag constraint, not as a post-submit rejection.**

### K — Trample over planeswalkers

- **Active** attacks Jace, the Mind Sculptor (3 loyalty, controlled by Defender) with
  Thrasta, Tempest's Roar (5/5, trample over planeswalkers).
- **Defender** blocks with Grizzly Bears (2/2). Order: Bears.

PW-trample introduces a **three-tier drain**: blocker → planeswalker → player face. The
player face edge only unlocks after the planeswalker has been hit for its loyalty value.

```
[Header chip: "Attacking Jace, the Mind Sculptor (3 loyalty)"]

Thrasta ─2→ Grizzly Bears   [lethal: 2 ✓]
        ─3→ Jace             [DRAIN tier 1 — lit, Bears at lethal]
                              [PW-loyalty: 3 ✓ — will kill Jace]
        ─0→ Defender         [DRAIN tier 2 — dim, locks until Jace hit for loyalty]

Grizzly Bears ─2→ Thrasta
```

With 5 power exactly, default is `2 + 3 + 0`. Active: **Confirm**.

**Variation K′ — Thrasta buffed to 10/10.** Default becomes `2 + 3 + 5`; tier-2 drain lights
up automatically once tier-1 drain hits loyalty. Confirm.

### L — Plain trample attacking a planeswalker (contrast)

- **Active** attacks Jace (3 loyalty) with Krosan Tusker (6/5, plain trample, *not* trample
  over PW).
- **Defender** blocks with Grizzly Bears (2/2).

Per CR 702.19f, no damage can go to the defending player even after Jace's loyalty is met.
**The defender drain edge is simply not rendered.**

```
[Header: "Attacking Jace — Krosan Tusker has trample, not trample over planeswalkers"]
[Defender drain not rendered]

Krosan Tusker ─2→ Grizzly Bears   [lethal: 2 ✓]
              ─4→ Jace             [DRAIN — lit; only target left for excess]
                                    [Jace at 4 damage > 3 loyalty → Jace dies]
Grizzly Bears ─2→ Krosan Tusker
```

Active: **Confirm**. Defender: **Confirm**. The *absence* of the defender drain
communicates the rule without any tooltip.

## 4. Data contract

Replace `CombatDamagePlanDecision` (`PendingDecision.kt:625-663`) with a richer
`CombatResolutionDecision` carrying the full graph.

```kotlin
data class CombatResolutionDecision(
    val id: DecisionId,
    val playerId: EntityId,         // primary chooser
    val coChooserId: EntityId? = null, // banding: who edits the inverted edges
    val firstStrike: Boolean,
    val attackers: List<ResolutionAttacker>,
    val blockers: List<ResolutionBlocker>,
    val defenders: List<ResolutionDefender>,  // players, PWs, battles attacked
    val edges: List<DamageEdge>,              // computed defaults
    val prompt: String,
    val context: DecisionContext,
) : PendingDecision

data class ResolutionAttacker(
    val id: EntityId, val name: String,
    val power: Int, val toughness: Int,
    val hasTrample: Boolean,
    val hasTrampleOverPlaneswalkers: Boolean,
    val hasDeathtouch: Boolean,
    val hasFirstStrike: Boolean,
    val hasDoubleStrike: Boolean,
    val dealsDamageThisStep: Boolean,   // false for non-FS sources on FS board
    val bandId: BandId?,
    val attackedDefenderId: EntityId,
    val blockedByIds: List<EntityId>,
    val markedDamage: Int,              // surfaces between FS and regular board
)

data class ResolutionBlocker(
    val id: EntityId, val name: String,
    val power: Int, val toughness: Int,
    val hasDeathtouch: Boolean,
    val hasFirstStrike: Boolean,
    val hasDoubleStrike: Boolean,
    val dealsDamageThisStep: Boolean,
    val blockedAttackerIds: List<EntityId>,
    val orderedAttackers: List<EntityId>, // CR 510.1d
    val markedDamage: Int,
)

data class ResolutionDefender(
    val id: EntityId,
    val kind: PLAYER | PLANESWALKER | BATTLE,
    val nameOrLifeOrLoyalty: …,
)

data class DamageEdge(
    val id: EdgeId,
    val sourceId: EntityId,         // attacker or blocker
    val targetId: EntityId,         // blocker, attacker, player, PW, battle
    val direction: ATK_TO_BLK | BLK_TO_ATK | ATK_TO_DEFENDER | ATK_TO_PW | ATK_TO_BATTLE,
    val amount: Int,                // pre-filled default
    val minimum: Int,                // lethal-first floor (1 for DT sources)
    val maximum: Int,                // capped by source power
    val isTrampleDrain: Boolean,
    val lethalThreshold: Int?,
    val editableBy: EntityId,        // which player may modify
    val unlockOrder: Int,            // drain unlocks only when lower-order edges at lethal
)

data class CombatResolutionResponse(
    val decisionId: DecisionId,
    val edges: List<EdgeAmount>,                          // (edgeId, amount)
    val orderedBlockers: Map<EntityId, List<EntityId>>,   // attacker → blocker row order
    val orderedAttackers: Map<EntityId, List<EntityId>>,  // blocker → attacker row order
)
```

The decision is a **strict superset** of `CombatDamagePlanDecision`. Per-edge `editableBy`
enables the two-actor banding case in a single payload.

## 5. Server-side work

### Rules-engine

1. **New decision + response types** in `core/PendingDecision.kt`. Keep `CombatDamagePlanDecision`
   for one release as an alias for rollback.
2. **Producer rewrite** in `CombatDamageManager.kt:212-348`. The new emitter:
   - Enumerates the same candidates as today, but emits one `CombatResolutionDecision`
     covering all attackers in the step (manual *and* auto-default attackers — auto cases
     have `editableBy = NONE` for transparency).
   - Computes per-edge `editableBy` using the existing `CombatDamageUtils.damageAssignmentChooser`
     logic (`CombatDamageUtils.kt:126-143`), applied per attacker, then merged.
   - Includes blocker→attacker edges from `BlockingComponent.blockedAttackerIds` and
     `AttackerOrderComponent.orderedAttackers`.
3. **Collapse the ordering pre-step**: skip emitting `OrderObjectsDecision` for combat from
   `BlockPhaseManager.kt:171-200`. Pre-fill default order in
   `DamageAssignmentOrderComponent` / `AttackerOrderComponent` automatically; the response
   carries the final order, populated by the client from row order at confirm time.
4. **Validation** in `DecisionValidators.kt:81`. Extend to enforce:
   - Per-attacker edge sum ≤ source power.
   - Each edge's `editableBy` matches the submitting player.
   - Lethal-first: walking `orderedBlockers`, every blocker must be at lethal (or DT-lethal)
     before any later blocker or drain edge gets non-zero damage.
5. **First-strike & double-strike**: no engine changes — `applyCombatDamage(state, firstStrike)`
   is already called twice per combat. Each call emits its own resolution decision. Marked
   damage between steps flows through existing component state.

### Game-server

- Add `CombatResolutionDecision` string in `DecisionEnricher.kt:88` and
  `SpectatorStateBuilder.kt:78`.
- AI fallback: `gameserver/ai/AiWebSocketSession.kt:320` — submit defaults for the new type.
- `ai/.../DecisionResponder.kt:63` — extend to the new decision.

### Two-actor dispatch

For the banding case (`coChooserId != null`):

- Session layer sends the pending-decision message to *both* `playerId` and `coChooserId`.
- Server accepts partial responses: a player's submission updates only edges with their
  `editableBy`. The engine holds the same decision id open until both halves have been
  confirmed.
- Implementation in `CombatContinuationResumer.resumeCombatDamagePlan`
  (`handlers/continuations/CombatContinuationResumer.kt:49-68`): track a two-key confirmation
  set; only re-enter `applyCombatDamage` once both keys are present.

## 6. Client-side work

### New components

- **`web-client/src/components/decisions/CombatResolutionBoard.tsx`** — replaces
  `CombatDamagePlanModal.tsx`. Full-screen overlay rendering the bipartite graph.
- **`web-client/src/components/decisions/board/`** (new directory):
  - `BoardLayout.tsx` — positions attackers (top row), blockers (middle), defenders / PWs
    (bottom).
  - `EdgeLayer.tsx` — SVG edges. Reuse `CombatArrows.tsx:1-665` primitives (Bezier paths,
    label badges).
  - `EdgeAmountChip.tsx` — draggable amount widget. Drag vertically to change value, with
    `−`/`+` buttons for accessibility and mobile fallback. Lethal-first enforced as a drag
    constraint: dragging below `minimum` snaps back.
  - `DrainEdge.tsx` — special styling for trample defender / PW / battle edges. Dimmed
    until prereq edges at lethal, brightens on unlock.
  - `BandingHeaderChip.tsx` — banding rules-flip explanation, citing CR 702.22j/k.
  - `StepChip.tsx` — "First Strike Damage" / "Regular Damage" indicator.
  - `MarkedDamageChip.tsx` — surfaces damage carried from first-strike step on the regular
    board.

### Reuse vs rebuild

- **Reuse:** `CombatArrows.tsx` SVG primitives, `bandColorFor` palette in
  `components/game/board/styles.ts`, `hoverCard` hook, `useResponsive`, `getCardImageUrl`,
  `OrderBlockersUI.tsx` DnD handlers for blocker row drag-to-reorder.
- **Discard after migration:** `CombatDamageAssignmentModal.tsx`, `CombatDamagePlanModal.tsx`,
  the COMBAT branch of `OrderBlockersUI.tsx`.

### Zustand

- Replace `submitCombatDamagePlanDecision` (`gameplaySlice.ts:287-303`) with
  `submitCombatResolutionDecision({ edges, orderedBlockers, orderedAttackers })`.
- Add a transient `combatBoardDraft` slice scoped to the open decision id with current edge
  amounts and row orders.
- Add selector `selectMyEditableEdges(playerId)` for read-only / interactive rendering.

### Auto-confirm trivial boards

When every editable edge is at its default and there is nothing meaningfully to choose
(Scenario I Board 2), the board shows for ~400 ms with a brief damage-flow animation then
auto-submits.

## 7. Migration / rollout

Phased rollout behind a `combatResolutionBoard` flag:

1. **Phase 1.** Add new decision and response types. Engine emits *either*
   `CombatDamagePlanDecision` (legacy) *or* `CombatResolutionDecision` (new) based on the
   flag. Add server-side validation. Update AI fallback.
2. **Phase 2.** Build the new board component and route the new decision type. Keep the
   legacy modal as fallback. Enable the flag in dev/staging.
3. **Phase 3.** Update e2e tests to drive the new board (`gamePage.confirmDamage()` helper at
   `e2e-scenarios/helpers/gamePage.ts:447-453`; add `dragDamage(attacker, target, amount)`
   for non-default cases). Flip the flag to default-on.
4. **Phase 4.** Delete legacy components and `OrderObjectsDecision`'s COMBAT branch. Remove
   the flag.

### Tests to update / add

- **Engine scenarios that must stay green:**
  - `NobleElephantScenarioTest.kt:195-282` (banded multi-blocker → single decision)
  - `TrampleCombatDamageScenarioTest.kt`
  - `HystrodonScenarioTest.kt`
- **E2E tests to rewrite:**
  - `e2e-scenarios/tests/general/trample-damage-assignment.spec.ts`
  - `e2e-scenarios/tests/general/multiple-blockers.spec.ts`
  - `e2e-scenarios/tests/general/combat-kill-blocker.spec.ts`
- **E2E gaps to fill:**
  - No banding e2e exists today — add `banding-damage-inversion.spec.ts` covering CR 702.22j
    and the two-actor gating.
  - No PW-trample e2e — add once the drain edge UI is live.
  - Multi-blocker bipartite (Ironfist Crusher case) — add e2e for the board crossover render.

## 8. Open questions

1. **Drag vs click-step UX model.** Drag feels graph-native; tap/step is friendlier on mobile
   and for a11y. Chips include both: vertical drag readout + `−`/`+` buttons. Defaults to
   drag on desktop, buttons on mobile (via `useResponsive.isMobile`).
2. **Keep blocker-order as a separate pre-step?** Folding into the damage board is cleaner
   but may surprise players expecting to commit order at block-time. Recommendation: collapse
   into the board (one step), drop the standalone order modal.
3. **Compact view for the 1v1 plain case?** A bipartite board with one attacker and one
   blocker is overkill. Detect `attackers.length === 1 && blockers.length <= 1 && no trample`
   and render a compact "Block: 2 ↔ 2 [Confirm]" strip instead. Decide: ship the compact view
   from day 1, or full board first and iterate.
4. **Scope of `AssignDamageDecision` cleanup.** Still used by `AssignAsUnblocked`
   (Maro-style Y/N) and `DivideCombatDamageFreely` (Butcher Orgg). Folding those into the
   resolution board adds scope. Recommendation: separate phase, defer.
5. **Replay / serialization compatibility.** Existing saved games serialize legacy decision
   types. Keep them deserializable (sealed-interface unions are forward-compatible if old
   variants stay) rather than writing a migration. Verify against `core/Serialization.kt`.
6. **Lethal-first as a hard drag constraint vs soft warning.** Strict snap-back prevents
   illegal layouts at edit time but may feel rigid for advanced players doing complex damage
   manipulation (e.g. interacting with damage-prevention triggers between steps). Lean
   strict; revisit if it hurts.
