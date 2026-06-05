// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Armored Pegasus
 * {1}{W}
 * Creature — Pegasus
 * 1/2
 * Flying
 */
val ArmoredPegasus = card("Armored Pegasus") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Pegasus"
    power = 1
    toughness = 2
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Andrew Robinson"
        flavorText = "Asked how it survived a run-in with a bog imp, the pegasus just shook its mane and burped."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a81b61af-cdb7-468f-9ff0-db82aa084023.jpg"
    }
}
