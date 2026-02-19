package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scornful Egotist
 * {7}{U}
 * Creature — Human Wizard
 * 1/1
 * Morph {U}
 */
val ScornfulEgotist = card("Scornful Egotist") {
    manaCost = "{7}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "Morph {U}"

    morph = "{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Jim Nelson"
        flavorText = "Once I was human. Now I am far more."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fec6b189-97e7-4627-9785-a9ce2f1ad89f.jpg?1562537398"
    }
}
