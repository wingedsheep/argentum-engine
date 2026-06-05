// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Djinn of the Lamp
 * {5}{U}{U}
 * Creature — Djinn
 * 5/6
 * Flying
 */
val DjinnoftheLamp = card("Djinn of the Lamp") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn"
    power = 5
    toughness = 6
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "DiTerlizzi"
        flavorText = "Once they learn the trick of carrying their own lamps, they never touch the ground again."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a5e7b52-2663-4140-9758-f24b8b947876.jpg"
    }
}
