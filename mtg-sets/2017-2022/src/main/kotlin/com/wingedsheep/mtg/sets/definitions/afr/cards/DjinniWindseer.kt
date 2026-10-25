package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Djinni Windseer
 * {3}{U}
 * Creature — Djinn
 * 3/3
 * Flying
 * When this creature enters, roll a d20.
 * 1—9 | Scry 1.
 * 10—19 | Scry 2.
 * 20 | Scry 3.
 */
val DjinniWindseer = card("Djinni Windseer") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Djinn"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, roll a d20.\n" +
        "1—9 | Scry 1.\n" +
        "10—19 | Scry 2.\n" +
        "20 | Scry 3."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 9)) {
                run(Effects.Scry(1))
            }
            ifThen(
                Conditions.All(
                    Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 10),
                    Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, 19),
                )
            ) {
                run(Effects.Scry(2))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(Effects.Scry(3))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67a42698-bf3b-4352-8178-9c98abfc5b09.jpg?1783926516"
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
    }
}
