// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Flux
 * {2}{U}
 * Sorcery
 * Each player discards any number of cards, then draws that many cards.
 * Draw a card.
 */
val Flux = card("Flux") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        effect = EffectPatterns.eachPlayerDiscardsDraws(controllerBonusDraw = 1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Ted Naifeh"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c26bf66-8fa8-4f69-9556-c9fcc56a7f33.jpg"
    }
}
