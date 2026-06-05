// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Willow Dryad
 * {G}
 * Creature — Dryad
 * 1/1
 * Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)
 */
val WillowDryad = card("Willow Dryad") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    power = 1
    toughness = 1
    keywords(Keyword.FORESTWALK)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "193"
        artist = "D. Alexander Gregory"
        flavorText = "Some think that dryads are the dreams of trees."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1def835b-aabb-4a9d-8fb9-460452de0d79.jpg"
    }
}
