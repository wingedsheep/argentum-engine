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
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fed57a17-7847-4e60-bc40-4452880f12a3.jpg"
    }
}
