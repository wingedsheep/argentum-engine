// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination


/**
 * Natural Order
 * {2}{G}{G}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a green creature.
 * Search your library for a green creature card, put it onto the battlefield, then shuffle.
 */
val NaturalOrder = card("Natural Order") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    additionalCost(AdditionalCost.SacrificePermanent(GameObjectFilter.Creature.withColor(Color.GREEN)))
    spell {
        effect = EffectPatterns.searchLibrary(filter = GameObjectFilter.Creature.withColor(Color.GREEN), destination = SearchDestination.BATTLEFIELD)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Alan Rabinowitz"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cecb34f8-6961-4c27-9368-26d156714d7b.jpg"
    }
}
