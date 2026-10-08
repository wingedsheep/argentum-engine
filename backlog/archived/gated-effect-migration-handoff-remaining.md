# Handoff: migrate the *remaining* gated-effect wrappers onto `GatedEffect`

You are continuing **phase-rs Lesson 1**. The `GatedEffect` frame exists and three wrappers are
already folded in (or cleared). Your job is to migrate the wrappers that are *still* bespoke, one
per PR, deleting each wrapper's executor as you go. This document is self-contained — read it fully
before touching code. It assumes **no prior context** and supersedes the per-wrapper sections of
`gated-effect-migration-handoff.md` (that doc's §1–§3 are still a good intro; its §4 table is now
out of date — use §5 below).

> **Prerequisite.** `GatedEffect` + `Gate` + one executor/continuation/resumer live in `mtg-sdk` /
> `rules-engine`. If `GatedEffect` doesn't exist, you're on the wrong base — stop and rebase onto
> `main`.

> **Process rules (non-negotiable).**
> - Use the **`add-feature` skill** for each migration — these are load-bearing SDK/engine changes.
> - Verify any **CR rule number** before citing it (download the rules `.txt` and `grep`; don't
>   trust memory). See root `CLAUDE.md`.
> - **One wrapper per PR.** The snapshot re-bless and blast radius differ wildly per wrapper; mixing
>   them makes review impossible.
> - Don't revert/stash other agents' in-flight work. If a refactor collides, pause and report.

---

## 1. The frame (recap)

```kotlin
GatedEffect(
    gate: Gate,                 // what must succeed first
    then: Effect,               // runs iff the gate succeeds
    otherwise: Effect? = null,  // runs iff the gate fails ("if you don't" / "otherwise")
    decisionMaker: EffectTarget? = null,  // who resolves the gate (default: controller)
    descriptionOverride: String? = null,
    hint: String? = null,
)

sealed interface Gate {
    data class MayDecide(prompt: String? = null, hint: String? = null) : Gate   // pure yes/no
    data class MayPay(cost: Effect) : Gate                                       // may pay a cost effect
}
```

One executor + one continuation resumer own the **canonical resolution order** for every gate:
targets on `then`/`otherwise` lock when the ability goes on the stack (CR 603.3d); the gate is
resolved at resolution time (CR 117.3a) by `decisionMaker`; success → `then`, failure → `otherwise`.

Where the frame lives:

| Concern | File |
|---|---|
| `GatedEffect`, `Gate`, `OptionalCostEffect` / `MayPayManaEffect` facades | `mtg-sdk/.../scripting/effects/GatedEffects.kt`, `.../CompositeEffects.kt` |
| Executor | `rules-engine/.../handlers/effects/composite/GatedEffectExecutor.kt` |
| Continuation `GatedEffectContinuation` | `rules-engine/.../core/CoreContinuations.kt` (+ `core/Serialization.kt` registration) |
| Resumer `resumeGatedEffect` | `rules-engine/.../handlers/continuations/EffectAndTriggerContinuationResumer.kt` |
| Executor registration | `rules-engine/.../handlers/effects/composite/CompositeExecutors.kt` |
| Target-index walk | `mtg-sdk/.../serialization/CardValidator.kt` (`is GatedEffect`) |
| Doc | `docs/card-sdk-language-reference.md` (search "GatedEffect") |

---

## 2. Status — what's done

