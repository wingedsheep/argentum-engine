package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.card

val SootfeatherFlock = card("Sootfeather Flock") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 2
    oracleText = "Flying\nMorph {3}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    keywords(Keyword.FLYING)

    morph = "{3}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "David Martin"
        flavorText = "They pick at the remains of the city's corpse."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/216a2ccc-8847-452b-b030-27d8506675bd.jpg?1562901660"
    }
}
