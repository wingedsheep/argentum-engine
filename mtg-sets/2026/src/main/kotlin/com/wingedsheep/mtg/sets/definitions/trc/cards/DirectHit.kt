package com.wingedsheep.mtg.sets.definitions.trc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Direct Hit
 * {2}{R}
 * Sorcery
 * Direct Hit deals 5 damage to target creature.
 */
val DirectHit = card("Direct Hit") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Direct Hit deals 5 damage to target creature."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "David Szabo"
        flavorText = "Beware a Klingon with a short temper and a big grudge."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97388288-9254-4fa8-bcae-c2e2a55ef92b.jpg?1785981126"
    }
}
