# Scoping: cast-time choices & inherited X (the "declare directive" problem)

**Status:** Phase 1 done — `DynamicAmount.CastX` + durable `CastChoicesComponent`; Hydroid Krasis
implemented and tested (RNA set scaffolded). Phases 2–4 (slot generalization, `declare { }` DSL,
emitter payoff) not started. **Owner:** TBD. **Related:**
[`sdk-language-design.md`](sdk-language-design.md), [`forge-parity-harness.md`](forge-parity-harness.md),
and the `:mtgish-tooling` *"Creator's note: extra costs & chosen / inherited values"* in
[`../mtgish-tooling/README.md`](../mtgish-tooling/README.md) (this doc is the design that note asks for).

> **TL;DR.** What Magic treats as *one* thing — the choices you lock in as you cast a spell (modes, X,
> "choose a color / creature type", which optional/additional costs you paid) — Argentum models through
> **three unrelated channels**, and only some survive resolution. X is stranded on a stack-only
> component (`SpellOnStackComponent.xValue`); chosen color/type/mode ride durable `Chosen*Component`s
> but are timed *as it enters*, not *as you cast*; additional costs are a third rail
> (`AdditionalCostPayment` + `WasKickedComponent` + a one-off `ContextProperty` for blight). The
> consequence is that **"When ~ enters, draw X cards" (X = the cast-time X) has no clean path** — you're
> forced to launder X through counters per-card. Forge solves this with a `declare` directive + a hidden
> SVar bag that rides the (mutable) card object. mtgish already models it cleanly too — `ValueX` /
> `Trigger_ValueXOfThatSpell` and `TheChosenColor` are *named bindings on the object*, read from
> separate abilities. **The fix:** one durable `CastChoicesComponent` attached at cast that rides the
> stable entity through resolution (the immutable-ECS analogue of Forge's SVar bag), plus a
> `declare { }` DSL and slot-reading `DynamicAmount`/`Condition`. Phase 1 (X-survives-resolution) is
> small and high-leverage; do it first on a real card.

---

## 1. Why this came up

The spell-casting / ability-activation path is sloppy around three things the rules treat as a single
"as you cast" step (CR 601):

- **Extra (additional) costs** — costs declared and paid alongside the mana cost.
- **Choosing values at cast/activation time** — X, a chosen creature type, a chosen color, a mode.
- **Inheriting a chosen value into later effects** — e.g. *"When ~ enters, draw X cards"* where `X`
  must be the same `X` chosen when the creature was cast; or *"the chosen color"* read by an activated
  ability that runs turns later.

The headline failure is the third. Today there is **no clean way to carry the cast-time X onto the
permanent and read it from a later triggered/activated ability** without abusing counters.

## 2. The actual problem: one concept, three half-built channels

| Choice | Where it lives now | Survives onto the permanent? |
|---|---|---|
| **X** | `SpellOnStackComponent.xValue` (+ `manaSpentOnXByColor`), `rules-engine/.../state/components/stack/StackComponents.kt:19` | ❌ No — stack-only. Laundered into counters per-card via `EntersWithDynamicCounters` |
| **Color / creature-type / mode** | `EntersWithChoice` replacement effect → durable `Chosen*Component` (`rules-engine/.../state/components/identity/ChosenValueComponents.kt`) | ✅ Yes, but only for *permanents*, timed "as it enters," not "as you cast" |
| **Additional costs** | `AdditionalCostPayment` (stack); `WasKickedComponent` (durable); `AdditionalCost.BlightVariable` → `DynamicAmount.ContextProperty(ADDITIONAL_COST_BLIGHT_AMOUNT)` | ⚠️ Partial — kicked-ness durable, blight-X is a one-off `ContextProperty` channel |

Three different mechanisms, three different timings, bespoke per-effect reads. `DynamicAmount.XValue`
resolves from the transient `EffectContext.xValue` (`DynamicAmountEvaluator.kt` →
`is DynamicAmount.XValue -> context.xValue ?: 0`), which is **only populated while the spell itself is
resolving** — so the ETB trigger and any later ability can't see it.

## 3. The load-bearing fact: the entity ID is stable across zones

`StackResolver.resolveSpell` emits `ZoneChangeEvent(spellId, …, Zone.BATTLEFIELD, …)` — the **same
`EntityId`** that was the spell on the stack becomes the permanent on the battlefield. (Rules-wise a
permanent entering the battlefield is a *new object*; the engine reuses the entity ID as an
implementation choice — which is exactly why the copy/LKI policy in §6 matters.) This is the whole
ballgame: **a component attached at cast time rides the entity onto the battlefield for free**, the
same way `WasKickedComponent` already does. We don't need to copy anything across a zone boundary; we
just need to stop *dropping* it.

## 4. Prior art — how Forge and mtgish model this

**Forge.** A Forge card is one long-lived *mutable* object with an SVar bag. `xPaid` is stamped into
that bag at cast, so `Count$xPaid` works in an ETB trigger because the same object carries the bag
through zones. The `declare` directive is just: the script lists the choices, the engine prompts in
order and writes each into the bag. The "hidden magic for X" is a side effect of mutability.

**mtgish.** More principled than Forge and ahead of Argentum here. It models choices as **named
bindings on the object**, referenced by name from *any* ability:

- `{"_GameNumber":"ValueX"}` and `{"_GameNumber":"Trigger_ValueXOfThatSpell"}` — the cast-time X,
  read by the enters-with-counters rule *and* by a "when you cast" trigger.
- `{"_TokenColorList":"TheChosenColor"}`, `{"_TokenSubtypes":"TheChosenCreatureType"}` — the chosen
  values, read by a **separate** activated-ability rule.
- `{"_Condition":"AColorWasChosen"}` — "was this choice made" guards.

**The synthesis to aim for:** mtgish's named-binding abstraction + Forge's "it rides the object"
persistence, implemented as **one immutable ECS component**.

## 5. Worked example — Hydroid Krasis (`{X}{G}{U}`)

The cleanest "inherited X" card, and **not yet implemented** in Argentum, so this is a real target.

> When you cast this spell, you gain X life and draw X cards. *(actually half X, rounded down)*
> Flying, trample.
> Hydroid Krasis enters the battlefield with X +1/+1 counters on it.

### 5.1 mtgish IR (real, from the corpus)

```jsonc
"ManaCost": [ {"_ManaSymbol":"ManaCostX"}, {"_ManaSymbol":"ManaCostG"}, {"_ManaSymbol":"ManaCostU"} ],
"Rules": [
  { "_Rule": "FromStack", "args": {
      "_Rule": "TriggerA",
      "args": [
        { "_Trigger": "WhenAPlayerCastsASpell", "args": [ /* You */, /* ThisSpell */ ] },
        { "_Actions": "ActionList", "args": [
            { "_Action": "GainLife",        "args": {"_GameNumber":"HalfRoundedDown","args":{"_GameNumber":"Trigger_ValueXOfThatSpell"}} },
            { "_Action": "DrawNumberCards", "args": {"_GameNumber":"HalfRoundedDown","args":{"_GameNumber":"Trigger_ValueXOfThatSpell"}} }
        ] } ] } },
  { "_Rule": "Flying" }, { "_Rule": "Trample" },
  { "_Rule": "AsPermanentEnters", "args": [ {"_Permanent":"ThisPermanent"},
      [ {"_ReplacementActionWouldEnter":"EntersWithNumberCounters",
         "args":[ {"_GameNumber":"ValueX"}, {"_CounterType":"PTCounter","args":[1,1]} ] } ] ] }
]
```

`Trigger_ValueXOfThatSpell` (in the trigger) and `ValueX` (in the enters rule) are *the same X*. No
mtgish change needed — the work is making Argentum *consume* this as a durable binding.

### 5.2 Argentum SDK — today: not writable

`DynamicAmount.XValue` only resolves during the spell's own resolution. The "when you cast" trigger and
the ETB-counters run in contexts that don't carry the spell's X. The only X-survives path that exists
is laundering through counters — which feeds the +1/+1 counters but **cannot** feed "draw half X". Dead
end. (This is the README gap.)

### 5.3 Argentum SDK — target

```kotlin
val HydroidKrasis = card("Hydroid Krasis") {
    manaCost = "{X}{G}{U}"
    typeLine = "Creature — Jellyfish Hydra Beast"
    power = 0; toughness = 0

    // X is declared as you cast and BOUND to this object for its whole lifetime.
    declare { x() }

    // "When you cast this spell, you gain half X life and draw half X cards."
    triggeredAbility {
        trigger = WhenYouCastThis
        effect = Effects.Composite(
            Effects.GainLife(DynamicAmount.Half(DynamicAmount.CastX, RoundDown)),
            Effects.DrawCards(DynamicAmount.Half(DynamicAmount.CastX, RoundDown)),
        )
    }
    keyword(Keyword.FLYING); keyword(Keyword.TRAMPLE)

    // Enters with X +1/+1 counters — reads the SAME binding, not a separate X.
    replacementEffect(EntersWithDynamicCounters(CounterTypeFilter.PlusOnePlusOne, DynamicAmount.CastX))
}
```

The one new primitive that unlocks everything: **`DynamicAmount.CastX`** — "the X this object was cast
with," read off the *current object* regardless of zone. Contrast `DynamicAmount.XValue` ("the X of the
spell currently resolving," context-scoped). `CastX` is the direct analogue of mtgish's `ValueX` /
`Trigger_ValueXOfThatSpell`. `declare { x() }` is the `declare` directive — an ordered, data-driven
list of cast-time choices the cast handler walks.

### 5.4 Argentum engine — today vs. target

**Today.** `SpellOnStackComponent.xValue` holds X only while on the stack; on resolution
`resolvePermanentSpell` drops `SpellOnStackComponent`, so **X is gone** even though the entity survives.

**Target.** Lean on §3 — attach a durable component at cast that rides the stable entity:

```kotlin
// New durable component — the immutable analogue of Forge's SVar bag.
data class CastChoicesComponent(
    val x: Int? = null,
    val manaSpentOnXByColor: Map<Color, Int> = emptyMap(),
    val chosen: Map<ChoiceSlot, ChoiceValue> = emptyMap(),   // color, creature type, mode, "kicked", blight amount…
) : Component
```

- **At cast** (CR 601 "as you cast" step), the cast handler writes `CastChoicesComponent(x = chosenX, …)`
  onto the spell entity, *in addition to* `SpellOnStackComponent` (which stays as the stack-presence
  marker). Today X is written only to `SpellOnStackComponent`; the change is to also stamp the durable
  component.
- **On resolution**, `resolvePermanentSpell` removes `SpellOnStackComponent` but **leaves
  `CastChoicesComponent` in place** — it rides to the battlefield on the same entity.
- **The evaluator** gains one case that reads the current object, not the context:
  ```kotlin
  is DynamicAmount.CastX ->
      state.getEntity(context.currentObjectId)?.get<CastChoicesComponent>()?.x ?: 0
  ```
  Now `CastX` resolves correctly from (a) the spell's own resolution, (b) the "when you cast" trigger
  (entity still on stack), and (c) the ETB-counters replacement (entity now on battlefield) — one
  mechanism, three sites.

## 6. Rules traps to design around up front

- **LKI / dies-triggers.** `CastChoicesComponent` must be part of last-known-information, or "when this
  creature that was cast with X=5 dies" loses its X. Same treatment as the `lastKnownCardDefinitionId`
  fallback on `ZoneChangeEvent`.
- **Copies (CR 706).** A copy of an X spell has X=0; a copy of a permanent does not inherit "as it
  entered" choices unless a rule says so. `CastChoicesComponent` must **not** be carried by clone/copy
  by default — design a per-slot copyable policy, don't carry the bag wholesale. (`EntersWithChoice`
  already wrestles with re-choosing for permanent copies; reuse that thinking.)
