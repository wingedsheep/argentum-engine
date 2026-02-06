package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Towering Baloth
 * {6}{G}{G}
 * Creature — Beast
 * 7/6
 * Morph {6}{G} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 */
val ToweringBaloth = card("Towering Baloth") {
    manaCost = "{6}{G}{G}"
    typeLine = "Creature — Beast"
    power = 7
    toughness = 6

    morph = "{6}{G}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "292"
        artist = "Arnie Swekel"
        flavorText = "The Mirari twisted all that lived in the Krosan Forest into gross mockeries of their former selves."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a8cc948-28ff-4bbe-b8c9-71de37478023.jpg?1562905065"
    }
}
