package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Raging Goblin
 * {R}
 * Creature — Goblin Berserker
 * 1/1
 * Haste
 */
val RagingGoblin = card("Raging Goblin") {
    manaCost = "{R}"
    typeLine = "Creature — Goblin Berserker"
    power = 1
    toughness = 1

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Pete Venters"
        flavorText = "Charging alone takes uncommon daring or uncommon stupidity. Or both."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f05a6b7-c8d9-e0f1-a2b3-c4d5e6f7a8b9.jpg"
    }
}
