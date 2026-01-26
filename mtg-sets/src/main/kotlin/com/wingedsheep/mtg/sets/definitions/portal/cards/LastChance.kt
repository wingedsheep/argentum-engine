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
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86f2c423-1694-466e-9a7d-4ec99e53578d.jpg"
    }
}
