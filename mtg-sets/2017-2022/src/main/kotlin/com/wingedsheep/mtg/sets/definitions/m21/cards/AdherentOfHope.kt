package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Adherent of Hope
 * {1}{W}
 * Creature — Human Soldier
 * 2/1
 * At the beginning of combat on your turn, if you control a Basri planeswalker, put a +1/+1
 * counter on this creature.
 *
 * "If you control a Basri planeswalker" is an intervening-if (CR 603.4) on the planeswalker
 * subtype, the Vivien's Jaguar shape.
 */
val AdherentOfHope = card("Adherent of Hope") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "At the beginning of combat on your turn, if you control a Basri planeswalker, " +
        "put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Basri"))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "At the beginning of combat on your turn, if you control a Basri planeswalker, " +
            "put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "321"
        artist = "Dan Murayama Scott"
        flavorText = "\"Basri has shown me the way forward, and I will follow.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce2bc7ad-458d-445e-a0a9-7897b596fdd0.jpg?1783930623"
    }
}
