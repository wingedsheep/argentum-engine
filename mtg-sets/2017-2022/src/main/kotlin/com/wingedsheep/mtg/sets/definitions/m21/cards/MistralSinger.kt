package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mistral Singer
 * {2}{U}
 * Creature — Siren
 * 2/2
 * Flying
 * Prowess
 */
val MistralSinger = card("Mistral Singer") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"

    keywords(Keyword.FLYING)
    prowess()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Ryan Yee"
        flavorText = "\"I sang of storms, there came a squall. I sang of waves, the seas grew tall. I sang of love, " +
            "but no voice called.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d961c441-b76b-4bd8-b510-a3e073207a1b.jpg?1783930725"
    }
}
