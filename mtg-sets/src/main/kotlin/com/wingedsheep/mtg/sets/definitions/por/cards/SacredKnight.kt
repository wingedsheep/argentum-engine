// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter


/**
 * Sacred Knight
 * {3}{W}
 * Creature — Human Knight
 * 3/2
 * This creature can't be blocked by black and/or red creatures.
 */
val SacredKnight = card("Sacred Knight") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 2
    staticAbility {
        ability = CantBeBlockedBy(blockerFilter = GameObjectFilter.Creature.withAnyColor(Color.BLACK, Color.RED))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Donato Giancola"
        flavorText = "\"No flame, no horror can sway me from my cause.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57b19990-c4a2-433d-a9ce-005216e9e4ac.jpg"
    }
}
