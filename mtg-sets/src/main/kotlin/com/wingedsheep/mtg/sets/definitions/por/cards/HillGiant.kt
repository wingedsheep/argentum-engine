// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Hill Giant
 * {3}{R}
 * Creature — Giant
 * 3/3
 */
val HillGiant = card("Hill Giant") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 3
    toughness = 3
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Randy Gallegos"
        flavorText = "Hill giants are mostly just big. Of course, that does count for a lot!"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cd36579-c108-40c0-bce4-38ab837a8c65.jpg"
    }
}
