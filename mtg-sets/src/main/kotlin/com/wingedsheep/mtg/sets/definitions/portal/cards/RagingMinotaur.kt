package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Raging Minotaur
 * {2}{R}{R}
 * Creature — Minotaur Berserker
 * 3/3
 * Haste
 */
val RagingMinotaur = card("Raging Minotaur") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Minotaur Berserker"
    power = 3
    toughness = 3

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Randy Gallegos"
        flavorText = "In a rage, the minotaur knows no friend from foe."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90a6b7c8-d9e0-f1a2-b3c4-d5e6f7a8b9c0.jpg"
    }
}
