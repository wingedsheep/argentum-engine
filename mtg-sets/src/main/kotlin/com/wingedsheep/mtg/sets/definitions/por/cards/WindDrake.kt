// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Wind Drake
 * {2}{U}
 * Creature — Drake
 * 2/2
 * Flying
 */
val WindDrake = card("Wind Drake") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Zina Saunders"
        flavorText = "\"No bird soars too high, if he soars with his own wings.\"\n—William Blake, The Marriage of Heaven and Hell"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/5486d2dc-9a5d-4f58-a5ec-d94de54b852f.jpg"
    }
}
