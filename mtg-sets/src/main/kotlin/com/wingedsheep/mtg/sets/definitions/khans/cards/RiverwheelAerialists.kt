package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Riverwheel Aerialists
 * {5}{U}
 * Creature — Djinn Monk
 * 4/5
 * Flying
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
val RiverwheelAerialists = card("Riverwheel Aerialists") {
    manaCost = "{5}{U}"
    typeLine = "Creature — Djinn Monk"
    power = 4
    toughness = 5
    oracleText = "Flying\nProwess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"

    keywords(Keyword.FLYING)
    prowess()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Jack Wang"
        flavorText = "Adepts of the Riverwheel Stronghold can run through rain and never get wet; masters use the raindrops as stepping stones."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c210b102-8a94-4b5e-858a-777fa8ad18b9.jpg?1562793060"
    }
}
