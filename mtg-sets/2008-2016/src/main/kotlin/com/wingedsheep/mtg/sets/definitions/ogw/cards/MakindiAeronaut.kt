package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Makindi Aeronaut
 * {1}{W}
 * Creature — Kor Scout Ally
 * 1/3
 * Flying
 */
val MakindiAeronaut = card("Makindi Aeronaut") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Scout Ally"
    power = 1
    toughness = 3
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Matt Stewart"
        flavorText = "\"The Eldrazi have surprised us before. It's my job to make sure that never happens again.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a35d3113-6cb7-4b74-94a3-8160d9ac1b89.jpg"
    }
}
