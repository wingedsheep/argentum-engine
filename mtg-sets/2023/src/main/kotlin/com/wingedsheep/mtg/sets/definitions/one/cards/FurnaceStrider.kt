package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Furnace Strider
 * {4}{R}
 * Creature — Phyrexian Beast
 * 4/5
 *
 * This creature enters with two oil counters on it.
 * Remove an oil counter from this creature: Target creature you control gains haste until end of turn.
 */
val FurnaceStrider = card("Furnace Strider") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phyrexian Beast"
    power = 4
    toughness = 5
    oracleText = "This creature enters with two oil counters on it.\n" +
        "Remove an oil counter from this creature: Target creature you control gains haste until end of turn."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 2, selfOnly = true))

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.OIL, 1)
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.HASTE, target = t)
        description = "Remove an oil counter from this creature: Target creature you control gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Denis Zhbankov"
        flavorText = "With the Quiet Furnace closed to all other factions, Urabrask's smiths were free to build anything their creativity demanded."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa625ab0-1e79-4497-a5da-98fe1abfd024.jpg?1783918030"
    }
}
