package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Azor's Elocutors
 * {3}{W/U}{W/U}
 * Creature — Human Advisor
 * 3/5
 *
 * The five-counter check is part of the upkeep trigger's resolution ("Then if …"), not an
 * intervening if: the counter goes on first, then the count is read. Each damage instance dealt to
 * the controller is one "source deals damage to you" trigger (2012-10-01 ruling: combat damage from
 * several creatures removes one counter per creature).
 */
val AzorsElocutors = card("Azor's Elocutors") {
    manaCost = "{3}{W/U}{W/U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Advisor"
    power = 3
    toughness = 5
    oracleText = "At the beginning of your upkeep, put a filibuster counter on this creature. Then if this creature " +
        "has five or more filibuster counters on it, you win the game.\n" +
        "Whenever a source deals damage to you, remove a filibuster counter from this creature."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.FILIBUSTER, 1, EffectTarget.Self) then
            Effects.If(
                Conditions.CompareAmounts(
                    DynamicAmounts.countersOnSelf(CounterType.FILIBUSTER),
                    ComparisonOperator.GTE,
                    5
                ),
                Effects.WinGame(
                    target = EffectTarget.Controller,
                    message = "Azor's Elocutors reached five filibuster counters."
                )
            )
    }

    triggeredAbility {
        trigger = Triggers.you.isDealtDamage()
        effect = Effects.RemoveCounters(CounterType.FILIBUSTER, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61e82934-546b-4734-a715-b22ace4c5a9b.jpg?1783940329"
        ruling("2012-10-01", "You’ll only win the game if Azor’s Elocutors has five or more filibuster counters on it when the first ability resolves. For example, if Azor’s Elocutors has four filibuster counters on it and you somehow add another during your main phase, you won’t win the game immediately.")
        ruling("2012-10-01", "The second ability of Azor’s Elocutors removes one filibuster counter per source, no matter how much damage that source dealt. For example, if two attacking creatures deal damage to you at the same time, two filibuster counters will be removed.")
    }
}
