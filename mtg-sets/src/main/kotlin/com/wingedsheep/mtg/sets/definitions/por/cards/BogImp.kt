// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Bog Imp
 * {1}{B}
 * Creature — Imp
 * 1/1
 * Flying (This creature can't be blocked except by creatures with flying or reach.)
 */
val BogImp = card("Bog Imp") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Imp"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Christopher Rush"
        flavorText = "Don't be fooled by their looks. Think of them as little knives with wings."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8681b3fd-33e5-4a45-8650-a4a142405096.jpg"
    }
}
