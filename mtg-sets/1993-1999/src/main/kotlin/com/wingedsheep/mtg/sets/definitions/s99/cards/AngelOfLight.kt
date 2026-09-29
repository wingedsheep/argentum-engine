package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val AngelOfLight = card("Angel of Light") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Angel"
    oracleText = "Flying, vigilance"
    power = 3
    toughness = 3

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "1"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/750ea219-b62d-49bd-9b30-8e0a62e75553.jpg?1783946053"
    }
}
