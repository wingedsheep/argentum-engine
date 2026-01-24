package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lizard Warrior
 * {3}{R}
 * Creature — Lizard Warrior
 * 4/2
 */
val LizardWarrior = card("Lizard Warrior") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Lizard Warrior"
    power = 4
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Tony Szczudlo"
        flavorText = "Don't let its appearance frighten you. Let its claws and teeth do that."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29a0b1c2-d3e4-f5a6-b7c8-d9e0f1a2b3c4.jpg"
    }
}
