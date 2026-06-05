// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player


/**
 * Winds of Change
 * {R}
 * Sorcery
 * Each player shuffles the cards from their hand into their library, then draws that many cards.
 */
val WindsofChange = card("Winds of Change") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        effect = EffectPatterns.wheelEffect(Player.Each)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/735b8aec-62d4-46db-9a68-a6c69cb6fd98.jpg"
    }
}
