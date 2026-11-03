package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Exploratory Mission
 * {2}{R}{R}
 * Enchantment
 *
 * Landfall — Whenever a land you control enters, put a quest counter on this enchantment. Then exile
 * cards from the top of your library until you exile a nonland card. You may play that card this turn.
 * If this enchantment has five or more quest counters on it, add {R}{R}.
 *
 * The counter check runs after the counter lands, so the fifth landfall already adds {R}{R}.
 */
val ExploratoryMission = card("Exploratory Mission") {
    manaCost = "{2}{R}{R}"
    typeLine = "Enchantment"
    colorIdentity = "R"
    oracleText = "Landfall — Whenever a land you control enters, put a quest counter on this enchantment. " +
        "Then exile cards from the top of your library until you exile a nonland card. You may play that " +
        "card this turn. If this enchantment has five or more quest counters on it, add {R}{R}."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self) then
            Effects.Pipeline {
                val (impulseCard, exiledCards) = gatherUntilMatch(GameObjectFilter.Nonland)
                exile(exiledCards)
                run(Effects.GrantMayPlayFromExile(from = impulseCard))
            } then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.countersOnSelf(CounterType.QUEST),
                    ComparisonOperator.GTE,
                    5
                ),
                then = Effects.AddMana(Color.RED, 2)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Jason Pastrana"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61bb2ec5-5f04-4c34-ba72-7fa226922c5c.jpg?1793013181"
    }
}
