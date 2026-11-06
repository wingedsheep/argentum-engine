package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ridgescale Tusker
 * {3}{G}{G}
 * Creature — Pangolin Beast
 * 5/5
 *
 * When this creature enters, put a +1/+1 counter on each other creature you control.
 *
 * Aether Revolt is the earliest printing; the only later one is Kaladesh Remastered (KLR),
 * which isn't scaffolded.
 */
val RidgescaleTusker = card("Ridgescale Tusker") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Pangolin Beast"
    oracleText = "When this creature enters, put a +1/+1 counter on each other creature you control."
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
        description = "When this creature enters, put a +1/+1 counter on each other creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Winona Nelson"
        flavorText = "\"Each creature holds a solution to a problem, a lesson to be learned.\"\n—Oviya Pashiri, sage lifecrafter"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84b689cc-35ef-4a23-bb1e-4d81b9fb8455.jpg?1783936741"
    }
}
