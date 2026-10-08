# Handoff: migrate the rest of the gated-effect cluster onto `GatedEffect`

You are continuing **phase-rs Lesson 1** (`backlog/phase-rs-lessons.md`). The frame already
exists; your job is to migrate the *remaining* optional/gated wrapper effects onto it, one wrapper
per PR, deleting each wrapper's bespoke executor as you go. This document is self-contained — read
it fully before touching code. It assumes **no prior context**.

> **Prerequisite.** The frame landed in **PR #484** (branch `gated-effect-frame`): `GatedEffect` +
> `Gate` + one executor/continuation/resumer, with `OptionalCostEffect` already migrated as the
> worked example. Start from that branch or from `main` *after* #484 merges. If `GatedEffect`
> doesn't exist in `mtg-sdk`, you're on the wrong base — stop and rebase.

> **Process rules (non-negotiable).**
> - Use the **`add-feature` skill** for each migration — these are load-bearing SDK/engine changes.
> - Verify any **CR rule number** before citing it (download the rules `.txt` and `grep`; don't
>   trust memory). See root `CLAUDE.md`.
> - **One wrapper per PR.** The snapshot re-bless and blast radius differ wildly per wrapper; mixing
>   them makes review impossible.
> - Don't revert/stash other agents' in-flight work. If a refactor collides, pause and report.

---

## 1. What the frame is