- **New-object semantics.** The engine reuses the entity ID across the stack→battlefield boundary, but
  the rules consider it a new object. Anything that *should* reset on a zone change (summoning sickness,
  "this turn" markers) must keep resetting; only the explicitly-declared cast choices persist.

## 7. Suggested phasing

1. **X survives resolution (smallest, highest leverage).** Copy `xValue` / `manaSpentOnXByColor` off
   `SpellOnStackComponent` onto a durable component during `resolvePermanentSpell`; add
   `DynamicAmount.CastX` reading it. Implement **Hydroid Krasis** as the proof. Low risk; immediately
   fixes "When ~ enters, draw X cards" with zero counter laundering. **Do this first** to validate the
   entity-carries-component approach before the bigger refactor.
2. **Generalize to slots.** Introduce `CastChoicesComponent` + `DynamicAmount.CastChoice(slot)` /
   `Condition.CastChoiceMade(slot)` / `Condition.CastChoiceIs(slot, …)`; migrate `EntersWithChoice`'s
   `Chosen*Component`, `WasKickedComponent`, and `BlightVariable`→`ContextProperty` onto it. (Per the
   "no single-use patterns" rule, don't extract the slot abstraction until X + one chosen value both
   use it.) Migrate **Riptide Replicator** — collapse the one-off `CreateChosenTokenEffect` into a
   generic `Effects.CreateToken` that reads `ChoiceSlot.Color` / `ChoiceSlot.CreatureType`.
