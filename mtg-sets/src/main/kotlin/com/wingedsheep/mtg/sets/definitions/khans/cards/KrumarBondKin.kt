package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Krumar Bond-Kin
 * {3}{B}{B}
 * Creature — Orc Warrior
 * 5/3
 * Morph {4}{B}
 */
val KrumarBondKin = card("Krumar Bond-Kin") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Orc Warrior"
    power = 5
    toughness = 3
    oracleText = "Morph {4}{B}"

    morph = "{4}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Kev Walker"
        flavorText = "\"The Abzan replaced my savagery with a noble heritage. I would give my life for my House.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57f38391-ffb7-4420-9a38-f791627b12b3.jpg?1562786941"
    }
}
