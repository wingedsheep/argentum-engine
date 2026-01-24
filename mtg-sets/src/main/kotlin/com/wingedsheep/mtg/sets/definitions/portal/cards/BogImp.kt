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
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a60d7a3-83a4-48db-ac6c-26c55c86898a.jpg"
    }
}
