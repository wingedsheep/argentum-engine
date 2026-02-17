# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

`mtg-sdk` is the **shared contract** module. It defines all data models, DSLs, and scripting primitives used by both
`mtg-sets` (card content) and `rules-engine` (execution). It has zero dependencies on other modules and no execution
logic.

## Build & Test

```bash
# From repo root
just test-rules          # Runs rules-engine tests (which depend on sdk)
./gradlew :mtg-sdk:test  # SDK-only tests (serialization + DSL)
```

## Package Structure

All code lives under `com.wingedsheep.sdk`:

| Package               | Purpose                                                                                                                           |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `model/`              | `CardDefinition`, `CardScript`, `EntityId`, `Deck` — master data objects                                                          |
| `core/`               | Enums: `Zone`, `Phase`, `Step`, `Keyword`, `CardType`, `Subtype`, `ManaCost`, etc.                                                |
| `scripting/`          | Sealed interfaces: `Effect`, `Trigger`, `StaticAbility`, `ActivatedAbility`, `TriggeredAbility`, `ReplacementEffect`, `Condition` |
| `scripting/effects/`  | Concrete `Effect` subtypes grouped by category (Damage, Life, Drawing, Library, etc.)                                             |
| `scripting/triggers/` | Concrete `Trigger` subtypes grouped by category (ZoneChange, Phase, Combat, etc.)                                                 |
| `dsl/`                | Facade objects and builders: `CardBuilder`, `Effects`, `Triggers`, `Targets`, `Costs`, `Conditions`, `Filters`, `EffectPatterns`  |
| `targeting/`          | `TargetRequirement` sealed interface and subtypes                                                                                 |
| `serialization/`      | kotlinx.serialization support for `CardDefinition`                                                                                |

## Card DSL

The primary entry point is the `card()` function in `CardBuilder.kt`:

```kotlin
val LightningBolt = card("Lightning Bolt") {
    manaCost = "{R}"
    typeLine = "Instant"
    oracleText = "Lightning Bolt deals 3 damage to any target."

    spell {
        val any = target("any", Targets.Any)
        effect = Effects.DealDamage(3, any)
    }

    metadata { rarity = Rarity.UNCOMMON }
}
```

Builder blocks available on `CardBuilder`:

- `spell { }` — instant/sorcery effect + targets + cast restrictions
- `triggeredAbility { }` — trigger, effect, optional target, `optional`, `triggerCondition`
- `activatedAbility { }` — cost, effect, optional target, `manaAbility`, `timing`
- `staticAbility { }` — continuous effect via `ability = StaticAbility.XYZ(...)`
- `loyaltyAbility(delta) { }` — planeswalker ability
- `keywords(...)` / `keywordAbility(...)` — simple and parameterized keywords
- `additionalCost(...)` / `replacementEffect(...)` — extra costs and replacement effects
- `metadata { }` — Scryfall metadata (rarity, artist, flavor, imageUri, etc.)

For basic lands with art variants use `basicLand("Plains") { ... }`.

## Targeting

`EffectTarget` determines what an effect operates on at resolution:

- `EffectTarget.ContextTarget(index)` — references cast-time target at position `index`
- `EffectTarget.Controller` — the ability/spell controller
- `EffectTarget.Self` — the permanent itself
- `EffectTarget.TriggeringEntity` — the entity that caused the trigger

`TargetRequirement` determines what a player can select at cast/activation time. Use the `Targets` facade (e.g.
`Targets.Any`, `Targets.Creature`, `Targets.Player`) or raw subtypes from `targeting/`.

For multi-target spells, use the named binding pattern:

```kotlin
spell {
    val creature = target("creature", Targets.Creature)
    val player = target("player", Targets.Player)
    effect = Effects.Composite(Effects.Destroy(creature), Effects.DealDamage(3, player))
}
```

## DSL Facades

Always use these facades rather than constructing effect/trigger data classes directly:

- **`Effects`** — factory methods for all effect types. When an effect has no standalone factory, compose from
  `EffectPatterns`.
- **`EffectPatterns`** — pre-built pipelines: `scry()`, `surveil()`, `mill()`, `searchLibrary()`, `exileUntilEndStep()`,
  `wheelEffect()`, `mayPay()`, etc.
- **`Triggers`** — named trigger constants (`Triggers.EntersBattlefield`, `Triggers.Dies`, `Triggers.Attacks`, etc.)
- **`Costs`** — ability costs (tap, sacrifice, pay life, discard, etc.)
- **`Conditions`** — boolean predicates for `triggerCondition`, `castOnlyIf`, `Branch` effects
- **`Filters`** / `GameObjectFilter` — object filters used in `Sacrifice`, `SearchLibrary`, group effects

## Adding a New Effect Type

When the engine needs a new primitive:

1. **Add the data class** in the appropriate file under `scripting/effects/` (or create a new file for a new category).
   Must be `@Serializable`.
2. **Register in `Effect.kt`** — the sealed interface (or the relevant sealed sub-interface) must include the new type.
3. **Add a factory method** to `Effects.kt` (or `EffectPatterns.kt` if it's a composition).
4. **Implement executor** in `rules-engine` (separate module).

All `Effect`, `Trigger`, `StaticAbility`, `TargetRequirement`, `Condition`, etc. subtypes must be annotated with
`@Serializable` because `CardDefinition` is serialized for transport.

## Adding a New Trigger Type

1. Add data class in the appropriate file under `scripting/triggers/`.
2. Include in the `Trigger` sealed interface in `Trigger.kt`.
3. Add a named constant or factory in `Triggers.kt` if it will be commonly used.
4. Wire up detection in `rules-engine`'s `TriggerDetector`.

## Testing

SDK tests use Kotest `DescribeSpec`:

```kotlin
class MyTest : DescribeSpec({
    describe("feature") {
        it("should do X") {
            val card = card("Test") { ... }
            card.name shouldBe "Test"
        }
    }
})
```

The two test classes are `CardDslTest` (DSL correctness) and `CardSerializationRoundTripTest` (serialization).
