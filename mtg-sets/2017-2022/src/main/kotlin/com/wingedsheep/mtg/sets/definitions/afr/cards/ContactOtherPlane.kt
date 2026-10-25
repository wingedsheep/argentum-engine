package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Contact Other Plane
 * {3}{U}
 * Instant
 * Roll a d20.
 * 1—9 | Draw two cards.
 * 10—19 | Scry 2, then draw two cards.
 * 20 | Scry 3, then draw three cards.
 */
val ContactOtherPlane = card("Contact Other Plane") {
    manaCost = "{3}{U}"
    typeLine = "Instant"
    oracleText = "Roll a d20.\n" +
        "1—9 | Draw two cards.\n" +
        "10—19 | Scry 2, then draw two cards.\n" +
        "20 | Scry 3, then draw three cards."

    spell {
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) {
                run(Effects.DrawCards(2))
            }
            ifThen(between(10, 19)) {
                run(Effects.Scry(2) then Effects.DrawCards(2))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(Effects.Scry(3) then Effects.DrawCards(3))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6fefb38-c6f2-43c4-a6b9-ac82f8827bc2.jpg?1783926518"
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
    }
}
