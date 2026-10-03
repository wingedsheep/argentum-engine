package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tempered Veteran
 * {1}{W}
 * Creature — Human Knight
 * 1/2
 * {W}, {T}: Put a +1/+1 counter on target creature with a +1/+1 counter on it.
 * {4}{W}{W}, {T}: Put a +1/+1 counter on target creature.
 *
 * Two tap abilities differing only in cost and target filter; the cheap one's target is
 * `Creature.withCounter(PLUS_ONE_PLUS_ONE)`, which is re-checked on resolution like any target.
 */
val TemperedVeteran = card("Tempered Veteran") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 1
    toughness = 2
    oracleText = "{W}, {T}: Put a +1/+1 counter on target creature with a +1/+1 counter on it.\n" +
        "{4}{W}{W}, {T}: Put a +1/+1 counter on target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val creature = target(TargetFilter(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE)))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "{W}, {T}: Put a +1/+1 counter on target creature with a +1/+1 counter on it."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{W}{W}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "{4}{W}{W}, {T}: Put a +1/+1 counter on target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Izzy"
        flavorText = "\"I've been carrying a sword since before you were born. I wouldn't give up my " +
            "oath for all the peace and comfort in the world.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b43d7bc-173c-41eb-bba9-a9d94dcfc5fa.jpg?1783930732"
    }
}
