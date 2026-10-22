package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Purifying Dragon (Innistrad: Midnight Hunt #155)
 * {3}{R}{R}
 * Creature — Dragon
 * 4/3
 * Flying
 * Whenever this creature attacks, it deals 1 damage to target creature defending player controls.
 * If that creature is a Zombie, this creature deals 2 damage to it instead.
 *
 * The Zombie check happens on resolution against the target's projected subtypes, picking 2
 * damage over 1 (the Zephyr Sentinel `If(TargetMatchesFilter)` shape).
 */
val PurifyingDragon = card("Purifying Dragon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\n" +
        "Whenever this creature attacks, it deals 1 damage to target creature defending player controls. " +
        "If that creature is a Zombie, this creature deals 2 damage to it instead."
    power = 4
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature.defendingPlayerControls())
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE),
                creature
            ),
            then = Effects.DealDamage(2, creature),
            otherwise = Effects.DealDamage(1, creature)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9db384e-9555-4ccc-a305-ee448252e685.jpg?1783925591"
    }
}
