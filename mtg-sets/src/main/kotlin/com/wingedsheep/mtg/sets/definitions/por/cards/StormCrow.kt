// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Storm Crow
 * {1}{U}
 * Creature — Bird
 * 1/2
 * Flying (This creature can't be blocked except by creatures with flying or reach.)
 */
val StormCrow = card("Storm Crow") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 2
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Una Fricker"
        flavorText = "Storm crow descending, winter unending. Storm crow departing, summer is starting."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfe87b59-b456-4532-a695-0dea3110d878.jpg"
    }
}
