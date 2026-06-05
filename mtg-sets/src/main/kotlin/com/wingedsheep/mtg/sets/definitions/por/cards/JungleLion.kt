// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock


/**
 * Jungle Lion
 * {G}
 * Creature — Cat
 * 2/1
 * This creature can't block.
 */
val JungleLion = card("Jungle Lion") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 1
    staticAbility {
        ability = CantBlock()
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Janine Johnston"
        flavorText = "The lion's only loyalty is to its hunger."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/613ceee3-92c7-46f1-8267-d6229ab15df5.jpg"
    }
}
