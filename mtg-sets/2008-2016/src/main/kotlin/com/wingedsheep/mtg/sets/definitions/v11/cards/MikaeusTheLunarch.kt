package com.wingedsheep.mtg.sets.definitions.v11.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mikaeus, the Lunarch
 * {X}{W}
 * Legendary Creature — Human Cleric
 * 0/0
 *
 * Mikaeus enters with X +1/+1 counters on it.
 * {T}: Put a +1/+1 counter on Mikaeus.
 * {T}, Remove a +1/+1 counter from Mikaeus: Put a +1/+1 counter on each other creature you control.
 *
 * Earliest real printing is From the Vault: Legends; Innistrad and later sets carry Printing rows.
 */
val MikaeusTheLunarch = card("Mikaeus, the Lunarch") {
    manaCost = "{X}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Cleric"
    power = 0
    toughness = 0
    oracleText = "Mikaeus enters with X +1/+1 counters on it.\n" +
        "{T}: Put a +1/+1 counter on Mikaeus.\n" +
        "{T}, Remove a +1/+1 counter from Mikaeus: Put a +1/+1 counter on each other creature you control."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{T}: Put a +1/+1 counter on Mikaeus."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE))
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
        description = "{T}, Remove a +1/+1 counter from Mikaeus: Put a +1/+1 counter on each other " +
            "creature you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77976ca8-7c7a-469c-a99a-808de1b8bf71.jpg?1783941033"
    }
}
