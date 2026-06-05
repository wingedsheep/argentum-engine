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
 * Untamed Wilds
 * {2}{G}
 * Sorcery
 * Search your library for a basic land card, put that card onto the battlefield, then shuffle.
 */
val UntamedWilds = card("Untamed Wilds") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        effect = EffectPatterns.searchLibrary(filter = GameObjectFilter.BasicLand, destination = SearchDestination.BATTLEFIELD)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Romas Kukalis"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f4fd77e-ee43-4de7-9ee8-1075ff70b5e7.jpg"
    }
}
