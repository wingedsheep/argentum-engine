package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val LuxaRiverShrine = card("Luxa River Shrine") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: You gain 1 life. Put a brick counter on this artifact.\n" +
        "{T}: You gain 2 life. Activate only if there are three or more brick counters on this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.GainLife(1) then
            Effects.AddCounters(CounterType.BRICK, 1, EffectTarget.Self)
        description = "You gain 1 life. Put a brick counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceCounterCountAtLeast(CounterType.BRICK, 3))
        )
        effect = Effects.GainLife(2)
        description = "You gain 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "232"
        artist = "Sung Choi"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f43dc6f-910c-42a1-b329-038e20dfb264.jpg?1783936451"
    }
}
