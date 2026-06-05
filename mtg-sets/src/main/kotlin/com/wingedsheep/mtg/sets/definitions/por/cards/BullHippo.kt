// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Bull Hippo
 * {3}{G}
 * Creature — Hippo
 * 3/3
 * Islandwalk (This creature can't be blocked as long as defending player controls an Island.)
 */
val BullHippo = card("Bull Hippo") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hippo"
    power = 3
    toughness = 3
    keywords(Keyword.ISLANDWALK)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Roger Raupp"
        flavorText = "Stay alert: his lumbering may shake the ground itself, but in water, he glides."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30dd236b-94fc-4c56-aeae-215c71a009ea.jpg"
    }
}
