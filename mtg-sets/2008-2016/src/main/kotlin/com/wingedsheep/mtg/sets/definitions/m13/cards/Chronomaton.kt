package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chronomaton
 * {1}
 * Artifact Creature — Golem
 * 1/1
 * {1}, {T}: Put a +1/+1 counter on this creature.
 */
val Chronomaton = card("Chronomaton") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    oracleText = "{1}, {T}: Put a +1/+1 counter on this creature."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Vincent Proce"
        flavorText = "On the third night, the villagers destroyed their clocks. The sounds of whirring gears and chiming metal held only dread for them."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aac35e28-dd0e-4dc8-b8e6-4a1e33706214.jpg"
    }
}
