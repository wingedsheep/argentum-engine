package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Verdant Automaton
 * {2}
 * Artifact Creature — Construct
 * 1/2
 * {3}{G}: Put a +1/+1 counter on this creature.
 */
val VerdantAutomaton = card("Verdant Automaton") {
    manaCost = "{2}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 2
    oracleText = "{3}{G}: Put a +1/+1 counter on this creature."

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Ryan Alexander Lee"
        flavorText = "\"A perfect fusion of nature and artifice.\"\n—Irsi, Kujar gardener"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52c66c5a-0b1b-4936-9e07-2d169f16c1a6.jpg"
    }
}
