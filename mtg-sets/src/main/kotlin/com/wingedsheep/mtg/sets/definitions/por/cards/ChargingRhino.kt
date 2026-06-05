// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan


/**
 * Charging Rhino
 * {3}{G}{G}
 * Creature — Rhino
 * 4/4
 * This creature can't be blocked by more than one creature.
 */
val ChargingRhino = card("Charging Rhino") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino"
    power = 4
    toughness = 4
    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Una Fricker"
        flavorText = "A rhino rarely uses its horn to announce its charge, only to end it."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49e47248-051c-4ee6-aad2-352ebd1f38ca.jpg"
    }
}
