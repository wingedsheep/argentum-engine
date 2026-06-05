// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Redwood Treefolk
 * {4}{G}
 * Creature — Treefolk
 * 3/6
 */
val RedwoodTreefolk = card("Redwood Treefolk") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 3
    toughness = 6
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Steve Luke"
        flavorText = "The soldiers chopped and chopped, and still the great tree stood, frowning down at them."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9399667-ae2a-4b64-84dd-8f97f3e5fe79.jpg"
    }
}
