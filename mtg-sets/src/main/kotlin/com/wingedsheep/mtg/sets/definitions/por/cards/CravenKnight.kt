// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock


/**
 * Craven Knight
 * {1}{B}
 * Creature — Human Knight
 * 2/2
 * This creature can't block.
 */
val CravenKnight = card("Craven Knight") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    staticAbility {
        ability = CantBlock()
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Charles Gillespie"
        flavorText = "\"I say victory is better than honor.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4cbae27-4a1a-4e16-8876-9a2925c45302.jpg"
    }
}
