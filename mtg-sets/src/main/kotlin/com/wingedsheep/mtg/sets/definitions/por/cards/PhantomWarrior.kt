// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Phantom Warrior
 * {1}{U}{U}
 * Creature — Illusion Warrior
 * 2/2
 * This creature can't be blocked.
 */
val PhantomWarrior = card("Phantom Warrior") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion Warrior"
    power = 2
    toughness = 2
    flags(AbilityFlag.CANT_BE_BLOCKED)
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Dan Frazier"
        flavorText = "The phantom warrior comes from nowhere and returns there just as quickly."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dbcb0df-d1cc-4718-ba1e-b590852cce20.jpg"
    }
}
