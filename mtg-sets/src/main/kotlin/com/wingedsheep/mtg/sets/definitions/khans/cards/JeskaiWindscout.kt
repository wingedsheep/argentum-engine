package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jeskai Windscout
 * {2}{U}
 * Creature — Bird Scout
 * 2/1
 * Flying
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
val JeskaiWindscout = card("Jeskai Windscout") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Bird Scout"
    power = 2
    toughness = 1
    oracleText = "Flying\nProwess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"

    keywords(Keyword.FLYING)
    prowess()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Johann Bodin"
        flavorText = "They range from Sage-Eye Stronghold to the farthest reaches of Tarkir."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66356e38-38e1-4b09-80c2-be26007ff99c.jpg?1562787785"
    }
}
