// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter


/**
 * Fleet-Footed Monk
 * {1}{W}
 * Creature — Human Monk
 * 1/1
 * This creature can't be blocked by creatures with power 2 or greater.
 */
val FleetFootedMonk = card("Fleet-Footed Monk") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 1
    staticAbility {
        ability = CantBeBlockedBy(blockerFilter = GameObjectFilter.Creature.powerAtLeast(2))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "D. Alexander Gregory"
        flavorText = "\"Hesitation is for the faithless. My belief lends me speed.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03cd972d-1cc1-4fb1-9b4e-a88ea115cf7f.jpg"
    }
}