| Wrapper | Status | PR | Notes |
|---|---|---|---|
| `OptionalCostEffect` | ✅ done | #484 | The worked example. Facade → `Gate.MayPay`. |
| `BlightEffect` | ✅ deleted | #486 | **Was dead code** — no card/facade built it, no executor, not in any snapshot. Deleted, not lowered. |
| `TapCreatureForEffectEffect` | ✅ deleted | #486 | Same — dead code, deleted. |
| `MayPayManaEffect` | ✅ done | #488 | Facade → `Gate.MayPay(PayManaCostEffect)`. **Read §3 — it set two precedents you will reuse.** |
| `ConditionalEffect` | ✅ done | #491 | Facade → `Gate.WhenCondition(condition)` — a synchronous state-test gate (no decision/pause). Added `Effect.asConditional()` matcher (§3a); routed the 3 `is ConditionalEffect` engine sites (ClientStateTransformer stack-resolve, ActivatedAbilityEnumerator stacking, LimitedCardRater) through it. Deleted `ConditionalEffectExecutor`. 171 snapshot lowerings, pure rename. |
| `MayEffect` | ✅ done | #492 | Facade → `Gate.MayDecide` — extended that gate with `sourceRequiredZone` + `inlineOnTrigger` and ported both skip cases (source-left-zone, infeasible `ChooseActionEffect`) into `GatedEffectExecutor`'s MayDecide branch. Added `Effect.asMayDecide()` matcher (§3a, exact bare-may shape: no `otherwise`); routed the trigger machinery (`TriggerProcessor` may-then-target reorder, `resumeMayTrigger` unwrap), `ClientStateTransformer` gift-walk, `CreateDelayedTriggerExecutor` context-target resolve, `CardValidator` index walk, and `LimitedCardRater` through it. Deleted `MayEffectExecutor` (kept `MayAbilityContinuation`/`resumeMayAbility` — still produced by Reflexive/Explore executors). 121 snapshot lowerings, pure rename. |
| `IfYouDoEffect` | ✅ done | #494 | Facade → `Gate.DoAction(action, successCriterion)` — a new **action-outcome** (non-decision) gate. Added a `Gate.DoAction` branch in `GatedEffectExecutor` (pre-push pattern: snapshot → push continuation → run action → sync-pop+evaluate, or leave it for the auto-resumer if the action pauses); moved the snapshot-capture + `SuccessCriterion` evaluation out of the deleted executor into `GatedEffectExecutor`. Renamed `IfYouDoContinuation`/`IfYouDoSnapshot` → `GatedActionContinuation`/`GatedActionSnapshot` (the action-drain counterpart to the yes/no `GatedEffectContinuation`); re-pointed the `CoreAutoResumerModule` auto-resumer + `Serialization` registration. No type-keyed engine sites existed, so no `asX()` matcher was needed. Deleted `IfYouDoEffectExecutor`. 13 snapshot lowerings (`IfYouDo` → `Gated`+`Gate.DoAction`), pure rename. |
| `MayPayXForEffect` | ✅ done | #495 | Facade → `Gate.MayPayX` — a new **number-chooser** (non-yes/no) gate, a parameterless `data object` (the {X} cost is implicit). Added a `Gate.MayPayX` branch in `GatedEffectExecutor` that builds the `ChooseNumberDecision` (0..max-affordable generic mana) and **keeps the existing `MayPayXContinuation`/`resumeMayPayX`** (§3b — only the type recognition moved; the continuation auto-taps X and binds it via `DynamicAmount.XValue`). Unaffordable → `otherwise`/silent skip, mirroring the old executor. No type-keyed engine sites existed (only the executor's `effectType`), so no `asX()` matcher was needed. Deleted `MayPayXForEffectExecutor` + its `CompositeExecutors` registration. 2 snapshot lowerings (`MayPayX` → `Gated`+`Gate.MayPayX`, `effect`→`then`), pure rename. |

