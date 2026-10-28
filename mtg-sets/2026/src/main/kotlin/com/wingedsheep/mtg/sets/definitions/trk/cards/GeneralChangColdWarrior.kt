package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * General Chang, Cold Warrior
 * {B}
 * Legendary Creature — Klingon Warrior Officer
 * 1/2
 *
 * {1}, Sacrifice a creature: Put a +1/+1 counter on each Klingon you control.
 */
val GeneralChangColdWarrior = card("General Chang, Cold Warrior") {
    manaCost = "{B}"
    typeLine = "Legendary Creature — Klingon Warrior Officer"
    colorIdentity = "B"
    oracleText = "{1}, Sacrifice a creature: Put a +1/+1 counter on each Klingon you control."
    power = 1
    toughness = 2
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Klingon").youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Cliff Nielsen"
        flavorText = "\"Be honest, Captain. Warrior to warrior. You do prefer it this way, don't you? As it was meant to be. No peace in our time.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c46a0910-215c-4757-8bfe-7eca479157a5.jpg?1786396042"
    }
}

