package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Crude Rampart
 * {3}{W}
 * Creature — Wall
 * 4/5
 * Defender
 * Morph {4}{W} (You may cast this card face down as a 2/2 creature for {3}.
 * Turn it face up any time for its morph cost.)
 */
val CrudeRampart = card("Crude Rampart") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Wall"
    power = 4
    toughness = 5

    keywords(Keyword.DEFENDER)
    morph = "{4}{W}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Sam Wood"
        flavorText = "Success is one part inspiration, nine parts desperation."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af5d1be2-d6ae-4820-aa01-62f261b0f110.jpg?1562936464"
    }
}
