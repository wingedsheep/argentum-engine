package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Prized Griffin
 * {4}{W}
 * Creature — Griffin
 * 3/4
 *
 * Flying
 */
val PrizedGriffin = card("Prized Griffin") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Griffin"
    oracleText = "Flying"
    power = 3
    toughness = 4

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Jesper Ejsing"
        flavorText = "\"I wrote these tales with a quill made from a griffin's feather. And the winning of that feather is quite a tale itself.\"\n" +
            "—Chulane, Teller of Tales"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/877bd423-83ff-4a28-b0d2-447a7821bb8c.jpg"
    }
}
