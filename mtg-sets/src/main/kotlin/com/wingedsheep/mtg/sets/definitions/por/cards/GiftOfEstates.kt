// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination


/**
 * Gift of Estates
 * {1}{W}
 * Sorcery
 * If an opponent controls more lands than you, search your library for up to three Plains cards, reveal them, put them into your hand, then shuffle.
 */
val GiftofEstates = card("Gift of Estates") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        condition = Conditions.OpponentControlsMoreLands
        effect = EffectPatterns.searchLibrary(filter = GameObjectFilter.Land.withSubtype("Plains"), count = 3, destination = SearchDestination.HAND, reveal = true)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/342b5afe-544f-4fa1-a833-4e0590b41eed.jpg"
    }
}
