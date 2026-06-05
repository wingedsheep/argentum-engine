// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Ancestral Memories
 * {2}{U}{U}{U}
 * Sorcery
 * Look at the top seven cards of your library. Put two of them into your hand and the rest into your graveyard.
 */
val AncestralMemories = card("Ancestral Memories") {
    manaCost = "{2}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        effect = EffectPatterns.lookAtTopAndKeep(count = 7, keepCount = 2)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf9b613c-61bf-4c2d-9c90-2949e442aea5.jpg"
    }
}
