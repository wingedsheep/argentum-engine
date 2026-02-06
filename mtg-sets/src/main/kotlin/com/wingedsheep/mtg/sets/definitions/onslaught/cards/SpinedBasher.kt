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

    morph = "{2}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Mark Tedin"
        flavorText = "This species of zombie is even more unpleasant than the garden variety."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3f40cc6-681d-4267-8fbc-a99bad7b1218.jpg?1562938225"
    }
}
