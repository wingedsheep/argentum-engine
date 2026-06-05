// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Snapping Drake
 * {3}{U}
 * Creature — Drake
 * 3/2
 * Flying (This creature can't be blocked except by creatures with flying or reach.)
 */
val SnappingDrake = card("Snapping Drake") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    power = 3
    toughness = 2
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Christopher Rush"
        flavorText = "Drakes claim to be dragons—until the dragons show up."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b49dd4c-ead6-4d94-9acb-0518d1f6426e.jpg"
    }
}
