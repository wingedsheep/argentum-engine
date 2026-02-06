package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Krosan Colossus
 * {6}{G}{G}{G}
 * Creature — Beast
 * 9/9
 * Morph {6}{G}{G} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 */
val KrosanColossus = card("Krosan Colossus") {
    manaCost = "{6}{G}{G}{G}"
    typeLine = "Creature — Beast"
    power = 9
    toughness = 9

    morph = "{6}{G}{G}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "270"
        artist = "Arnie Swekel"
        flavorText = "It doesn't think. It doesn't feel. It just grows."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d4c8c32-4e28-40e6-8aa1-9a0e649b78ec.jpg?1562905078"
    }
}
