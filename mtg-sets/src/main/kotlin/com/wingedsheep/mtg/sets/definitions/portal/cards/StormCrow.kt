package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Storm Crow
 * {1}{U}
 * Creature - Bird
 * 1/2
 * Flying
 */
val StormCrow = card("Storm Crow") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 2

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Una Fricker"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/036ef8c9-72ac-46ce-af07-83b79d736538.jpg"
    }
}
