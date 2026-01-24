package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TakeExtraTurnEffect

/**
 * Last Chance
 * {R}{R}
 * Sorcery
 * Take an extra turn after this one. At the beginning of that turn's end step, you lose the game.
 */
val LastChance = card("Last Chance") {
    manaCost = "{R}{R}"
    typeLine = "Sorcery"

    spell {
        effect = TakeExtraTurnEffect(loseAtEndStep = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "David A. Cherry"
        flavorText = "One final chance to seize victory—or embrace defeat."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6a7b8c9-d0e1-f2a3-b4c5-d6e7f8a9b0c1.jpg"
    }
}
