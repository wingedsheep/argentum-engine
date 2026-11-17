package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lotus Path Djinn
 * {3}{U}
 * Creature — Djinn Monk
 * 2/3
 *
 * Flying
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
val LotusPathDjinn = card("Lotus Path Djinn") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn Monk"
    oracleText = "Flying\n" +
        "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)
    prowess()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Steve Argyle"
        flavorText = "\"The lotus takes root where body and mind intersect. It blooms when body and mind become one.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/9/197ec248-70d9-47f4-a53b-99b23f021116.jpg?1783938707"
    }
}
