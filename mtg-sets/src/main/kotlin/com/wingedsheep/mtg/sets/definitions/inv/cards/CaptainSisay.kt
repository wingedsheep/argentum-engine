package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Captain Sisay
 * {2}{G}{W}
 * Legendary Creature — Human Soldier
 * 2/2
 * {T}: Search your library for a legendary card, reveal that card, put it into
 *      your hand, then shuffle.
 */
val CaptainSisay = card("Captain Sisay") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "{T}: Search your library for a legendary card, reveal that card, put it into your hand, then shuffle."

    activatedAbility {
        cost = Costs.Tap
        effect = EffectPatterns.searchLibrary(
            filter = GameObjectFilter.Any.legendary(),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Ray Lago"
        flavorText = "Her leadership forged the Weatherlight's finest crew."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d24d441c-f37f-44fe-8a93-f5c89df807e4.jpg?1562937244"
    }
}
