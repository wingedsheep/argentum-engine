package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lead by Example — Oath of the Gatewatch #134 (canonical printing; reprinted in Battlebond)
 * {1}{G} · Instant
 *
 * Support 2. (Put a +1/+1 counter on each of up to two target creatures.)
 *
 * Support on an instant or sorcery has no "other" clause: one requirement taking zero to two creatures
 * (any controller), one +1/+1 counter on each — Shoulder to Shoulder without the draw.
 */
val LeadByExample = card("Lead by Example") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Support 2. (Put a +1/+1 counter on each of up to two target creatures.)"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Johannes Voss"
        flavorText = "Gideon and Nissa carved a path through the Eldrazi swarm, and their Zendikari allies charged into the fray behind them."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cdcb985-b0bb-4921-a7e4-3ee6e8f8992b.jpg?1783937901"
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
        ruling("2016-01-22", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
    }
}
