package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Sylvan Shepherd
 * {2}{G}
 * Creature — Human Druid
 * 2/3
 * Vigilance
 * Whenever this creature attacks, roll a d20.
 * 1—9 | You gain 1 life.
 * 10—19 | You gain 2 life.
 * 20 | You gain 5 life.
 */
val SylvanShepherd = card("Sylvan Shepherd") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever this creature attacks, roll a d20.\n" +
        "1—9 | You gain 1 life.\n" +
        "10—19 | You gain 2 life.\n" +
        "20 | You gain 5 life."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) { run(Effects.GainLife(1)) }
            ifThen(between(10, 19)) { run(Effects.GainLife(2)) }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(Effects.GainLife(5))
            }
        }
        description = "Whenever this creature attacks, roll a d20. 1—9: you gain 1 life. " +
            "10—19: you gain 2 life. 20: you gain 5 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "206"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7d608fb-35b0-498e-988a-f53986b59dd2.jpg?1783926455"
    }
}
