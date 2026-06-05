// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter


/**
 * Cloud Spirit
 * {2}{U}
 * Creature — Spirit
 * 3/1
 * Flying
 * This creature can block only creatures with flying.
 */
val CloudSpirit = card("Cloud Spirit") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 1
    keywords(Keyword.FLYING)
    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc7547aa-fcf7-4b6e-955d-cc5ebc40cd7d.jpg"
    }
}
