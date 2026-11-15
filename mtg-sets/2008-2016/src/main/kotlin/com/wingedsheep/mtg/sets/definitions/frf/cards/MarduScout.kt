package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mardu Scout
 * {R}{R}
 * Creature — Goblin Scout
 * 3/1
 * Dash {1}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)
 */
val MarduScout = card("Mardu Scout") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Scout"
    power = 3
    toughness = 1
    oracleText = "Dash {1}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    dash = "{1}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Zoltan Boros"
        flavorText = "The Mardu all enjoy war, but only the goblins make a game of it."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e7891b8-ba69-4c5b-a29f-ee7bcf2374f0.jpg"
    }
}
