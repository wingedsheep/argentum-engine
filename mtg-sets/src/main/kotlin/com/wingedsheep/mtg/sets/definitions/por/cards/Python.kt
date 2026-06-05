// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Python
 * {1}{B}{B}
 * Creature — Snake
 * 3/2
 */
val Python = card("Python") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake"
    power = 3
    toughness = 2
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Alan Rabinowitz"
        flavorText = "What could it have swallowed to grow so large?"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82c552a1-6245-4caf-8249-765ce7ea80d2.jpg"
    }
}