3. **The `declare { }` DSL.** So the cast handler drives prompts from data (the `declare` directive);
   retire the special-case X field path.
4. **Bridge/emitter payoff.** Teach `:mtgish-tooling` to emit declarations; flip the SCAFFOLD shapes
   to AUTOGEN (§8).

## 8. Payoff — near 1:1 mtgish→Argentum mapping for the emitter

Once `CastChoicesComponent` + `declare { }` + slot-reading `DynamicAmount`/`Condition` exist, the
bridge stops scaffolding this whole class:

| mtgish IR | Argentum (target) |
|---|---|
| `ManaCostX` | `declare { x() }` |
| `ValueX` / `Trigger_ValueXOfThatSpell` | `DynamicAmount.CastX` |
| `ChooseAColor` / `ChooseACreatureType` | `declare { color() / creatureType() }` |
| `TheChosenColor` / `TheChosenCreatureType` | `*.FromCastChoice(ChoiceSlot.…)` |
| `AColorWasChosen` | `Condition.CastChoiceMade(ChoiceSlot.Color)` |
| `AdditionalCastingCost` | `declare { additionalCost(...) }` |
| `Kicker` / `SpellActions_Kicker` | `declare { additionalCost(optional = true) }` + gate on `Condition.WasKicked` |

Fixing the engine model directly converts a swath of today's SCAFFOLD shapes to AUTOGEN, and generated
cards read like the hand-authored targets in §5.3.

