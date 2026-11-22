package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Burst of Strength
 * {G}
 * Instant
 * Put a +1/+1 counter on target creature and untap it.
 */
val BurstOfStrength = card("Burst of Strength") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature and untap it."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Marco Nelor"
        flavorText = "\"Our fight is not about territory or revenge. It is about freedom, plain and simple.\"\n—Ruric Thar, Ghor-Clan chieftain"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1cbd617-9f2e-4882-b8f0-dfc2fced2281.jpg"
    }
}
