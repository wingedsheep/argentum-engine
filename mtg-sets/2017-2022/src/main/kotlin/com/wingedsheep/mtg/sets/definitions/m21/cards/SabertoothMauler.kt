package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sabertooth Mauler
 * {3}{G}
 * Creature — Cat
 * 3/3
 * At the beginning of your end step, if a creature died this turn, put a +1/+1 counter on this
 * creature and untap it.
 *
 * "If a creature died this turn" is an intervening-if: no trigger at all when nothing died, and
 * it triggers once no matter how many creatures died (any creature, even before this one entered).
 */
val SabertoothMauler = card("Sabertooth Mauler") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your end step, if a creature died this turn, put a +1/+1 counter " +
        "on this creature and untap it."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.Untap(EffectTarget.Self)
        description = "At the beginning of your end step, if a creature died this turn, put a +1/+1 " +
            "counter on this creature and untap it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Randy Vargas"
        flavorText = "\"I hope the idiot who introduced these creatures to the jungle was their first meal.\"\n—Opar, veteran guide"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe39e38e-76e5-4883-b530-d3e30e88ccad.jpg?1783930669"
        ruling("2020-06-23", "If a creature didn't die before your end step begins, Sabertooth Mauler's ability doesn't trigger at all. The creature may have died before Sabertooth Mauler entered the battlefield, however.")
        ruling("2020-06-23", "The triggered ability triggers just once, no matter how many creatures died this turn.")
    }
}
