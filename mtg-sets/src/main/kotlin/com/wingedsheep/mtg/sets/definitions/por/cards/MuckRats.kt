// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Muck Rats
 * {B}
 * Creature — Rat
 * 1/1
 */
val MuckRats = card("Muck Rats") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 1
    toughness = 1
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Colin MacNeil"
        flavorText = "The difference between a nuisance and a threat is often merely a matter of numbers."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4041226-7ce2-46d1-8844-20fa50b6568a.jpg"
    }
}
