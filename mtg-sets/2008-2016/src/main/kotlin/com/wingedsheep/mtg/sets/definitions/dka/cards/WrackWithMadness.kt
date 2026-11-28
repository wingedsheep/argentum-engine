package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wrack with Madness
 * {3}{R}
 * Sorcery
 * Target creature deals damage to itself equal to its power.
 *
 * The target is both the damage source and the recipient (same shape as Kiku, Night's Flower),
 * so its own deathtouch, lifelink, infect or wither apply to the damage.
 */
val WrackWithMadness = card("Wrack with Madness") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature deals damage to itself equal to its power."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(
            DynamicAmounts.powerOf(creature),
            creature,
            damageSource = creature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Todd Lockwood"
        flavorText = "The skathul, spirits consumed by revenge, fester and seethe while looking for weak minds to assault."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f27ee20-b5c5-4867-b832-9cdade0eda03.jpg?1783940810"
        ruling(
            "2011-01-22",
            "Creature abilities like deathtouch or lifelink may affect the results of the damage dealt."
        )
    }
}
