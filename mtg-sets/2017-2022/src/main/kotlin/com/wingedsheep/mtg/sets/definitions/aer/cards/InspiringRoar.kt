package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Inspiring Roar
 * {3}{W}
 * Sorcery
 * Put a +1/+1 counter on each creature you control.
 */
val InspiringRoar = card("Inspiring Roar") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on each creature you control."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Chris Rahn"
        flavorText = "\"This is about more than aether or inventions. Tezzeret's schemes must be stopped!\"\n—Ajani Goldmane"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ef9708e-bfe4-4d24-8304-1618daa1c3ee.jpg?1783936715"
        inBooster = false
    }
}
