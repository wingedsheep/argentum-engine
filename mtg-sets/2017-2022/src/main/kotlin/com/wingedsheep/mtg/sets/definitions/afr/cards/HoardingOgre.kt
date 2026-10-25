package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Hoarding Ogre
 * {3}{R}
 * Creature — Ogre
 * 3/3
 * Whenever this creature attacks, roll a d20.
 * 1—9 | Create a Treasure token.
 * 10—19 | Create two Treasure tokens.
 * 20 | Create three Treasure tokens.
 */
val HoardingOgre = card("Hoarding Ogre") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Ogre"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks, roll a d20.\n" +
        "1—9 | Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "10—19 | Create two Treasure tokens.\n" +
        "20 | Create three Treasure tokens."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) { run(Effects.CreateTreasure(1)) }
            ifThen(between(10, 19)) { run(Effects.CreateTreasure(2)) }
            ifThen(Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, 20)) {
                run(Effects.CreateTreasure(3))
            }
        }
        description = "Whenever this creature attacks, roll a d20. 1—9: create a Treasure token. " +
            "10—19: create two Treasure tokens. 20: create three Treasure tokens."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df39eaac-b13f-4cb3-960d-8f7a0d590663.jpg?1783926478"
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
        ruling("2021-07-23", "Some effects may modify the result of a die roll. This may be part of the instruction to roll a die or it may come from other cards. Anything that references the \"result\" of a die roll is looking for the result after these modifications. Anything that is looking for the \"natural result\" is looking for the number shown on the face of the die before these modifications.")
    }
}
