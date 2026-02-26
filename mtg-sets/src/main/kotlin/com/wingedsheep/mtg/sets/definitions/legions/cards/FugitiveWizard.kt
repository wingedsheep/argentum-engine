package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fugitive Wizard
 * {U}
 * Creature — Human Wizard
 * 1/1
 */
val FugitiveWizard = card("Fugitive Wizard") {
    manaCost = "{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Jim Nelson"
        flavorText = "Many wizards came to the Riptide Project hoping to return home with answers. Most, however, wouldn't return home at all."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1020538-89c8-4986-9687-78ab326acb3e.jpg?1562927553"
    }
}
