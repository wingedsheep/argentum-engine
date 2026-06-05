// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Raging Cougar
 * {2}{R}
 * Creature — Cat
 * 2/2
 * Haste
 */
val RagingCougar = card("Raging Cougar") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 2
    keywords(Keyword.HASTE)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Terese Nielsen"
        flavorText = "Mountaineers quickly learn to travel with their spears always pointed up."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd9d126a-9db9-4adc-9cf6-11408c63201d.jpg"
    }
}
