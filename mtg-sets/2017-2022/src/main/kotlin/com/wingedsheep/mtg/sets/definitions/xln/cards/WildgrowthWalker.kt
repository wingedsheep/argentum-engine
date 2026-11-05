package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wildgrowth Walker
 * {1}{G}
 * Creature — Elemental
 * 1/3
 *
 * Whenever a creature you control explores, put a +1/+1 counter on this creature and you gain
 * 3 life.
 */
val WildgrowthWalker = card("Wildgrowth Walker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "Whenever a creature you control explores, put a +1/+1 counter on this creature " +
        "and you gain 3 life."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).explores()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Ryan Alexander Lee"
        flavorText = "\"Hear me, stone, root, branch: be the fist of this land. Turn back those who " +
            "trample upon your domain.\"\n—Song of the Shaper"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52e4c0f8-d5f0-4224-9974-190606911480.jpg?1783935714"
    }
}
