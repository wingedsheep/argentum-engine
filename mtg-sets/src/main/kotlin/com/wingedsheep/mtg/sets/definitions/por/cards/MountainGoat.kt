// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Mountain Goat
 * {R}
 * Creature — Goat
 * 1/1
 * Mountainwalk (This creature can't be blocked as long as defending player controls a Mountain.)
 */
val MountainGoat = card("Mountain Goat") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goat"
    power = 1
    toughness = 1
    keywords(Keyword.MOUNTAINWALK)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Una Fricker"
        flavorText = "Only the heroic and the mad follow mountain goat trails."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/325100f1-d424-4db0-bfa9-24877156c0ba.jpg"
    }
}