## 9. Definition of done

- [x] `DynamicAmount.CastX` resolves from a durable component, working from the spell's own resolution,
      a "when you cast this" trigger, an ETB trigger, and a later activated ability.
      (`CastXDurableValueTest` covers all four; the evaluator reads `CastChoicesComponent` on the
      permanent, `SpellOnStackComponent.xValue` while on the stack, then the context as LKI.)
- [x] **Hydroid Krasis** implemented with a passing scenario test: cast for X=6 → gain 3 life, draw 3,
      enters with six +1/+1 counters (6/6); cast for X=0 → no draw, 0/0 dies as SBA.
      (`HydroidKrasisScenarioTest`, plus an X=5 round-down case.)
- [x] X survives a dies-trigger (LKI, via the leave `ZoneChangeEvent.xValue`) and is *not* inherited by
      a copy (CR 707.2; `CastXNotInheritedByCopyTest` clones an X-cast creature → no counters/component).
- [ ] (Phase 2+) `CastChoicesComponent` unifies X + chosen color/type/mode + kicked + blight; Riptide
      Replicator migrated off `CreateChosenTokenEffect`.
- [x] `docs/card-sdk-language-reference.md` updated for `DynamicAmount.CastX` (the `declare { }` DSL and
      slot readers remain Phase 2/3).

> Build with the **`add-feature`** skill — this is a cross-layer SDK primitive (SDK → engine →
> projection/triggers → continuations → server DTO if X is shown), not a single card.
