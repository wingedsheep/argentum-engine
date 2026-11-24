package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Gore-House Chainwalker — Return to Ravnica #96 (canonical printing)
 * {1}{R} · Creature — Human Warrior · 2/1
 *
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 */
val GoreHouseChainwalker = card("Gore-House Chainwalker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)"

    unleash()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56ba132f-95fc-4b99-a1dc-ebe6f622bb41.jpg?1783940355"
    }
}
