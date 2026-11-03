package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bellowing Aegisaur
 * {5}{W}
 * Creature — Dinosaur
 * 3/5
 *
 * Enrage — Whenever this creature is dealt damage, put a +1/+1 counter on each other creature
 * you control.
 */
val BellowingAegisaur = card("Bellowing Aegisaur") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    oracleText = "Enrage — Whenever this creature is dealt damage, put a +1/+1 counter on each other creature you control."
    power = 3
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
        description = "Enrage — Whenever this creature is dealt damage, put a +1/+1 counter on each other creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Craig J Spearing"
        flavorText = "Its roaring challenge is punctuated by the slam of its tail on the earth, rousing the herds of the plains."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3e3b6c5-fd30-4d45-a122-ce60d5707357.jpg?1783935806"
        ruling(
            "2017-09-29",
            "If another creature you control is dealt lethal damage at the same time that Bellowing Aegisaur " +
                "is dealt damage, the other creature won't be saved by the +1/+1 counter that would have been put on it."
        )
    }
}
