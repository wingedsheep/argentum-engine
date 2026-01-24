package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByPower

/**
 * Fleet-Footed Monk
 * {1}{W}
 * Creature — Human Monk
 * 1/1
 * Fleet-Footed Monk can't be blocked by creatures with power 2 or greater.
 */
val FleetFootedMonk = card("Fleet-Footed Monk") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 1

    staticAbility {
        ability = CantBeBlockedByPower(minPower = 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "D. Alexander Gregory"
        flavorText = "Hesitation is for the faithless. My belief lends me speed."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03cd972d-1cc1-4fb1-9b4e-a88ea115cf7f.jpg"
    }
}
