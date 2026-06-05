// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Giant Octopus
 * {3}{U}
 * Creature — Octopus
 * 3/3
 */
val GiantOctopus = card("Giant Octopus") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus"
    power = 3
    toughness = 3
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "John Matson"
        flavorText = "At the sight of the thing the calamari vendor's eyes went wide, but from fear or avarice none could tell."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4528edca-cc36-4f63-9615-24ca315d672c.jpg"
    }
}
