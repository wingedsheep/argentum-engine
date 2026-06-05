// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Prosperity
 * {X}{U}
 * Sorcery
 * Each player draws X cards.
 */
val Prosperity = card("Prosperity") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        effect = EffectPatterns.eachPlayerDrawsX(includeController = true, includeOpponents = true)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Phil Foglio"
        flavorText = "\"Life can never be too good.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/6/269bb4fc-9d8f-42cc-8f71-6a658e41533c.jpg"
    }
}
