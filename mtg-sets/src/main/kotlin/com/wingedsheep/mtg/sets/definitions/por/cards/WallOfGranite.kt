// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Wall of Granite
 * {2}{R}
 * Creature — Wall
 * 0/7
 * Defender (This creature can't attack.)
 */
val WallofGranite = card("Wall of Granite") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 7
    keywords(Keyword.DEFENDER)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Kev Walker"
        flavorText = "The wisest man builds his house behind the rock."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70c5ac71-bf45-4b99-8184-36ce88dd728a.jpg"
    }
}
