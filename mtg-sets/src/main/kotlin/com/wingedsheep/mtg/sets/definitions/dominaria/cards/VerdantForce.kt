package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Verdant Force
 * {5}{G}{G}{G}
 * Creature — Elemental
 * 7/7
 * At the beginning of each upkeep, create a 1/1 green Saproling creature token.
 */
val VerdantForce = card("Verdant Force") {
    manaCost = "{5}{G}{G}{G}"
    typeLine = "Creature — Elemental"
    power = 7
    toughness = 7
    oracleText = "At the beginning of each upkeep, create a 1/1 green Saproling creature token."

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Viktor Titov"
        flavorText = "\"The bower shuddered. The stillness broke. The scurf shifted, and a being emerged from the flowers and ferns.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d972f97-1945-440b-8bd3-63038db22257.jpg?1562732288"
    }
}
