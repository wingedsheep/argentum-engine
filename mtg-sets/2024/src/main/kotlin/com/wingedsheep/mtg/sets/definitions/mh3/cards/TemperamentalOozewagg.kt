package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Temperamental Oozewagg (MH3 #172)
 * {3}{G}
 * Creature — Ooze Brushwagg
 * 4/4
 * {2}{G}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)
 * Modified creatures you control have trample. (Equipment, Auras you control, and counters are
 * modifications.)
 *
 * Adapt is the zero-counter gate over AddCounters (CR 701.46a), checked on resolution. "Modified"
 * (CR 700.9) is [StatePredicate.IsModified] narrowing a creatures-you-control group.
 */
val TemperamentalOozewagg = card("Temperamental Oozewagg") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ooze Brushwagg"
    power = 4
    toughness = 4
    oracleText = "{2}{G}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)\n" +
        "Modified creatures you control have trample. (Equipment, Auras you control, and counters are modifications.)"

    // {2}{G}: Adapt 2.
    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
        )
    }

    // Modified creatures you control have trample.
    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature.youControl().withStatePredicate(StatePredicate.IsModified)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Pete Venters"
        flavorText = "Its position on the food chain causes great confusion in everyone from hunters to other monsters."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6625df2e-7046-411a-ae86-c46ac0953a0b.jpg?1783911256"
    }
}
