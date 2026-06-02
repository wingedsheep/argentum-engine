package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Lothlórien Lookout
 * {1}{G}
 * Creature — Elf Scout
 * 1/3
 *
 * Whenever this creature attacks, scry 1.
 */
val LothlorienLookout = card("Lothlórien Lookout") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 1
    toughness = 3
    oracleText = "Whenever this creature attacks, scry 1."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = LibraryPatterns.scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Daniel Correia"
        flavorText = "\"That was the custom of the Elves of Lórien, to dwell in the trees. Therefore they were called Galadhrim, the Tree-people.\"\n—Legolas"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e3639c1-ebc3-4a0b-ab93-549e45aff0f7.jpg?1686969459"
    }
}
