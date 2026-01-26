package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Imp
 * {1}{B}
 * Creature — Imp
 * 1/1
 * Flying
 */
val BogImp = card("Bog Imp") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Imp"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Richard Kane Ferguson"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8681b3fd-33e5-4a45-8650-a4a142405096.jpg"
    }
}
