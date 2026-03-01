package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Canyon Lurkers
 * {4}{R}
 * Creature — Human Rogue
 * 5/2
 * Morph {3}{R}
 */
val CanyonLurkers = card("Canyon Lurkers") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Human Rogue"
    power = 5
    toughness = 2
    oracleText = "Morph {3}{R}"

    morph = "{3}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Cynthia Sheppard"
        flavorText = "\"The broken Qal Sisma foothills make poor terrain for cavalry, but a perfect setting for ambushes.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef6c56d1-6eca-43be-9834-0478bea67b48.jpg?1562795756"
    }
}
