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
 * Cloud Pirates
 * {U}
 * Creature — Human Pirate
 * 1/1
 * Flying
 * This creature can block only creatures with flying.
 */
val CloudPirates = card("Cloud Pirates") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)
    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f7386c6-d17a-4c7d-884d-2471b87d8b8e.jpg"
    }
}
