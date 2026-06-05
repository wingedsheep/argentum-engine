// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Horned Turtle
 * {2}{U}
 * Creature — Turtle
 * 1/4
 */
val HornedTurtle = card("Horned Turtle") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Turtle"
    power = 1
    toughness = 4
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Adrian Smith"
        flavorText = "Its spear has no shaft, its shield no handle."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7d25497-36b4-48b9-ba01-f24f6222d6be.jpg"
    }
}
