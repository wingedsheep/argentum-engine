// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TakeExtraTurnEffect


/**
 * Last Chance
 * {R}{R}
 * Sorcery
 * Take an extra turn after this one. At the beginning of that turn's end step, you lose the game.
 */
val LastChance = card("Last Chance") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        effect = TakeExtraTurnEffect(loseAtEndStep = true)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Hannibal King"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86f2c423-1694-466e-9a7d-4ec99e53578d.jpg"
    }
}
