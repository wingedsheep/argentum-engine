// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Merfolk of the Pearl Trident
 * {U}
 * Creature — Merfolk
 * 1/1
 */
val MerfolkofthePearlTrident = card("Merfolk of the Pearl Trident") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    power = 1
    toughness = 1
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "DiTerlizzi"
        flavorText = "Are merfolk humans with fins, or are humans merfolk with feet?"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/126fec7a-4f36-49e5-a2d7-96deb7af856f.jpg"
    }
}
