package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Slip Out the Back
 * {U}
 * Instant
 * Put a +1/+1 counter on target creature. It phases out. (Treat it and anything attached to it
 * as though they don't exist until its controller's next turn.)
 */
val SlipOutTheBack = card("Slip Out the Back") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature. It phases out. (Treat it and anything " +
        "attached to it as though they don't exist until its controller's next turn.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then
            Effects.PhaseOut(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Zara Alfonso"
        flavorText = "\"I was never here.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8725f4c4-fad7-460e-b86c-ff81674f0980.jpg?1783923138"
    }
}
