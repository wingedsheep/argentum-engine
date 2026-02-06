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
        imageUri = "https://cards.scryfall.io/large/front/a/8/a804f3c0-5ebf-43ca-b200-09f7c1bbe902.jpg?1562934820"
    }
}
