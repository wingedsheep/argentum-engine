package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter

/**
 * Crowned Ceratok
 * {3}{G}
 * Creature — Rhino
 * 4/3
 * Trample
 * Each creature you control with a +1/+1 counter on it has trample.
 */
val CrownedCeratok = card("Crowned Ceratok") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino"
    power = 4
    toughness = 3
    oracleText = "Trample\nEach creature you control with a +1/+1 counter on it has trample."

    keywords(Keyword.TRAMPLE)

    // Each creature you control with a +1/+1 counter on it has trample.
    staticAbility { ability = GrantKeywordByCounter(Keyword.TRAMPLE, CounterType.PLUS_ONE_PLUS_ONE, controllerOnly = true) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Steve Prescott"
        flavorText = "The best way to calm a rampaging ceratok is to wait until it dies of natural causes."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7eacc64-f418-4df0-bd8a-6b0036d0d2a1.jpg?1783940119"
    }
}
