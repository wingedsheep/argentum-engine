package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Chaos Channeler
 * {2}{R}{R}
 * Creature — Human Shaman
 * 4/3
 * Wild Magic Surge — Whenever this creature attacks, roll a d20.
 * 1—9 | Exile the top card of your library. You may play it this turn.
 * 10—19 | Exile the top two cards of your library. You may play them this turn.
 * 20 | Exile the top three cards of your library. You may play them this turn.
 */
val ChaosChanneler = card("Chaos Channeler") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Human Shaman"
    power = 4
    toughness = 3
    oracleText = "Wild Magic Surge — Whenever this creature attacks, roll a d20.\n" +
        "1—9 | Exile the top card of your library. You may play it this turn.\n" +
        "10—19 | Exile the top two cards of your library. You may play them this turn.\n" +
        "20 | Exile the top three cards of your library. You may play them this turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) { run(Patterns.Exile.impulse(count = 1)) }
            ifThen(between(10, 19)) { run(Patterns.Exile.impulse(count = 2)) }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(Patterns.Exile.impulse(count = 3))
            }
        }
        description = "Wild Magic Surge — Whenever this creature attacks, roll a d20. 1—9: exile the top card " +
            "of your library; you may play it this turn. 10—19: exile the top two cards of your library; you " +
            "may play them this turn. 20: exile the top three cards of your library; you may play them this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b9bb587-f732-42c9-a2a7-152629889997.jpg?1783926482"
        ruling("2021-07-23", "If you do not play the exiled cards on the turn that they are exiled, they remain exiled indefinitely.")
        ruling("2021-07-23", "Normal costs and timing permissions apply to cards played this way. Unless an effect says you can play additional lands during your turn, you cannot play a land exiled this way if you have already played one this turn.")
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
    }
}
