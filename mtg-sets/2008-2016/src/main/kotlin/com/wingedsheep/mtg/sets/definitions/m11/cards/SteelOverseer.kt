package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Steel Overseer
 * {2}
 * Artifact Creature — Construct
 * 1/1
 * {T}: Put a +1/+1 counter on each artifact creature you control.
 */
val SteelOverseer = card("Steel Overseer") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    oracleText = "{T}: Put a +1/+1 counter on each artifact creature you control."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.ArtifactCreature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Chris Rahn"
        flavorText = "\"The world is already run by all manner of machines. One day, they'll remind us of that fact.\"\n—Sargis Haz, artificer"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9da673d-7cc0-4435-b5a5-5098630f7712.jpg"
    }
}
