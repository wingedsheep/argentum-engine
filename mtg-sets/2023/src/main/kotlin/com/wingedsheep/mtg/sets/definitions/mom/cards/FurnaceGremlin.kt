package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Furnace Gremlin
 * {1}{R}
 * Creature — Phyrexian Gremlin
 * 1/2
 *
 * {1}{R}: This creature gets +1/+0 until end of turn.
 * When this creature dies, incubate X, where X is its power.
 */
val FurnaceGremlin = card("Furnace Gremlin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phyrexian Gremlin"
    oracleText = "{1}{R}: This creature gets +1/+0 until end of turn.\n" +
        "When this creature dies, incubate X, where X is its power. (Create an Incubator token with X +1/+1 " +
        "counters on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)"
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Incubate(DynamicAmounts.sourcePower())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/445b3315-6587-4d57-ab7e-faab1eec0241.jpg?1783916992"
    }
}
