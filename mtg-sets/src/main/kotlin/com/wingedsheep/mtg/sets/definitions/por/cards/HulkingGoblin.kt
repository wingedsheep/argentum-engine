// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock


/**
 * Hulking Goblin
 * {1}{R}
 * Creature — Goblin
 * 2/2
 * This creature can't block.
 */
val HulkingGoblin = card("Hulking Goblin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 2
    staticAbility {
        ability = CantBlock()
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Pete Venters"
        flavorText = "The bigger they are, the harder they avoid work."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e3eead8-7e07-4463-9e67-c396d2d7931e.jpg"
    }
}
