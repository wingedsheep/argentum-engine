package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Osai Vultures
 * {1}{W}
 * Creature — Bird
 * 1/1
 *
 * Flying
 * At the beginning of each end step, if a creature died this turn, put a carrion counter on this
 * creature.
 * Remove two carrion counters from this creature: This creature gets +1/+1 until end of turn.
 *
 * "If a creature died this turn" is an intervening-if (checked when the step begins and again on
 * resolution), and "each end step" fires on every player's turn.
 */
val OsaiVultures = card("Osai Vultures") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "At the beginning of each end step, if a creature died this turn, put a carrion counter on this creature.\n" +
        "Remove two carrion counters from this creature: This creature gets +1/+1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.AddCounters(CounterType.CARRION, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.CARRION, 2)
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Dan Frazier"
        flavorText = "A sign of battle, the Vultures circle and wait for the victorious to depart."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f85614b3-62a3-4da9-a74a-7ea40fad1b52.jpg?1783948082"
    }
}
