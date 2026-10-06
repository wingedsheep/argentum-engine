package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feral Hydra
 * {X}{G}
 * Creature — Hydra Beast
 * 0/0
 * This creature enters with X +1/+1 counters on it.
 * {3}: Put a +1/+1 counter on this creature. Any player may activate this ability.
 */
val FeralHydra = card("Feral Hydra") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra Beast"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "{3}: Put a +1/+1 counter on this creature. Any player may activate this ability."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.AnyPlayerMay)
        description = "{3}: Put a +1/+1 counter on this creature. Any player may activate this ability."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Steve Prescott"
        flavorText = "It shreds its prey as each head fights for the choicest bits."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46f76986-e9fb-4c51-b946-880b501775b0.jpg?1783942554"
    }
}
