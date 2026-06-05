// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination


/**
 * Sylvan Tutor
 * {G}
 * Sorcery
 * Search your library for a creature card, reveal it, then shuffle and put that card on top.
 */
val SylvanTutor = card("Sylvan Tutor") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        effect = EffectPatterns.searchLibrary(filter = GameObjectFilter.Creature, destination = SearchDestination.TOP_OF_LIBRARY, reveal = true)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/287ba07e-6434-4850-940f-454fcab3f535.jpg"
    }
}
