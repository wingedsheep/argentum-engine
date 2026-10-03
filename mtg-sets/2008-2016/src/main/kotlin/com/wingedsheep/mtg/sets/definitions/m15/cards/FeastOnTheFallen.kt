package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Feast on the Fallen
 * {2}{B}
 * Enchantment
 * At the beginning of each upkeep, if an opponent lost life last turn, put a +1/+1 counter on
 * target creature you control.
 *
 * "Last turn" is the previous turn in the game, whoever's it was, and only *losing* life counts —
 * not a net decrease (rulings). The "if" is an intervening-if (CR 603.4).
 *
 * Canonical printing: Magic 2015. Reprinted in J22 as a `Printing` row.
 */
val FeastOnTheFallen = card("Feast on the Fallen") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each upkeep, if an opponent lost life last turn, put a +1/+1 " +
        "counter on target creature you control."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.OpponentLostLifeLastTurn
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Dave Kendall"
        flavorText = "\"As our numbers dwindle, the ranks of the dead grow ever stronger.\"\n—Thalia, Knight-Cathar"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d96a9227-14ce-4d35-b5e6-0d0c657207c0.jpg?1783939184"
        ruling(
            "2014-07-18",
            "Feast on the Fallen looks at the entire previous turn to determine whether its ability " +
                "triggers or not. It doesn't matter whether Feast on the Fallen was on the battlefield " +
                "when the opponent lost life."
        )
        ruling(
            "2014-07-18",
            "Feast on the Fallen checks only if an opponent lost life during the turn, not whether " +
                "that player's life total decreased over the course of the turn. For example, if an " +
                "opponent lost 2 life and then gained 8 life last turn, Feast on the Fallen's ability " +
                "will trigger."
        )
    }
}
