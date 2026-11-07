package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dawn Gryff (Eldritch Moon #19)
 * {2}{W}
 * Creature — Hippogriff
 * 2/2
 *
 * Flying
 */
val DawnGryff = card("Dawn Gryff") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Hippogriff"
    power = 2
    toughness = 2
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Christine Choi"
        flavorText = "After facing so many horrors, the survivors of Videns parish couldn't comprehend " +
            "the hope that the gryff represented."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42d30894-82b9-4af8-b0bb-48a78acbc4bd.jpg"
    }
}
