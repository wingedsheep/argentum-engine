package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spitting Gourna
 * {3}{G}{G}
 * Creature — Beast
 * 3/4
 * Reach
 * Morph {4}{G} (You may cast this card face down as a 2/2 creature for {3}.
 * Turn it face up any time for its morph cost.)
 */
val SpittingGourna = card("Spitting Gourna") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 4
    oracleText = "Reach\nMorph {4}{G}"

    keywords(Keyword.REACH)
    morph = "{4}{G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "284"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/746b98bf-5398-4a00-b4fe-a990ea9cfd77.jpg?1562922510"
    }
}
