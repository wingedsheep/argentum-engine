package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Biogenic Upgrade
 * {4}{G}{G}
 * Sorcery
 * Distribute three +1/+1 counters among one, two, or three target creatures, then double the number
 * of +1/+1 counters on each of those creatures.
 *
 * The distribution is announced as the spell is cast (CR 601.2d), each target getting at least one;
 * the doubling then reads every +1/+1 counter on each target, including ones it already had.
 */
val BiogenicUpgrade = card("Biogenic Upgrade") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Distribute three +1/+1 counters among one, two, or three target creatures, then double the number of +1/+1 counters on each of those creatures."
    spell {
        val creatures = targets(TargetFilter.Creature, count = 3, minCount = 1)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 3) then
            Effects.ForEachTargetOf(creatures, Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.IterationEntity))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Tomasz Jedruszek"
        flavorText = "\"To survive exceptional times we must adapt by exceptional means.\"\n—Vannifar"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dd73fb2-453f-40b9-8beb-dfa99e6a706e.jpg"
    }
}
