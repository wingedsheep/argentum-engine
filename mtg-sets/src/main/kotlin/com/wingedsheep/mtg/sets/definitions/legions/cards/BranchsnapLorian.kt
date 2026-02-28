package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Branchsnap Lorian
 * {1}{G}{G}
 * Creature — Beast
 * 4/1
 * Trample
 * Morph {G}
 */
val BranchsnapLorian = card("Branchsnap Lorian") {
    manaCost = "{1}{G}{G}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 1
    keywords(Keyword.TRAMPLE)
    oracleText = "Trample\nMorph {G} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    morph = "{G}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Heather Hudson"
        flavorText = "Lorians treat trees and prey the same way: by ripping them limb from limb."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52118ff1-ad76-4b97-9fdc-6adfe80140f8.jpg?1562911651"
    }
}
