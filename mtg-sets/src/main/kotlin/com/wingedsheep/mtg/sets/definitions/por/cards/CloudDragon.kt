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
 * Cloud Dragon
 * {5}{U}
 * Creature — Illusion Dragon
 * 5/4
 * Flying
 * This creature can block only creatures with flying.
 */
val CloudDragon = card("Cloud Dragon") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion Dragon"
    power = 5
    toughness = 4
    keywords(Keyword.FLYING)
    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bb7fb59-65c0-4af6-9d3a-79cd6602d004.jpg"
    }
}
