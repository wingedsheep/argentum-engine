// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan


/**
 * Stalking Tiger
 * {3}{G}
 * Creature — Cat
 * 3/3
 * This creature can't be blocked by more than one creature.
 */
val StalkingTiger = card("Stalking Tiger") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 3
    toughness = 3
    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Colin MacNeil"
        flavorText = "No one is happy when they notice a tiger."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbc78337-2d1a-4a1d-8630-fcf7a7f6abce.jpg"
    }
}
