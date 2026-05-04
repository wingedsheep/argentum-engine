package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bombard
 * {2}{R}
 * Instant
 * Bombard deals 4 damage to target creature.
 */
val Bombard = card("Bombard") {
    manaCost = "{2}{R}"
    typeLine = "Instant"
    oracleText = "Bombard deals 4 damage to target creature."

    spell {
        val target = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(4, target)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Diego Gisbert"
        flavorText = "\"Go back to your little war, sun-stooge! This here is *our* operation!\"\n—Kunthaar, Kav mine operator"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29492df8-3077-4ded-a6e2-51d3bd4669b4.jpg?1752947077"
    }
}