**Migration effectively complete.** Every wrapper whose resolution shares the frame's
single-decision-maker spine is now folded (#1–#3, #5). The two left — #4 `PayOrSufferEffect` and #6
`AnyPlayerMayPayEffect` — are deliberately kept **bespoke** because their *resolution machine*
differs, not their parameters; see the verdicts in §5. The next worthwhile step is no longer a
forced fold but, if anything, a small `PayCost → Effect` unification (§5 #4) — only if it genuinely
simplifies.

---

## 3. Two precedents from the completed migrations — reuse these

### 3a. Recognize the lowered form **by shape, not by type**

After a wrapper becomes a facade, `ability.effect is OldWrapper` no longer compiles — the compiled
form is a `GatedEffect`. Any engine path that special-cased the old type must instead recognize the
**lowered shape**. `MayPayManaEffect` did this with a single matcher that every former
`is/as MayPayManaEffect` site now keys off:

```kotlin
// rules-engine/.../handlers/effects/composite/OptionalManaPayment.kt
data class OptionalManaPayment(val cost: ManaCost, val then: Effect)

fun Effect.asOptionalManaPayment(): OptionalManaPayment? {
    val gated = this as? GatedEffect ?: return null
    if (gated.otherwise != null || gated.decisionMaker != null) return null   // exact equivalence
    val gate = gated.gate as? Gate.MayPay ?: return null
    val pay = gate.cost as? PayManaCostEffect ?: return null
    return OptionalManaPayment(pay.cost, gated.then)
}
```

When you migrate a wrapper that had bespoke engine paths keyed on its type (the `MayEffect`
may-trigger machinery is the big one), add an analogous `Effect.asX(): X?` matcher and route every
old `is/as` site through it. **Match the *exact* equivalent** (`otherwise == null`,
`decisionMaker == null`, the specific inner-cost type) so you don't accidentally capture a
different gate that happens to share the outer `GatedEffect` type.

### 3b. You can keep bespoke continuations; only the *type recognition* must move

`MayPayManaEffect` kept its entire mana-source-selection + reversed-order-trigger continuation
chain (`MayPayManaContinuation`, `MayPayManaTriggerContinuation`, `ManaSourceSelectionContinuation`,
their resumers). Those frames carry the *cost + trigger*, not the effect *type*, so they were
untouched — only the three `is/as MayPayManaEffect` recognition/unwrap sites changed, plus the
`GatedEffectExecutor` gained a branch that builds the old executor's decision for the matched shape.
**Deleting the wrapper's data class and its bespoke *executor* is the goal; you do not have to
collapse every downstream continuation into the frame** if doing so would change behavior. Delete a
continuation/resumer only once *every* path that produced it is gone.

### 3c. Identical lowered shapes become indistinguishable — decide the unified behavior

`MayPayManaEffect(cost, eff)` and `OptionalCostEffect(PayManaCostEffect(cost), eff)` now lower to the
**byte-identical** `GatedEffect`. The runtime cannot tell which facade authored a card, so the shape
gets **one** behavior (mana → source-selection UX). When you migrate a wrapper whose lowered form
could collide with an already-migrated one, find the colliding cards, pick the behavior deliberately,
and document it. A pure-lowering snapshot diff is a `OldType` → `Gated` rename; **any structural
change beyond the rename means you changed behavior — investigate before re-blessing.**

---

## 4. The mechanical recipe (per wrapper)

1. **Branch + skill.** New branch off latest `main`; invoke `add-feature`.
2. **(New gate only)** Add the `Gate` variant; add its branch in `GatedEffectExecutor.execute`, in
   `resumeGatedEffect` (or the auto-resumer for action-drain gates), in
   `CardValidator.collectIndicesRecursive` (the `when (gate)` is exhaustive), and in
   `GatedEffect.description`. Compile.
3. **Lower the wrapper:** replace the `data class` with a same-name/same-signature facade `fun`
   returning `GatedEffect`, same package. Keep the KDoc.
4. **(If it had type-keyed engine paths)** add an `Effect.asX(): X?` matcher (§3a) and route every
   `is`/`as`/`shouldBeInstanceOf` site through it. `grep -rn "\bWrapperName\b" --include=*.kt`.
5. **Delete the bespoke executor** + its `CompositeExecutors.kt` registration (and any now-dead
   continuation/resumer — but only once *every* path is migrated; audit casts first).
6. **Build incrementally:** `./gradlew :mtg-sdk:compileKotlin :rules-engine:compileKotlin :mtg-sets:compileKotlin`.
7. **Tests:** add/adjust engine scenario tests proving parity (and canonical order for new gates).
   Add a small unit test pinning any new `asX()` matcher's boundary. Update any SDK test that
   constructed the wrapper as a type.
8. **Re-bless snapshots** and eyeball the diff is a pure rename:
   ```
   ./gradlew :mtg-sets:test --tests "*CardDefinitionSnapshotTest" -DupdateSnapshots=true
   git diff mtg-sets/src/test/resources/snapshots/cards/   # confirm type/field rename only
   ```
   Sanity check: removed `"type": "OldType"` count == added `"type": "Gated"` count, and no *other*
   effect type appears in the diff.
9. **Docs:** update `docs/card-sdk-language-reference.md` in the same PR.
10. **Full sweep, then PR:** `./gradlew :mtg-sdk:test :mtg-sets:test :rules-engine:test :game-server:test`

---

## 5. The remaining wrappers

Counts are `grep` hits in `*/src/main` (blast radius ≈ snapshot goldens that re-bless). Recommended
order: the new-gate-but-synchronous one first, then the two monsters, then decide on the tail.

| # | Wrapper | → Gate | Shape | src files | Executor to delete |
|---|---|---|---|---|---|
| 1 | ~~`ConditionalEffect`~~ ✅ done | `WhenCondition(condition)` | B | ~168 | ~~`ConditionalEffectExecutor`~~ deleted |
| 2 | ~~`MayEffect`~~ ✅ done | `MayDecide` | A* | ~129 | ~~`MayEffectExecutor`~~ deleted |
| 3 | ~~`IfYouDoEffect`~~ ✅ done | `DoAction(action, criterion)` | B | ~16 | ~~`IfYouDoEffectExecutor`~~ deleted |
| 4 | `PayOrSufferEffect` | ⛔ **kept bespoke** | A/B | ~28 | (continuations in `SacrificeAndPayContinuationResumer`) |
| 5 | ~~`MayPayXForEffect`~~ ✅ done | `MayPayX` **(new)** | B | ~4 | ~~`MayPayXForEffectExecutor`~~ deleted |
| 6 | `AnyPlayerMayPayEffect` | ⛔ **kept bespoke** | B | ~6 | `PlayerExecutors` branch |

\* `MayEffect` is "Shape A" only after you extend `MayDecide`/`GatedEffectExecutor` to cover its
extra behavior (see below).

### Do **NOT** fold (identity/timing, not gate parameters)

- `BeholdEffect` — reveal/identity semantics. May *contain* a `GatedEffect` as its payoff.
- `ReflexiveTriggerEffect` — target chosen *after* the action (distinct timing axis).
- `Duration` — stays a clean field on the effects that need it.
- unless-cost (`CounterEffect` + `CounterCondition`) — already unified; leave it.

---

### #1 `ConditionalEffect` → `Gate.WhenCondition` (do this one first)

- **Synchronous gate, no pause.** Evaluate the `Condition` and run `then`/`otherwise` directly.
  Mirror `ConditionalEffectExecutor`'s use of the condition evaluator. The condition must evaluate
  identically at resolution and projection — use `ConditionEvaluationContext`, **never** a separate
  `*ProjectionCondition`.
- `ConditionalEffect` fields are `effect`/`elseEffect` → map to `then`/`otherwise`.
- This is the first **new gate** — you'll extend `GatedEffectExecutor` (a synchronous branch that
  doesn't push a `YesNoDecision`), `CardValidator`'s exhaustive `when (gate)`, and
  `GatedEffect.description`. There's no decision, so there's no resumer branch.
- Don't confuse it with `ConditionalOnCollectionExecutor` (a different effect — leave it).
- ~168 files re-bless: large but mechanical. Confirm pure rename.

### #2 `MayEffect` → `Gate.MayDecide` (biggest; extend the gate first)

`MayEffect` carries behavior the minimal `MayDecide` doesn't model yet. **Before** lowering, extend
`GatedEffectExecutor`'s `MayDecide` branch (and `MayDecide`/`GatedEffect` as needed) to cover:
- `sourceRequiredZone` — skip silently if the source left the required zone.
- `inlineOnTrigger` — must flow into `DecisionContext(inlineOnTrigger = …)`.
- `ChooseActionEffect` feasibility — `MayEffectExecutor` skips the prompt when the inner
  `ChooseActionEffect` has no feasible choices; port that.
- `decisionMaker` (supported) and `hint`.

Then handle the **may-trigger machinery** (this is the hard coupling, and where §3a pays off):
`MayTriggerContinuation` / `resumeMayTrigger` (in `EffectAndTriggerContinuationResumer.kt`) and
`TriggerProcessor` do `trigger.ability.effect as MayEffect` to unwrap a "may" *trigger* (yes/no asked
as the trigger goes on the stack, then targets — `processMayThenTargetTrigger`). That is a distinct
flow from the resolution-time `MayEffect`. Add an `Effect.asMayDecide(): …?` matcher (§3a) and route
the `is/as MayEffect` sites through it; keep the may-trigger continuations working against the gated
form exactly as `MayPayManaEffect` kept its trigger continuations (§3b). **Do not leave a half-cast
that compiles but silently mis-resolves "may" triggers.** ~129 files re-bless — the largest diff.

### #3 `IfYouDoEffect` → `Gate.DoAction(action, criterion)` (hardest mechanically)

This gate is **not decision-driven** — it runs an `action` (which may itself pause), then evaluates a
`SuccessCriterion` against a pre-action snapshot to decide `then` vs `otherwise`. Machinery to absorb:
- `IfYouDoEffectExecutor` (pre-pushes a continuation *before* the action runs),
- `IfYouDoContinuation` + `IfYouDoSnapshot`,
- the **auto-resumer** in `CoreAutoResumerModule` that fires when the action's own continuations drain
  (there is no yes/no decision to resume on).

`GatedEffectContinuation` today only handles the yes/no (decision-driven) resume. For `DoAction`
you'll need the action-drain/auto-resume path too — either extend the frame's continuation to carry
the snapshot + criterion and teach `CoreAutoResumerModule` about it, or keep a dedicated continuation
for the action-drain case while still routing through one `GatedEffectExecutor`. Keep the
`SuccessCriterion` logic (`Auto`/`CollectionNonEmpty`/`Always`) intact. Test: action did work → `then`;
action did nothing (declined / empty hand / no legal sacrifice) → `otherwise`; action paused mid-way
then completed.

### #4 `PayOrSufferEffect` — ⛔ kept bespoke (does not lower cleanly)

**Verdict: left bespoke.** "[suffer] unless you pay [cost]" *looks* like `MayPay(cost)` with
`then = null, otherwise = suffer`, but it doesn't lower cleanly:

- It stores a **`PayCost`** (8 variants: Discard/Sacrifice/PayLife/Mana/OwnManaCost/Exile/Choice/Tap),
  a cost hierarchy *distinct* from the `Effect`-typed `Gate.MayPay.cost` and **also used by Morph**.
  `PayCost.Choice` (player picks among sub-costs) and `PayCost.OwnManaCost` ("pay its mana cost")
  have **no `Effect` equivalent**.
- `GatedEffectExecutor.canAfford` only recognizes `PayManaCostEffect`/`PayLifeEffect`/`CompositeEffect`.
  Folding would mean re-implementing the whole `PayOrSufferExecutor` (per-`PayCost` affordability +
  card-selection / targeting / yes/no UX) *inside* the frame's executor, plus its two `PayCost`-keyed
  continuations — that **bloats** the frame rather than simplifying it (the opposite of the lesson).

If anyone revisits this, the only clean shape would be a dedicated `PayCost → Effect` adapter (with
real `Effect` forms for Choice/OwnManaCost) — a separate, larger refactor that should stand on its
own merits, not a wrapper fold. Until then `PayOrSufferEffect` stays a standalone effect.

### #5 `MayPayXForEffect` — ✅ done → `Gate.MayPayX`

**Done (#495).** A **number chooser** (0..max affordable), not a yes/no — modeled as a new
parameterless `Gate.MayPayX` gate kind, exactly as `Gate.DoAction` added its own resolution branch +
kept its own continuation (§3b). `GatedEffectExecutor.executeMayPayX` builds the `ChooseNumberDecision`
and reuses the existing `MayPayXContinuation`/`resumeMayPayX` to auto-tap the chosen X and bind it via
`DynamicAmount.XValue`. This composes cleanly: it removes a top-level `Effect` type + its executor,
extends the gate algebra in a principled direction, and `then`/`otherwise`/`decisionMaker` come for
free. Verified by `DecreeOfJusticeMayPayXTest` (pay / decline / unaffordable-skip).

### #6 `AnyPlayerMayPayEffect` — ⛔ kept bespoke (multi-player)

**Verdict: left bespoke.** APNAP, **multi-player**: asks each player in turn order; the
single-`decisionMaker` frame can't express the ordering (the `AnyPlayerMayPayContinuation` carries
`currentPlayerId` + `remainingPlayers` and loops). Modeling it as a gate would mean teaching the frame
a multi-player loop — out of scope for the single-decision-maker spine. Stays a standalone effect.

---

## 6. Load-bearing rules (will bite you)

- **`Effect` and `Gate` are sealed interfaces** → new `Gate` variants auto-register for kotlinx
  serialization with `@Serializable` + `@SerialName("Gate.X")`. **`ContinuationFrame` is NOT** — any
  new continuation must be added to the `polymorphic(ContinuationFrame::class)` block in
  `rules-engine/.../core/Serialization.kt`, or you get a runtime serialization error.
- **Continuations must carry targets.** Any frame that later executes an effect referencing
  `EffectTarget.ContextTarget(n)` must propagate the `EffectContext` (with `targets`/`namedTargets`),
  or the effect resolves with an empty context and silently fizzles.
- **One condition, both contexts** — conditions evaluate identically at resolution and projection
  (no separate `*ProjectionCondition`; use `ConditionEvaluationContext`). Critical for #1.
- **Pure data, no logic in SDK.** `Gate`/`GatedEffect` are serializable data bags; behavior lives in
  the executor/resumer/matcher (the matchers from §3a live in `rules-engine`, not the SDK).
- **Immutability**; **events not silent mutations**; **projected state for battlefield reads** — all
  still apply (root `CLAUDE.md`).
- **Snapshot churn is the safety net.** A pure-rename diff across N sets is the *correct* outcome of a
  clean lowering; a structural change means you changed behavior — investigate before re-blessing.
- **Update the DSL reference in the same change** (`mtg-sdk` standing rule).

---

## 7. Definition of done (per wrapper PR)

- [ ] Wrapper is a facade `fun` lowering to `GatedEffect`; no card source edits.
- [ ] Bespoke executor (+ dead continuation/resumer, if fully superseded) deleted; registration removed.
- [ ] All `is`/`as`/`shouldBeInstanceOf` sites updated — via an `Effect.asX()` matcher (§3a) if the
      type had engine-side recognition; no `as Wrapper` cast left mis-resolving.
- [ ] Scenario tests prove behavioral parity (+ canonical order for new gates) and pass; matcher
      boundary unit-tested if you added one.
- [ ] Snapshot goldens re-blessed; diff verified as a pure rename.
- [ ] `docs/card-sdk-language-reference.md` updated.
- [ ] `:mtg-sdk :mtg-sets :rules-engine :game-server` test suites green.
- [ ] CR numbers in comments/commit verified against the official rules text.
