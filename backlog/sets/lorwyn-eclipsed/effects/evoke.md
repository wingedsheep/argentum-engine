# Evoke (Alternative Cost → Sacrifice on ETB)

**Status:** Implemented
**Cards affected in Lorwyn Eclipsed:** 5 — Incarnation cycle (`Catharsis`, `Deceit`, `Emptiness`,
`Wistfulness`, and one more). All are six-mana hybrid creatures with a cheap two-mana evoke cost.
**Priority:** High — blocks the whole Incarnation cycle, which anchors the Elemental/tribal theme.

## Rules text

> Evoke {cost} *(You may cast this spell for its evoke cost. If you do, it's sacrificed when it
> enters.)*

The ECL incarnations pair Evoke with **mana-spent-gated ETB triggers** — "When this creature enters,
if {W}{W} was spent to cast it, ...". See `mana-spent-gated-triggers.md` for that half. This doc
covers only the alternative cost + sacrifice-on-entry machinery.

Rules subtleties to respect (CR 702.74):
- The player chooses evoke at cast time; it is an alternative cost (not an additional cost), so
  cost reductions apply normally, and it's mutually exclusive with other alternative costs.
- "It's sacrificed when it enters" creates a delayed triggered ability that goes on the stack
  when the creature enters. The ETB triggers from the card's own abilities go on the stack at the
  same time — APNAP ordering applies. A player can still respond between ETB and the sacrifice
  trigger resolving, which is why evoke creatures can still *do* their ETB effect.
- Flicker effects on an evoked creature do **not** bring the "sac it" delayed trigger along — it's
  tied to the original entering, not the object's identity across zones.

## Implementation plan

### 1. SDK — add `KeywordAbility.Evoke`
`mtg-sdk/.../scripting/KeywordAbility.kt`

```kotlin
@SerialName("Evoke")
@Serializable
data class Evoke(val cost: ManaCost) : KeywordAbility {
    override val description = "Evoke $cost"
}
```

Add a parameterized keyword DSL hook in `CardBuilder.kt`, mirroring `cycling("{2}")`:

```kotlin
fun evoke(cost: String) = keywordAbility(KeywordAbility.Evoke(ManaCost.parse(cost)))
```

Also add `Keyword.EVOKE("Evoke")` to `core/Keyword.kt` so reminder-text parsing can pick it up if
a card grants evoke.

### 2. SDK — mark the alternative cast mode
Evoke is an alternative casting cost. The existing mechanism for that lives on `CastSpellHandler`
and `CastPaymentProcessor`. Add a new case to whatever enum/sealed type represents
"cast modes" (look for how `Morph`/`Kicker`/`Flashback` pipe through). Minimum additions:
- A `CastMode.Evoke` (or field on `CastSpell` action — `castForEvoke: Boolean`) indicating the
  spell was cast for its evoke cost.
- A `LegalAction.CastForEvoke` enumerated by `CastSpellEnumerator` whenever the card has an
  `Evoke` keyword and the player can pay the evoke cost.

### 3. Engine — pay the evoke cost
Extend `CastPaymentProcessor` so when `castForEvoke` is set, it uses the `Evoke.cost` as the mana
cost instead of the card's base mana cost. Every other cost-reduction and payment hook should
keep working, because it's still mana going through `ManaSolver`.

### 4. Engine — tag the permanent with `EvokedComponent`
On resolution, when a creature enters the battlefield and the cast record shows `castForEvoke`,
attach a tag component:

```kotlin
// rules-engine/.../state/components/battlefield/EvokedComponent.kt
data object EvokedComponent : Component
```

This tag is purely a marker used by the delayed trigger in the next step. It is stripped in
`stripBattlefieldComponents()` like any other battlefield-only component.

### 5. Engine — delayed triggered ability "sacrifice when it enters"
There are two implementation choices here. Prefer **(a)** unless it's painful:

**(a) Use the existing replacement/delayed-trigger infra.**  When a permanent enters with
`EvokedComponent`, emit an `EnterBattlefieldEvent` as normal, and have a dedicated detector queue a
`SacrificePermanentEffect(EffectTarget.Self)` on the stack as an independent triggered ability.
This keeps ETB triggers and the sacrifice trigger as siblings on the stack, which matches CR
rulings (you can respond between them).

**(b) Replacement effect that rewrites ETB.**  Only take this route if (a) can't reach "the sac
trigger and the ETB trigger are separate stack objects".

Hook the detector into `TriggerDetector.detectTriggers()` under the ETB event path, checking for
`EvokedComponent` on the entering entity.

### 6. Card DSL
After the above is wired, the Catharsis card looks like:

```kotlin
card("Catharsis") {
    manaCost = "{4}{R/W}{R/W}"
    typeLine = "Creature — Elemental Incarnation"
    power = 4; toughness = 4
    evoke("{R/W}{R/W}")

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes("{W}{W}")
        effect = Effects.CreateTokens(2, TokenPatterns.KithkinSoldier)
    }
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes("{R}{R}")
        effect = Effects.GroupPumpAndHaste(filter = GameObjectFilter.Creature.controlledByYou())
    }
}
```

(`ManaSpentToCastIncludes` is specified in `mana-spent-gated-triggers.md`.)

### 7. Tests
- Unit: evoke cost is a legal cast option iff player has mana; cast via evoke correctly deducts
  evoke cost and not the base cost.
- Scenario: cast an Incarnation for its evoke cost, verify (1) ETB fires, (2) creature is
  sacrificed at the same stack resolution window, (3) a Lightning Bolt aimed at the creature
  between ETB and the sac trigger still kills it before the sac trigger resolves.
- Scenario: flicker an evoked creature with Ephemerate — it should **not** be sacrificed on
  re-entry (no `EvokedComponent` on the new object).
- Regression: cost-reduction static abilities (`Convoke`, affinity-likes) reduce the evoke cost,
  not the base cost, when evoking.

## Dependencies

- Requires `mana-spent-gated-triggers.md` for the incarnations' actual payoffs, but the Evoke
  keyword itself is independent.
- No new continuation types required — existing cast/resolve plumbing suffices.
