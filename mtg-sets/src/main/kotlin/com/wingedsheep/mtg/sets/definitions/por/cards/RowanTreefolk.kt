// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Rowan Treefolk
 * {3}{G}
 * Creature — Treefolk
 * 3/4
 */
val RowanTreefolk = card("Rowan Treefolk") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 3
    toughness = 4
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Gerry Grace"
        flavorText = "One of the forest's best protectors is the forest itself."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/852a0956-8558-4754-ab57-6f1cc4de740e.jpg"
    }
}
