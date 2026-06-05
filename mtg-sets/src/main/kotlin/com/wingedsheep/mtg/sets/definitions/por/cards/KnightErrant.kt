// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Knight Errant
 * {1}{W}
 * Creature — Human Knight
 * 2/2
 */
val KnightErrant = card("Knight Errant") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Dan Frazier"
        flavorText = "\". . . [B]efore honor is humility.\"\n—The Bible, Proverbs 15:33"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c31b4b4-18fc-4a6e-8d74-fd5340964320.jpg"
    }
}
