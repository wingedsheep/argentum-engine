// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Panther Warriors
 * {4}{G}
 * Creature — Cat Warrior
 * 6/3
 */
val PantherWarriors = card("Panther Warriors") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat Warrior"
    power = 6
    toughness = 3
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Eric Peterson"
        flavorText = "The dogs of war are nothing compared to the cats."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8be610ce-5b84-416e-b427-98887642ff01.jpg"
    }
}
