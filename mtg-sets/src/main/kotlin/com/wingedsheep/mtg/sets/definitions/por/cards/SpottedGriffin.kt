// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Spotted Griffin
 * {3}{W}
 * Creature — Griffin
 * 2/3
 * Flying
 */
val SpottedGriffin = card("Spotted Griffin") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Griffin"
    power = 2
    toughness = 3
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "William Simpson"
        flavorText = "When the cat flies and the bird stalks, guard your horses carefully."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f5b708b-368f-48d2-8eca-40f2ae6d5178.jpg"
    }
}
