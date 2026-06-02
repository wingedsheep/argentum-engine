package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Galadhrim Guide
 * {3}{G}
 * Creature — Elf Scout
 * 3/4
 *
 * When this creature enters, scry 2.
 */
val GaladhrimGuide = card("Galadhrim Guide") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, scry 2."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Inka Schulz"
        flavorText = "\"I shall lead you well, and the paths are smooth and straight.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4603f59-f899-4caf-a874-bf234d2045fb.jpg?1686969386"
    }
}
