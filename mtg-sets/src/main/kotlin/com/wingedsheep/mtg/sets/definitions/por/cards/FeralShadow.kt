// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Feral Shadow
 * {2}{B}
 * Creature — Nightstalker
 * 2/1
 * Flying
 */
val FeralShadow = card("Feral Shadow") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightstalker"
    power = 2
    toughness = 1
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Colin MacNeil"
        flavorText = "Not all shadows are cast by light—some are cast by darkness."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f4c00-6bf8-440b-9761-b17a0e36c27e.jpg"
    }
}
