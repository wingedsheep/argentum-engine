package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Free from Flesh
 * {R}
 * Instant
 * Target creature gets +2/+2 until end of turn. Put two oil counters on it.
 */
val FreeFromFlesh = card("Free from Flesh") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. Put two oil counters on it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature) then
            Effects.AddCounters(CounterType.OIL, 2, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Isis"
        flavorText = "He finally understood that phyresis was not an ending, but a new, perfect beginning."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c83600d-ea4d-4219-8dbb-34c4215a2005.jpg?1783918031"
    }
}
