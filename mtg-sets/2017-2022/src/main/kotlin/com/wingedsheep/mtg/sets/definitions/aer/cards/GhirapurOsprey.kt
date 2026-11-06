package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ghirapur Osprey
 * {2}{W}
 * Creature — Bird
 * 2/2
 * Flying
 */
val GhirapurOsprey = card("Ghirapur Osprey") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Zezhou Chen"
        flavorText = "Many airship captains keep pets that share their love of the sky."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/2186f382-2d68-4191-b490-a072f49eaabf.jpg?1783936779"
    }
}
