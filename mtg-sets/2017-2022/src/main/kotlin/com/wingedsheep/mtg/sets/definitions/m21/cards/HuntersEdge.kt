package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hunter's Edge
 * {3}{G}
 * Sorcery
 * Put a +1/+1 counter on target creature you control. Then that creature deals damage equal to
 * its power to target creature you don't control.
 */
val HuntersEdge = card("Hunter's Edge") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control. Then that creature deals damage equal to its power to target creature you don't control."
    spell {
        val yours = target(TargetFilter.Creature.youControl())
        val theirs = target(TargetFilter.Creature.opponentControls())
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = yours) then
            Effects.DealDamage(DynamicAmounts.powerOf(yours), theirs, damageSource = yours)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Johann Bodin"
        flavorText = "The hunt ends. Lunch begins."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c08c80f-f27c-4e3a-b048-143aea740096.jpg?1783930673"
        ruling("2020-06-23", "You can't cast Hunter's Edge unless you choose both a creature you control and a creature you don't control as targets.")
        ruling("2020-06-23", "If either creature is an illegal target as Hunter's Edge tries to resolve, the creature you control won't deal damage.")
        ruling("2020-06-23", "If the creature you don't control is an illegal target as Hunter's Edge tries to resolve but the creature you control is a legal target, you just put a +1/+1 counter on the creature you control.")
    }
}
