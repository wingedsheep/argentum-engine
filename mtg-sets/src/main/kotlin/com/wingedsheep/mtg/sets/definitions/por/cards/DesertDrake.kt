// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Desert Drake
 * {3}{R}
 * Creature — Drake
 * 2/2
 * Flying
 */
val DesertDrake = card("Desert Drake") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Drake"
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Gerry Grace"
        flavorText = "Never doubt a dragon just because of its size."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24673b35-aed2-40c0-a4ae-93bc4d392562.jpg"
    }
}
