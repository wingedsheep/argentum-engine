// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Highland Giant
 * {2}{R}{R}
 * Creature — Giant
 * 3/4
 */
val HighlandGiant = card("Highland Giant") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 3
    toughness = 4
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Ron Spencer"
        flavorText = "Though slow-witted and slow-moving, they are quick to anger."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32f49716-1522-4f36-92c9-63ef2059c4ef.jpg"
    }
}
