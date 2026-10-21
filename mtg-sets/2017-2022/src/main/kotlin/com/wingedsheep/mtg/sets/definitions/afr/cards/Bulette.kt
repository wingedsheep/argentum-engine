package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bulette
 * {3}{G}
 * Creature — Beast
 * 3/3
 * At the beginning of your end step, if a creature died this turn, put a +1/+1 counter on this creature.
 */
val Bulette = card("Bulette") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    oracleText = "At the beginning of your end step, if a creature died this turn, put a +1/+1 counter on this creature."
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Jason Kang"
        flavorText = "Capable of moving swiftly through even tightly-packed earth and leaping surprising distances above ground, bulettes are difficult to escape once they've caught scent of prey."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/206a9e7b-45c1-4213-8fc4-27d90e2ab0e9.jpg?1783926468"
    }
}
