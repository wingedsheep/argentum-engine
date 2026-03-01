package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodfire Expert
 * {2}{R}
 * Creature — Efreet Monk
 * 3/1
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
val BloodfireExpert = card("Bloodfire Expert") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Efreet Monk"
    power = 3
    toughness = 1
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"

    prowess()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Raymond Swanland"
        flavorText = "Some efreet abandon their homes in the volcanic Fire Rim to embrace the Jeskai Way and discipline their innate flames."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0742564e-f7a7-4107-b9ed-63703ca4702c.jpg?1562782152"
    }
}
