package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** "a basic, Sphere, or Locus land" — the pool both abilities read. */
private val basicSphereOrLocus =
    GameObjectFilter.BasicLand or
        GameObjectFilter.Land.withSubtype("Sphere") or
        GameObjectFilter.Land.withSubtype("Locus")

/**
 * Monument to Perfection — Phyrexia: All Will Be One #233
 * {2}
 * Artifact
 * {3}, {T}: Search your library for a basic, Sphere, or Locus land card, reveal it, put it into
 * your hand, then shuffle.
 * {3}: This artifact becomes a 9/9 Phyrexian Construct artifact creature, loses all abilities,
 * and gains indestructible and toxic 9. Activate only if there are nine or more lands with
 * different names among the basic, Sphere, and Locus lands you control.
 *
 * The transform has no duration — it lasts indefinitely (ruling 2023-02-04), so every piece is
 * `Duration.Permanent`. "Loses all abilities" is applied before the indestructible / toxic 9
 * grants (same resolution, later timestamp), so the grants survive the wipe; a second
 * activation's wipe removes the first activation's toxic, so toxic never stacks (ruling).
 * The activation gate counts distinct names via `distinctNames()` over the same pool.
 */
val MonumentToPerfection = card("Monument to Perfection") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Search your library for a basic, Sphere, or Locus land card, reveal it, " +
        "put it into your hand, then shuffle.\n" +
        "{3}: This artifact becomes a 9/9 Phyrexian Construct artifact creature, loses all abilities, " +
        "and gains indestructible and toxic 9. Activate only if there are nine or more lands with " +
        "different names among the basic, Sphere, and Locus lands you control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = basicSphereOrLocus,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.RemoveAllAbilities(EffectTarget.Self, Duration.Permanent) then
            Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 9,
                toughness = 9,
                keywords = setOf(Keyword.INDESTRUCTIBLE),
                creatureTypes = setOf("Phyrexian", "Construct"),
                duration = Duration.Permanent
            ) then
            Effects.GrantToxic(9, EffectTarget.Self, Duration.Permanent)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, basicSphereOrLocus).distinctNames(),
                    ComparisonOperator.GTE,
                    9
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab892e7b-6797-4ede-ab5d-d9d0fa488c80.jpg?1783917987"
        ruling("2023-02-04", "Monument to Perfection's last ability lasts indefinitely.")
        ruling("2023-02-04", "Because the last ability causes Monument to Perfection to lose all the abilities, activating it multiple times before the first activation resolves does not result in it having multiple instances of toxic 9.")
    }
}
