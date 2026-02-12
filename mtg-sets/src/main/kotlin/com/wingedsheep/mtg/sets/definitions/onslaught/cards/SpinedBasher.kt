package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spined Basher
 * {2}{B}
 * Creature — Zombie Beast
 * 3/1
 * Morph {2}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 */
val SpinedBasher = card("Spined Basher") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie Beast"
    power = 3
    toughness = 1
    oracleText = "Morph {2}{B}"

    morph = "{2}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Thomas M. Baxa"
        flavorText = "This species of zombie is even more unpleasant than the garden variety."
        imageUri = "https://cards.scryfall.io/large/front/4/d/4d0d666a-8e31-466c-937f-54df910f664e.jpg?1562913024"
    }
}
