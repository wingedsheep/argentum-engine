// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Coral Eel
 * {1}{U}
 * Creature — Fish
 * 2/1
 */
val CoralEel = card("Coral Eel") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish"
    power = 2
    toughness = 1
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Una Fricker"
        flavorText = "Some fishers like to eat eels, and some eels like to eat fishers."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35bbb10f-c118-4905-8329-3963af415178.jpg"
    }
}
