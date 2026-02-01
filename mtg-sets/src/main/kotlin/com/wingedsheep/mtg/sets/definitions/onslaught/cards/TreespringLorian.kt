package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Treespring Lorian
 * {5}{G}
 * Creature — Beast
 * 5/4
 * Morph {5}{G} (You may cast this card face down as a 2/2 creature for {3}.
 * Turn it face up any time for its morph cost.)
 */
val TreespringLorian = card("Treespring Lorian") {
    manaCost = "{5}{G}"
    typeLine = "Creature — Beast"
    power = 5
    toughness = 4

    morph = "{5}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "293"
        artist = "Heather Hudson"
        flavorText = "No matter your strength, the Krosan Forest is stronger. No matter your speed, the Wirewood Forest is faster."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f525d7ce-37d3-4989-beb4-173447cb5294.jpg?1562953129"
    }
}
