package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloated Processor
 * {2}{B}
 * Creature — Phyrexian
 * 3/2
 *
 * Sacrifice another Phyrexian: Put a +1/+1 counter on this creature.
 * When this creature dies, incubate X, where X is its power.
 */
val BloatedProcessor = card("Bloated Processor") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian"
    oracleText = "Sacrifice another Phyrexian: Put a +1/+1 counter on this creature.\n" +
        "When this creature dies, incubate X, where X is its power. (Create an Incubator token with X +1/+1 " +
        "counters on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)"
    power = 3
    toughness = 2

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Permanent.withSubtype("Phyrexian"))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Incubate(DynamicAmounts.sourcePower())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Brock Grossman"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66ddcca8-5720-4341-acce-c6694ddc97f8.jpg?1783917016"
    }
}
