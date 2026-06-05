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
 * Nature's Lore
 * {1}{G}
 * Sorcery
 * Search your library for a Forest card, put that card onto the battlefield, then shuffle.
 */
val NaturesLore = card("Nature's Lore") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        effect = EffectPatterns.searchLibrary(filter = GameObjectFilter.Land.withSubtype("Forest"), destination = SearchDestination.BATTLEFIELD)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Terese Nielsen"
        flavorText = "Nature's secrets can be read on every tree, every branch, every leaf."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5242227-d033-4e03-b1e6-b334b17bb158.jpg"
    }
}
