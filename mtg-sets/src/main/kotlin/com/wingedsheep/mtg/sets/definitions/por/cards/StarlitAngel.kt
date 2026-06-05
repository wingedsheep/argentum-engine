// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Starlit Angel
 * {3}{W}{W}
 * Creature — Angel
 * 3/4
 * Flying
 */
val StarlitAngel = card("Starlit Angel") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 4
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Rebecca Guay"
        flavorText = "To soar as high as hope, to dive as swift as justice."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36691cd0-c709-4452-a61a-d6e2049fdfcf.jpg"
    }
}
