package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Dig Through Time
 * {6}{U}{U}
 * Instant
 * Delve
 * Look at the top seven cards of your library. Put two of them into your hand
 * and the rest on the bottom of your library in any order.
 */
val DigThroughTime = card("Dig Through Time") {
    manaCost = "{6}{U}{U}"
    typeLine = "Instant"

    keywords(Keyword.DELVE)

    spell {
        effect = EffectPatterns.lookAtTopAndKeep(
            count = 7,
            keepCount = 2,
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Ryan Yee"
        flavorText = "The Jeskai believe that a single moment of clarity is worth a lifetime of study."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf2a7655-9749-4ff6-b3b3-5a023b74a100.jpg?1562793264"
    }
}