`GatedEffect` replaces the "one wrapper type + one executor + one continuation touchpoint per
optional concern" pattern with a single composable frame:

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
    data class MayPay(cost: Effect) : Gate                                       // may pay a cost
}
```

**The point** ("composition over enumeration"): one executor + one continuation
resumer own the *canonical resolution order*, so target-locking-vs-gate-timing is correct **by
construction** for every gate instead of being re-encoded (and re-bugged) per wrapper:

1. Targets on `then`/`otherwise` are locked when the ability goes on the stack (trigger time,
   **CR 603.3d**) — independent of the gate.
2. The gate is resolved at resolution time (**CR 117.3a**) by `decisionMaker`.
3. Success → `then`; failure → `otherwise`.

### Where the frame lives (branch `gated-effect-frame`)

| Concern | File |
|---|---|
| `GatedEffect`, `Gate`, `OptionalCostEffect` facade | `mtg-sdk/.../scripting/effects/GatedEffects.kt` |
| Executor (MayPay affordability + yes/no; MayDecide yes/no) | `rules-engine/.../handlers/effects/composite/GatedEffectExecutor.kt` |
| Continuation `GatedEffectContinuation` | `rules-engine/.../core/CoreContinuations.kt` |
| Continuation serial registration | `rules-engine/.../core/Serialization.kt` (`subclass(GatedEffectContinuation::class)`) |
| Resumer `resumeGatedEffect` + registration | `rules-engine/.../handlers/continuations/EffectAndTriggerContinuationResumer.kt` |
| Executor registration | `rules-engine/.../handlers/effects/composite/CompositeExecutors.kt` |
| Target-index walk | `mtg-sdk/.../serialization/CardValidator.kt` (`is GatedEffect`) |
| Reference tests | `rules-engine/.../scenarios/GatedEffectScenarioTest.kt` |
| Doc | `docs/card-sdk-language-reference.md` (search "GatedEffect") |

---

## 2. The reference migration (study this first)

`OptionalCostEffect` → `Gate.MayPay` is already done. It is your template. Read the diff of PR #484
(`git show` the commit `Introduce GatedEffect frame…`). The moves were:

1. **Convert the wrapper data class into a facade *function* of the same name + signature**, in the
   same package, returning a `GatedEffect`. This is the key trick: card source like
   `OptionalCostEffect(cost = …, ifPaid = …)` keeps compiling unchanged (it now calls a function),
   but the *compiled/serialized* form becomes `Gated`. No card edits.
   ```kotlin
   fun OptionalCostEffect(cost: Effect, ifPaid: Effect, ifNotPaid: Effect? = null,
                          descriptionOverride: String? = null): GatedEffect =
       GatedEffect(gate = Gate.MayPay(cost), then = ifPaid, otherwise = ifNotPaid,
                   descriptionOverride = descriptionOverride)
   ```
2. **Fix the few type-level references** the lowering broke: `is OptionalCostEffect` / `as
   OptionalCostEffect` / `shouldBeInstanceOf<OptionalCostEffect>` / facade return types. (For
   OptionalCost that was `CardValidator` + `CardDslTest` + the `mayPay`/`mayPayOrElse` patterns.)
3. **Delete the bespoke executor** (`OptionalCostEffectExecutor.kt`) and its registration in
   `CompositeExecutors.kt`; the `GatedEffectExecutor` now handles the gate.
4. **Re-bless the snapshot goldens** and confirm the diff is a *pure rename* (type + field-key
   change + re-indent), nothing structural.
5. **Update `docs/card-sdk-language-reference.md`** in the same PR.
6. Add scenario tests proving parity (and, where a gate is new, the canonical order).

The wrapper's *human description* should be reproduced by `GatedEffect`/`Gate` so prompts and any
description-derived UI stay identical (note: `description` is a computed `val`, not serialized, so
it never appears in snapshots — only the structural type/field changes do).

---

## 3. Two migration shapes

**Shape A — wrapper maps to an *existing* gate** (`MayPay` or `MayDecide`): pure facade lowering as
above. No engine change beyond deleting the old executor.

**Shape B — wrapper needs a *new* gate**: first **add the `Gate` variant** + a branch in
`GatedEffectExecutor` + a branch in `resumeGatedEffect` + the `CardValidator` walk, *then* lower the
wrapper onto it. `DoAction` (for IfYouDo) and `WhenCondition` (for ConditionalEffect) are Shape B.

When you add a `Gate` variant, also extend the `when (gate)` in `CardValidator.collectIndicesRecursive`
(it's exhaustive) and in `GatedEffect.description`.

---

## 4. The remaining wrappers

Counts are `grep` hits in `*/src/main` as of this writing — they signal blast radius (= how many
snapshot goldens re-bless). Recommended order is easiest/lowest-risk first; it deviates from the
lesson's order on purpose (do the cheap, mechanical ones before the two monsters).

| # | Wrapper | → Gate | Shape | src files | Notes |
|---|---|---|---|---|---|
| 1 | `MayPayManaEffect` | `MayPay(PayManaCostEffect(cost))` | A | ~36 | Watch the **trigger-time** "may pay" variant (see §5). |
| 2 | `BlightEffect` | `MayPay(<blight pipeline>)` | A | ~2 | Tiny. Cost = the blight cost effect; `then` = inner. |
| 3 | `TapCreatureForEffectEffect` | `MayPay(<tap-a-creature>)` | A | ~2 | Tiny. |
| 4 | `ConditionalEffect` | `WhenCondition(condition)` **(new)** | B | ~168 | Synchronous gate (no pause). Big re-bless. See §5. |
| 5 | `MayEffect` | `MayDecide` | A* | ~129 | Biggest behavior surface + cast sites. See §5 — needs MayDecide extended first. Huge re-bless. |
| 6 | `IfYouDoEffect` | `DoAction(action, criterion)` **(new)** | B | ~16 | Hardest mechanically: action-drain + `SuccessCriterion`. See §5. |
| 7 | `PayOrSufferEffect` | `MayPay(cost)` with `then=null, otherwise=suffer` | A/B | ~28 | Uses `PayCost` (not `Effect`) + its own continuations. Decide if it folds cleanly. |
| 8 | `MayPayXForEffect` | new "may pay X" gate | B | ~4 | Number chooser, not yes/no. Only if the algebra extends cleanly. |
| 9 | `AnyPlayerMayPayEffect` | `AnyPlayerMayPay(cost)` **(new)** | B | ~6 | **APNAP multi-player** — the single-`decisionMaker` frame doesn't model this. Lesson says keep separate unless it folds cleanly. Likely *leave as-is*. |

\* `MayEffect` is "Shape A" only after you extend `MayDecide`/`GatedEffectExecutor` to cover its
extra behavior (see §5).

### Do **NOT** fold (per the lesson — these are identity/timing, not gate parameters)

- `BeholdEffect` — reveal/identity semantics. May *contain* a `GatedEffect` as its payoff.
- `ReflexiveTriggerEffect` — target chosen *after* the action (distinct timing axis).
- `Duration` — stays a clean field on the effects that need it.
- unless-cost (`CounterEffect` + `CounterCondition`) — already unified; leave it.

---

## 5. Per-wrapper gotchas

### `ConditionalEffect` → `Gate.WhenCondition` (do this one early)
- Synchronous: evaluate the `Condition` and run `then`/`otherwise` with **no pause**. Mirror
  `ConditionalEffectExecutor`'s use of the condition evaluator. The condition must evaluate
  identically at resolution and projection (use `ConditionEvaluationContext`; never a separate
  `*ProjectionCondition`).
- `ConditionalEffect` fields are `effect`/`elseEffect` → `then`/`otherwise`. There are ~2 type
  checks to fix.
- Don't confuse it with `ConditionalOnCollectionExecutor` (a different effect — leave it).
- 168 files re-bless: expect a large but mechanical snapshot diff. Confirm it's pure rename.

### `MayPayManaEffect` → `Gate.MayPay(PayManaCostEffect(cost))`
- The resolution-time form lowers cleanly: `Gate.MayPay` already runs `CompositeEffect(cost, then,
  stopOnError)` and `PayManaCostExecutor` handles mana-source selection — so paying mana already
  works through the frame.
- **Caution:** there is a separate *trigger-time* "you may pay" path (`MayPayManaTriggerContinuation`
  in `ManaContinuations.kt`, driven by `TriggerProcessor` and `ManaPaymentContinuationResumer`) used
  when the "may pay" is offered as a trigger goes on the stack rather than at resolution. Audit those
  sites (`grep -rn "as MayPayManaEffect"`) and decide whether the trigger path also routes through
  the frame or stays. Don't delete `MayPayManaExecutor` until every path is covered.

### `MayEffect` → `Gate.MayDecide` (biggest; extend the gate first)
`MayEffect` carries behavior the current minimal `MayDecide` doesn't yet model. **Before** lowering,
extend `GatedEffectExecutor`'s `MayDecide` branch (and `MayDecide`/`GatedEffect` as needed) to cover:
- `sourceRequiredZone` — skip silently if the source left the required zone.
- `inlineOnTrigger` — must flow into `DecisionContext(inlineOnTrigger = …)`.
- `ChooseActionEffect` feasibility — `MayEffectExecutor` skips the prompt when the inner
  `ChooseActionEffect` has no feasible choices; port that.
- `decisionMaker` (already supported) and `hint`.

Then handle the **6 type-level cast sites** (`grep -rn "is MayEffect\|as MayEffect"` over
`*/src/main`). The important coupling is the **may-trigger** machinery: `MayTriggerContinuation` /
`resumeMayTrigger` (in `EffectAndTriggerContinuationResumer.kt`) and `TriggerProcessor` do
`trigger.ability.effect as MayEffect` to unwrap a "may" *trigger* (yes/no asked as the trigger goes
on the stack, then targets). That is a distinct flow from the resolution-time `MayEffect`. You must
either (a) keep that trigger-time unwrap working against `GatedEffect`/`Gate.MayDecide`, or (b)
migrate it deliberately. Do not leave a half-cast that compiles but silently mis-resolves "may"
triggers. ~129 files re-bless — the largest snapshot diff in the whole effort.

### `IfYouDoEffect` → `Gate.DoAction(action, criterion)` (hardest mechanically)
This gate is **not decision-driven** — it runs an `action` (which may itself pause), then evaluates
`SuccessCriterion` against a pre-action snapshot to decide `then` vs `otherwise`. The existing
machinery to absorb:
- `IfYouDoEffectExecutor` (pre-pushes a continuation *before* the action runs),
- `IfYouDoContinuation` + `IfYouDoSnapshot`,
- the **auto-resumer** in `CoreAutoResumerModule` that fires when the action's own continuations
  drain (there is no yes/no decision to resume on).

`GatedEffectContinuation` today only handles the yes/no (decision-driven) resume. For `DoAction`
you'll need the action-drain/auto-resume path too — either extend the frame's continuation to carry
the snapshot + criterion and teach `CoreAutoResumerModule` about it, or keep a dedicated
continuation for the action-drain case while still routing through one `GatedEffectExecutor`. Keep
the `SuccessCriterion` evaluation logic (`Auto`/`CollectionNonEmpty`/`Always`) intact. Add scenario
tests for: action did work → `then`; action did nothing (declined / empty hand / no legal
sacrifice) → `otherwise`; action paused mid-way then completed.

### `PayOrSufferEffect` / `AnyPlayerMayPayEffect` / `MayPayXForEffect`
- `PayOrSuffer` is "[suffer] unless you pay [cost]" — i.e. `MayPay(cost)` with `then = null,
  otherwise = suffer`. But it stores a `PayCost` (not an `Effect`) and has its own continuations
  (`PayOrSufferContinuation`, `PayOrSufferChoiceContinuation`). Only fold it if it lowers cleanly;
  otherwise leave it and note why.
- `MayPayX` needs a number chooser (0..max), not a yes/no — a genuinely different gate. Defer unless
  the algebra extends naturally.
- `AnyPlayerMayPay` is APNAP, multi-player; the single-`decisionMaker` frame can't express the
  ordering. The lesson says keep it separate. **Recommend leaving it** unless you can model
  multi-player order cleanly — document the decision either way.

---

## 6. The mechanical recipe (per wrapper)

1. **Branch + skill.** New branch off the latest base; invoke `add-feature`.
2. **(Shape B only)** Add the `Gate` variant; add its branch in `GatedEffectExecutor.execute`, in
   `resumeGatedEffect` (or the auto-resumer for action-drain gates), in
   `CardValidator.collectIndicesRecursive`, and in `GatedEffect.description`. Compile.
3. **Lower the wrapper:** replace the `data class` with a same-name/same-signature facade `fun`
   returning `GatedEffect`, in the same package. Keep KDoc.
4. **Fix type-level references** (`is`/`as`/`shouldBeInstanceOf`/facade return types). Use
   `grep -rn "\bWrapperName\b" --include=*.kt`.
5. **Delete the bespoke executor** + its `CompositeExecutors.kt` registration (and any now-dead
   continuation/resumer — but only once *every* path is migrated; audit casts first).
6. **Build incrementally:** `./gradlew :mtg-sdk:compileKotlin :rules-engine:compileKotlin
   :mtg-sets:compileKotlin`.
7. **Tests:** add/adjust engine scenario tests proving parity (and canonical order for new gates).
   Update any SDK unit test that constructed the wrapper as a type.
8. **Re-bless snapshots** and eyeball the diff is a pure rename:
   ```
   ./gradlew :mtg-sets:test --tests "*CardDefinitionSnapshotTest" -DupdateSnapshots=true
   git diff mtg-sets/src/test/resources/snapshots/cards/   # confirm type/field rename only
   ```
9. **Docs:** update `docs/card-sdk-language-reference.md` in the same PR.
10. **Full sweep, then PR:**
    ```
    ./gradlew :mtg-sdk:test :mtg-sets:test :rules-engine:test :game-server:test
    ```

---

## 7. Load-bearing rules (will bite you)

- **`Effect` and `Gate` are sealed interfaces** → new `Gate` variants auto-register for kotlinx
  serialization with just `@Serializable` + `@SerialName("Gate.X")`. **`ContinuationFrame` is NOT** —
  any new continuation must be added to the `polymorphic(ContinuationFrame::class)` block in
  `rules-engine/.../core/Serialization.kt`, or you'll get a runtime serialization error.
- **Continuations must carry targets.** Any frame that later executes an effect referencing
  `EffectTarget.ContextTarget(n)` must propagate the `EffectContext` (with `targets`/`namedTargets`),
  or the effect resolves with an empty context and silently fizzles. `GatedEffectContinuation`
  already carries `effectContext` — keep it.
- **Pure data, no logic in SDK.** `Gate`/`GatedEffect` are serializable data bags; all behavior lives
  in the executor/resumer.
- **Immutability**; **events not silent mutations**; **projected state for battlefield reads** — all
  still apply (root `CLAUDE.md`).
- **Snapshot churn is expected and is the safety net.** A pure-rename diff across N sets is the
  *correct* outcome of a clean lowering; a structural change in the diff means you changed behavior —
  investigate before re-blessing.
- **Update the DSL reference in the same change** (`mtg-sdk` standing rule).

---

## 8. Definition of done (per wrapper PR)

- [ ] Wrapper is a facade `fun` lowering to `GatedEffect`; no card source edits.
- [ ] Bespoke executor (+ dead continuation/resumer, if fully superseded) deleted; registration removed.
- [ ] All `is`/`as`/`shouldBeInstanceOf` sites updated; no `as Wrapper` cast left mis-resolving.
- [ ] Scenario tests prove behavioral parity (+ canonical order for new gates) and pass.
- [ ] Snapshot goldens re-blessed; diff verified as a pure rename.
- [ ] `docs/card-sdk-language-reference.md` updated.
- [ ] `:mtg-sdk :mtg-sets :rules-engine :game-server` test suites green.
- [ ] CR numbers in comments/commit verified against the official rules text.
