package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Needleshot Gourna
 * {4}{G}{G}
 * Creature — Beast
 * 3/6
 * Reach
 */
val NeedleshotGourna = card("Needleshot Gourna") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 6
    oracleText = "Reach"

    keywords(Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Edward P. Beard, Jr."
        flavorText = "The first aven scout squad returned from Krosa with disturbing stories. The second returned with disturbing casualties."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9b1628d-aacd-4e19-9ebb-bcd9b2842c91.jpg?1562945371"
    }
}
