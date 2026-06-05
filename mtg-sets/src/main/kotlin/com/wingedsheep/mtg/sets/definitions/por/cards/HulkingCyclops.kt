// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock


/**
 * Hulking Cyclops
 * {3}{R}{R}
 * Creature — Cyclops
 * 5/5
 * This creature can't block.
 */
val HulkingCyclops = card("Hulking Cyclops") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cyclops"
    power = 5
    toughness = 5
    staticAbility {
        ability = CantBlock()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Paolo Parente"
        flavorText = "Anyone can get around a cyclops, but few can stand in its way."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f20ae982-8a70-4dd3-8254-0d447954f580.jpg"
    }
}
