// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Foot Soldiers
 * {3}{W}
 * Creature — Human Soldier
 * 2/4
 */
val FootSoldiers = card("Foot Soldiers") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 4
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Kev Walker"
        flavorText = "Infantry deployment is the art of putting your troops in the wrong place at the right time."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/458ddb33-66c4-4753-b1eb-8937ab812a81.jpg"
    }
}
