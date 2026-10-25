package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Herald of Hadar
 * {4}{B}
 * Creature — Human Warlock
 * 4/4
 * Circle of Death — {5}{B}: Roll a d20.
 * 1—9 | Each opponent loses 2 life.
 * 10—19 | Each opponent loses 2 life and you gain 2 life.
 * 20 | Each opponent loses 2 life and you gain 2 life. Create two Treasure tokens.
 */
val HeraldOfHadar = card("Herald of Hadar") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Human Warlock"
    power = 4
    toughness = 4
    oracleText = "Circle of Death — {5}{B}: Roll a d20.\n" +
        "1—9 | Each opponent loses 2 life.\n" +
        "10—19 | Each opponent loses 2 life and you gain 2 life.\n" +
        "20 | Each opponent loses 2 life and you gain 2 life. Create two Treasure tokens. " +
        "(They're artifacts with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    activatedAbility {
        cost = Costs.Mana("{5}{B}")
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) {
                run(Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)))
            }
            ifThen(between(10, 19)) {
                run(Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2))
            }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(
                    Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
                        Effects.GainLife(2) then
                        Effects.CreateTreasure(2)
                )
            }
        }
        description = "Circle of Death — {5}{B}: Roll a d20. 1—9: each opponent loses 2 life. 10—19: each " +
            "opponent loses 2 life and you gain 2 life. 20: each opponent loses 2 life, you gain 2 life, " +
            "and you create two Treasure tokens."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/baa74204-2593-4468-856d-a5791336126a.jpg?1783926492"
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
    }
}
